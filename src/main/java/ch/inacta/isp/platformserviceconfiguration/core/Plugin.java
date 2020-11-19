package ch.inacta.isp.platformserviceconfiguration.core;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.FileSet;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.glassfish.jersey.jackson.JacksonFeature;

import ch.inacta.isp.platformserviceconfiguration.core.model.AccessTokenResponse;
import ch.inacta.isp.platformserviceconfiguration.core.strategy.AuthorizationStrategy;
import ch.inacta.isp.platformserviceconfiguration.core.strategy.KeycloakStrategy;
import ch.inacta.isp.platformserviceconfiguration.core.strategy.RabbitMQStrategy;

@Mojo(name = "rest-request")
public class Plugin extends AbstractMojo {

    private static final String REALM_PLACEHOLDER = "%4T";
    private static final String DEFAULT_METHOD = "POST";
    private static final MediaType DEFAULT_REQUEST_TYPE = APPLICATION_JSON_TYPE;

    @Parameter(property = "app")
    private String app;

    @Parameter(property = "fileSet")
    private FileSet fileSet;

    @Parameter(property = "fileSets")
    private final List<FileSet> fileSets = new ArrayList<>();

    @Parameter(property = "resource")
    private String resource;

    @Parameter(property = "realms")
    private String realms;

    @Parameter(property = "endpoint")
    private URI endpoint;

    @Parameter(property = "authorization")
    private Map<String, String> authorization;

    @Parameter(property = "method")
    private String method;

    @Parameter(property = "requestType")
    private MediaType requestType;

    @Override
    public void execute() throws MojoExecutionException {

        final List<ErrorInfo> errorInfos = new ArrayList<>();
        final List<File> files = getFilesToProcess();

        final AuthorizationStrategy authorizationStrategy = getStrategy();

        for (final String resourcePath : getResourcePaths()) {
            final Invocation.Builder builder = createBuilder(authorizationStrategy, resourcePath);

            if (!files.isEmpty()) {
                errorInfos.addAll(executeRequestWithFiles(builder, files));
            } else {
                errorInfos.addAll(executeRequestWithoutFiles(builder));
            }
        }

        if (!errorInfos.isEmpty()) {
            throw new MojoExecutionException(String.format("Unable to process files: %n%s", wrap(" ", "%n", errorInfos)));
        }
    }

    private List<ErrorInfo> executeRequestWithoutFiles(final Invocation.Builder builder) throws MojoExecutionException {

        final List<ErrorInfo> errorInfos = new ArrayList<>();

        if (getMethod().equalsIgnoreCase("GET")) {
            throw new MojoExecutionException("Get requests are not supported!");
        } else {
            final ErrorInfo result = processResponse(builder.method(getMethod(), Entity.form(new Form())));
            if (result != null) {
                errorInfos.add(result);
            }
        }

        return errorInfos;
    }

    private List<ErrorInfo> executeRequestWithFiles(final Invocation.Builder builder, final List<File> files) {

        final List<ErrorInfo> errorInfos = new ArrayList<>();

        for (final File file : files) {
            getLog().info(String.format("Submitting file [%s]", file.toString()));
            final ErrorInfo result = processResponse(builder.method(getMethod(), Entity.entity(file, getRequestType())));
            if (result != null) {
                errorInfos.add(new FileErrorInfo(file.getPath(), result));
            }
        }

        return errorInfos;
    }

    private ErrorInfo processResponse(final Response response) {

        if (response.getStatusInfo().getFamily() == SUCCESSFUL) {
            getLog().info(String.format("Status: [%d]", response.getStatus()));
        } else {
            getLog().warn(String.format("Error code: [%d]", response.getStatus()));
            return new ErrorInfo(response.getStatus(), response.getEntity().toString());
        }

        return null;
    }

    private Invocation.Builder createBuilder(final AuthorizationStrategy authorizationStrategy, final String resource) throws MojoExecutionException {

        final Client client = ClientBuilder.newClient();
        client.register(JacksonFeature.class);
        WebTarget webTarget = client.target(getEndpoint());

        getLog().info(String.format("Endpoint: [%s %s]", getMethod(), webTarget.getUri()));

        AccessTokenResponse accessTokenResponse = null;
        if (!getAuthParams().isEmpty()) {
            if (authorizationStrategy.getClass() == KeycloakStrategy.class) {
                ((KeycloakStrategy) authorizationStrategy).setWebTarget(webTarget);
            }

            accessTokenResponse = authorizationStrategy.authorize(getAuthParams());
        }

        webTarget = webTarget.path(resource);
        final Invocation.Builder builder = webTarget.request(authorizationStrategy.getRequestType(), authorizationStrategy.getResponseType());

        setHeaders(builder, accessTokenResponse);

        return builder;
    }

