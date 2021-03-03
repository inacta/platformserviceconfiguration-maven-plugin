package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getEncoder;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.client.Entity.form;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;
import org.glassfish.jersey.jackson.JacksonFeature;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;
import ch.inacta.maven.platformserviceconfiguration.core.model.AccessTokenResponse;
import ch.inacta.maven.platformserviceconfiguration.core.model.ErrorInfo;

/**
 * Strategy to handle rabbitmq specific authorization and functionalities.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
class RabbitMQStrategy {

    private final Plugin plugin;
    private final Log logger;

    /**
     * Default constructor
     *
     * @param plugin
     *            this plugin with all the called parameters
     */
    RabbitMQStrategy(final Plugin plugin) {

        this.plugin = plugin;
        this.logger = plugin.getLog();
    }

    /**
     * REST API call to create a queue
     */
    void createQueue() throws MojoExecutionException {

        final Invocation.Builder builder = createClientBuilder();
        executeRequest(builder);
    }

    private Invocation.Builder createClientBuilder() {

        final AccessTokenResponse accessTokenResponse = getAccessTokenResponse();
        return newClient().register(JacksonFeature.class).target(this.plugin.getEndpoint()).path(this.plugin.getResource())
                .request(APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE)
                .header("Authorization", accessTokenResponse.getTokenType() + " " + accessTokenResponse.getAccessToken());
    }

    private AccessTokenResponse getAccessTokenResponse() {

        final String username = this.plugin.getAuthorization().get("username");
        final String password = this.plugin.getAuthorization().get("password");

        final AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        accessTokenResponse.setTokenType("Basic");

        final String encodedToken = getEncoder().encodeToString((username + ":" + password).getBytes(UTF_8));
        accessTokenResponse.setAccessToken(encodedToken);

        return accessTokenResponse;
    }

    private void executeRequest(final Invocation.Builder builder) throws MojoExecutionException {

        final Response response = builder.put(form(new Form()));
        processResponse(response);
    }

    private void processResponse(final Response response) throws MojoExecutionException {

        if (response.getStatusInfo().getFamily() == SUCCESSFUL) {
            this.logger.info(format("Status: [%d]", response.getStatus()));
        } else {
            this.logger.warn(format("Error code: [%d]", response.getStatus()));
            final ErrorInfo errorInfo = new ErrorInfo(response.getStatus(), response.getEntity().toString());
            throw new MojoExecutionException(format("Unable to create queue: %s", errorInfo.toString()));
        }
    }
}
