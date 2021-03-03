package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static java.lang.String.format;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.client.Entity.form;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.lang3.StringUtils.join;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.glassfish.jersey.jackson.JacksonFeature;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;
import ch.inacta.maven.platformserviceconfiguration.core.model.AccessTokenResponse;
import ch.inacta.maven.platformserviceconfiguration.core.model.ErrorInfo;
import ch.inacta.maven.platformserviceconfiguration.core.model.FileErrorInfo;

/**
 * Strategy to handle keycloak specific authorization and functionalities.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
class KeycloakStrategy {

    private static final String AUTHORIZATION_RESOURCE = "auth/realms/master/protocol/openid-connect/token";
    private static final String CLIENT_ID = "client_id";
    private static final String GRANT_TYPE = "grant_type";
    private static final String PASSWORD = "password";
    private static final String ADMIN_CLI = "admin-cli";
    private static final String REALM_PLACEHOLDER = "%4T";

    private final Plugin plugin;
    private final Log logger;

    /**
     * Default constructor
     *
     * @param plugin
     *            this plugin with all the called parameters
     */
    KeycloakStrategy(final Plugin plugin) {

        this.plugin = plugin;
        this.logger = plugin.getLog();
    }

    /**
     * Admin REST API call for the given resources with the JSON files.
     */
    void postJSONFiles() throws MojoExecutionException {

        final List<ErrorInfo> errorInfos = new ArrayList<>();

        for (final String resourcePath : getResourcePaths()) {
            final Invocation.Builder builder = createClientBuilder(resourcePath);
            errorInfos.addAll(executeRequests(builder));
        }

        if (!errorInfos.isEmpty()) {
            throw new MojoExecutionException(format("Unable to process files: %n%s", wrap(errorInfos)));
        }
    }

    private List<String> getResourcePaths() throws MojoExecutionException {

        final List<String> resourcePaths = new ArrayList<>();

        if (this.plugin.getRealms().isEmpty()) {

            if (this.plugin.getResource().contains(REALM_PLACEHOLDER)) {
                throw new MojoExecutionException("No realms are defined!");
            }
            resourcePaths.add(this.plugin.getResource());

        } else {

            if (!this.plugin.getResource().contains(REALM_PLACEHOLDER)) {
                throw new MojoExecutionException(format("No placeholder symbol '%s' for realms found!", REALM_PLACEHOLDER));
            }

            for (final String realm : this.plugin.getRealms().replace(" ", "").split(",")) {
                resourcePaths.add(this.plugin.getResource().replace(REALM_PLACEHOLDER, realm));
            }
        }

        return resourcePaths;
    }

    private Invocation.Builder createClientBuilder(final String resourcePath) throws MojoExecutionException {

        final AccessTokenResponse accessTokenResponse = getAccessTokenResponse();
        return newClient().register(JacksonFeature.class).target(this.plugin.getEndpoint()).path(resourcePath)
                .request(APPLICATION_JSON_TYPE, APPLICATION_OCTET_STREAM_TYPE)
                .header("Authorization", accessTokenResponse.getTokenType() + " " + accessTokenResponse.getAccessToken());
    }

    private AccessTokenResponse getAccessTokenResponse() throws MojoExecutionException {

        if (!this.plugin.getAuthorization().containsKey(GRANT_TYPE)) {
            this.plugin.getAuthorization().put(GRANT_TYPE, PASSWORD);
        }
        if (!this.plugin.getAuthorization().containsKey(CLIENT_ID)) {
            this.plugin.getAuthorization().put(CLIENT_ID, ADMIN_CLI);
        }

        final Response response = newClient().register(JacksonFeature.class).target(this.plugin.getEndpoint()).path(AUTHORIZATION_RESOURCE)
                .request(APPLICATION_FORM_URLENCODED_TYPE).accept(APPLICATION_JSON).post(form(getFormParameters(this.plugin.getAuthorization())));

        if (response.getStatusInfo().getFamily() == SUCCESSFUL) {
            return response.readEntity(AccessTokenResponse.class);
        } else {
            this.logger.error(format("Failed to authorize request with parameters: %s", join(this.plugin.getAuthorization(), ", ")));
            throw new MojoExecutionException("Failed to authorize request!");
        }
    }

    private List<ErrorInfo> executeRequests(final Invocation.Builder builder) throws MojoExecutionException {

        final List<ErrorInfo> errorInfos = new ArrayList<>();

        for (final File file : this.plugin.getFilesToProcess()) {
            this.logger.info(format("Submitting file [%s]", file.toString()));
            final Optional<ErrorInfo> result = processResponse(builder.post(entity(file, APPLICATION_JSON_TYPE)));
            result.ifPresent(errorInfo -> errorInfos.add(new FileErrorInfo(file.getPath(), errorInfo)));
        }

        return errorInfos;
    }

    private Optional<ErrorInfo> processResponse(final Response response) {

        if (response.getStatusInfo().getFamily() == SUCCESSFUL) {
            this.logger.info(format("Status: [%d]", response.getStatus()));
            return Optional.empty();
        } else {
            this.logger.warn(format("Error code: [%d]", response.getStatus()));
            return Optional.of(new ErrorInfo(response.getStatus(), response.getEntity().toString()));
        }
    }

    private Form getFormParameters(final Map<String, String> formParams) {

        final Form form = new Form();
        for (final Map.Entry<String, String> entry : formParams.entrySet()) {
            form.param(entry.getKey(), entry.getValue());
            this.logger.debug(format("Form-param [%s:%s]", entry.getKey(), entry.getValue()));
        }
        return form;
    }

    private <T> String wrap(final List<T> tokens) {

        final StringBuilder stringBuilder = new StringBuilder();
        for (final T token : tokens) {
            stringBuilder.append(" ").append(token.toString()).append("%n");
        }
        return stringBuilder.toString();
    }
}