    private void setHeaders(final Invocation.Builder builder, final AccessTokenResponse accessTokenResponse) {

        if (accessTokenResponse != null) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(accessTokenResponse.getTokenType()).append(" ").append(accessTokenResponse.getAccessToken());
            builder.header("Authorization", stringBuilder.toString());
        }
    }

    private List<String> getResourcePaths() throws MojoExecutionException {

        final List<String> resourcePaths = new ArrayList<>();

        if (!getRealms().isEmpty()) {
            final List<String> configuredRealms = Arrays.asList(getRealms().replace(" ", "").split(","));

            if (!getResource().contains(REALM_PLACEHOLDER)) {
                throw new MojoExecutionException(String.format("No placeholder symbol '%s' for realms found!", REALM_PLACEHOLDER));
            }
            for (final String realm : configuredRealms) {
                resourcePaths.add(getResource().replace(REALM_PLACEHOLDER, realm));
            }
        } else {
            if (getResource().contains(REALM_PLACEHOLDER)) {
                throw new MojoExecutionException("No realms are defined!");
            }
            resourcePaths.add(getResource());
        }

        return resourcePaths;
    }

    private AuthorizationStrategy getStrategy() throws MojoExecutionException {

        final AuthorizationStrategy strategy;

        if (getApp().equalsIgnoreCase("keycloak")) {
            strategy = new KeycloakStrategy(getLog());
        } else {
            strategy = new RabbitMQStrategy();
        }

        getLog().info(String.format("Selected authorization strategy: [%s]", strategy.getStrategyName()));
        return strategy;
    }

    private List<File> getFilesToProcess() throws MojoExecutionException {

        final List<File> files = new ArrayList<>();

        if (getFileSet() != null && getFileSet().getDirectory() != null) {
            getFileSets().add(getFileSet());
        }

        for (final FileSet fileSet : getFileSets()) {
            if (fileSet != null) {
                final FileSetTransformer fileSetTransformer = new FileSetTransformer(getLog(), fileSet);
                files.addAll(fileSetTransformer.toFileList());
                getLog().info(String.format("Files found: %s", StringUtils.join(fileSetTransformer.toFileList(), "\n")));
            }
        }

        return files;
    }

    private <T> String wrap(final String prefix, final String suffix, final List<T> tokens) {

        final StringBuilder stringBuilder = new StringBuilder();
        for (final T token : tokens) {
            stringBuilder.append(prefix).append(token.toString()).append(suffix);
        }
        return stringBuilder.toString();
    }

    /**
     * Gets the in the plugin configuration defined app.
     * 
     * @return the app
     * @throws MojoExecutionException
     */
    public String getApp() throws MojoExecutionException {

        if (this.app == null) {
            throw new MojoExecutionException("Tag 'app' has to be defined in configuration!");
        }

        return this.app;
    }

    /**
     * Gets the defined file set.
     * 
     * @return a file set
     */
    public FileSet getFileSet() {

        return this.fileSet;
    }

    /**
     * Gets the defined file sets.
     * 
     * @return a list of file sets
     */
    public List<FileSet> getFileSets() {

        return this.fileSets;
    }

    /**
     * Gets the defined resource.
     * 
     * @return the resource path
     * @throws MojoExecutionException
     */
    public String getResource() throws MojoExecutionException {

        if (this.resource == null) {
            throw new MojoExecutionException("Tag 'resource' has to be defined in configuration!");
        }

        return this.resource;
    }

    /**
     * Gets the defined realms.
     * 
     * @return the realms as string
     */
    public String getRealms() {

        if (this.realms == null) {
            return "";
        }

        return this.realms;
    }

    /**
     * Gets the defined endpoint.
     * 
     * @return the endpoint as URI
     * @throws MojoExecutionException
     */
    public URI getEndpoint() throws MojoExecutionException {

        if (this.endpoint == null) {
            throw new MojoExecutionException("Tag 'endpoint' has to be defined in configuration!");
        }

        return this.endpoint;
    }

    /**
     * Gets the defined authorization params.
     * 
     * @return a map containing the authorization params
     */
    public Map<String, String> getAuthParams() {

        if (this.authorization == null) {
            return new HashMap<>();
        }

        return this.authorization;
    }

    /**
     * Gets the defined request method.
     * 
     * @return the request method as string
     */
    public String getMethod() {

        if (this.method == null) {
            return DEFAULT_METHOD;
        }

        return this.method;
    }

    /**
     * Gets the defined request type.
     * 
     * @return the request type
     */
    public MediaType getRequestType() {

        if (WILDCARD_TYPE.equals(this.requestType)) {
            return DEFAULT_REQUEST_TYPE;
        }

        return this.requestType;
    }
}