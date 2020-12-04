package ch.inacta.maven.platformserviceconfiguration.core;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.client.Entity.form;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
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

import ch.inacta.maven.platformserviceconfiguration.core.model.AccessTokenResponse;
import ch.inacta.maven.platformserviceconfiguration.core.strategy.AuthorizationStrategy;
import ch.inacta.maven.platformserviceconfiguration.core.strategy.KeycloakStrategy;
import ch.inacta.maven.platformserviceconfiguration.core.strategy.RabbitMQStrategy;

/**
 * Platformservice configuration plugin implementation.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
@Mojo(name = "configure")
public class Plugin extends AbstractMojo {

    private static final String DEFAULT_METHOD = "POST";
    private static final MediaType DEFAULT_REQUEST_TYPE = APPLICATION_JSON_TYPE;
    private static final String REALM_PLACEHOLDER = "%4T";

    @Parameter(property = "app")
    private String app;

    @Parameter(property = "authorization")
    private Map<String, String> authorization;

    @Parameter(property = "endpoint")
    private URI endpoint;

    @Parameter(property = "fileSet")
    private FileSet fileSet;

    @Parameter(property = "fileSets")
    private final List<FileSet> fileSets = new ArrayList<>();

    @Parameter(property = "method")
    private String method;

    @Parameter(property = "realms")
    private String realms;

    @Parameter(property = "requestType")
    private MediaType requestType;

    @Parameter(property = "resource")
    private String resource;

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
            throw new MojoExecutionException(format("Unable to process files: %n%s", wrap(" ", "%n", errorInfos)));
        }
    }

    private Invocation.Builder createBuilder(final AuthorizationStrategy authorizationStrategy, final String resource) throws MojoExecutionException {

        final Client client = newClient();
        client.register(JacksonFeature.class);
        WebTarget webTarget = client.target(getEndpoint());

        getLog().info(format("Endpoint: [%s %s]", getMethod(), webTarget.getUri()));

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

    private List<ErrorInfo> executeRequestWithFiles(final Invocation.Builder builder, final List<File> files) {

        final List<ErrorInfo> errorInfos = new ArrayList<>();

        for (final File file : files) {
            getLog().info(format("Submitting file [%s]", file.toString()));
            final ErrorInfo result = processResponse(builder.method(getMethod(), entity(file, getRequestType())));
            if (result != null) {
                errorInfos.add(new FileErrorInfo(file.getPath(), result));
            }
        }

        return errorInfos;
    }

    private List<ErrorInfo> executeRequestWithoutFiles(final Invocation.Builder builder) throws MojoExecutionException {

        final List<ErrorInfo> errorInfos = new ArrayList<>();

        if (getMethod().equalsIgnoreCase("GET")) {
            throw new MojoExecutionException("Get requests are not supported!");
        } else {
            final ErrorInfo result = processResponse(builder.method(getMethod(), form(new Form())));
            if (result != null) {
                errorInfos.add(result);
            }
        }

        return errorInfos;
    }

    private List<File> getFilesToProcess() throws MojoExecutionException {

        final List<File> files = new ArrayList<>();

        if (getFileSet() != null && getFileSet().getDirectory() != null) {
            getFileSets().add(getFileSet());
        }

        for (final FileSet set : getFileSets()) {
            if (set != null) {
                final FileSetTransformer fileSetTransformer = new FileSetTransformer(getLog(), set);
                files.addAll(fileSetTransformer.toFileList());
                getLog().info(format("Files found: %s", StringUtils.join(fileSetTransformer.toFileList(), "\n")));
            }
        }

        return files;
    }

    private List<String> getResourcePaths() throws MojoExecutionException {

        final List<String> resourcePaths = new ArrayList<>();

        if (!getRealms().isEmpty()) {
            final List<String> configuredRealms = asList(getRealms().replace(" ", "").split(","));

            if (!getResource().contains(REALM_PLACEHOLDER)) {
                throw new MojoExecutionException(format("No placeholder symbol '%s' for realms found!", REALM_PLACEHOLDER));
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

        switch (getApp().toUpperCase()) {
        case "KEYCLOAK":
            strategy = new KeycloakStrategy(getLog());
            break;
        case "RABBITMQ":
            strategy = new RabbitMQStrategy();
            break;
        default:
            getLog().error(format("Unknown authorization strategy. Please check your configuration."));
            throw new MojoExecutionException(format("Unknown authorization strategy. Strategy [%s] is not supported", getApp()));
        }

        getLog().info(format("Selected authorization strategy: [%s]", strategy.getStrategyName()));
        return strategy;
    }

    private ErrorInfo processResponse(final Response response) {

        if (response.getStatusInfo().getFamily() == SUCCESSFUL) {
            getLog().info(format("Status: [%d]", response.getStatus()));
        } else {
            getLog().warn(format("Error code: [%d]", response.getStatus()));
            return new ErrorInfo(response.getStatus(), response.getEntity().toString());
        }

        return null;
    }

    private void setHeaders(final Invocation.Builder builder, final AccessTokenResponse accessTokenResponse) {

        if (accessTokenResponse != null) {
            final StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(accessTokenResponse.getTokenType()).append(" ").append(accessTokenResponse.getAccessToken());
            builder.header("Authorization", stringBuilder.toString());
        }
    }

    private <T> String wrap(final String prefix, final String suffix, final List<T> tokens) {

        final StringBuilder stringBuilder = new StringBuilder();
        for (final T token : tokens) {
            stringBuilder.append(prefix).append(token.toString()).append(suffix);
        }
        return stringBuilder.toString();
    }

    private String getApp() throws MojoExecutionException {

        if (this.app == null) {
            throw new MojoExecutionException("Tag 'app' has to be defined in configuration!");
        }

        return this.app;
    }

    private Map<String, String> getAuthParams() {

        if (this.authorization == null) {
            return new HashMap<>();
        }

        return this.authorization;
    }

    private URI getEndpoint() throws MojoExecutionException {

        if (this.endpoint == null) {
            throw new MojoExecutionException("Tag 'endpoint' has to be defined in configuration!");
        }

        return this.endpoint;
    }

    private FileSet getFileSet() {

        return this.fileSet;
    }

    private List<FileSet> getFileSets() {

        return this.fileSets;
    }

    private String getMethod() {

        if (this.method == null) {
            return DEFAULT_METHOD;
        }

        return this.method;
    }

    private String getRealms() {

        if (this.realms == null) {
            return "";
        }

        return this.realms;
    }

    private MediaType getRequestType() {

        if (WILDCARD_TYPE.equals(this.requestType)) {
            return DEFAULT_REQUEST_TYPE;
        }

        return this.requestType;
    }

    private String getResource() throws MojoExecutionException {

        if (this.resource == null) {
            throw new MojoExecutionException("Tag 'resource' has to be defined in configuration!");
        }

        return this.resource;
    }
}