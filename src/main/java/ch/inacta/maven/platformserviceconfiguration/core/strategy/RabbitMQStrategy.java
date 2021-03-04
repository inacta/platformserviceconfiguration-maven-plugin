package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getEncoder;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.client.Entity.form;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.keycloak.OAuth2Constants.PASSWORD;
import static org.keycloak.OAuth2Constants.USERNAME;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.Response;

import org.apache.maven.plugin.MojoExecutionException;
import org.glassfish.jersey.jackson.JacksonFeature;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;
import ch.inacta.maven.platformserviceconfiguration.core.model.AccessTokenResponse;
import ch.inacta.maven.platformserviceconfiguration.core.model.ErrorInfo;

/**
 * Strategy to handle RabbitMQ specific configuration tasks.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
class RabbitMQStrategy {

    private static final String BASIC_TOKEN_TYPE = "Basic";
    private static final String REST_API_FOR_CREATING_QUEUE = "api/queues/";
    private final Plugin plugin;

    /**
     * Default constructor.
     *
     * @param plugin
     *            this plugin with all the called parameters
     */
    RabbitMQStrategy(final Plugin plugin) {

        this.plugin = plugin;
    }

    /**
     * Simple REST call to create a queue.
     */
    void createQueue() throws MojoExecutionException {

        if (this.plugin.getResource().isEmpty()) {
            throw new MojoExecutionException("Tag 'resource' has to be defined with the queue name!");
        }

        final Response response = executeRequest(REST_API_FOR_CREATING_QUEUE + this.plugin.getResource());
        processResponse(response);
    }

    private Response executeRequest(final String queueName) {

        final AccessTokenResponse accessTokenResponse = getAccessTokenResponse();
        return newClient().register(JacksonFeature.class).target(this.plugin.getEndpoint()).path(queueName)
                .request(APPLICATION_JSON_TYPE, APPLICATION_JSON_TYPE)
                .header(AUTHORIZATION, accessTokenResponse.getTokenType() + " " + accessTokenResponse.getAccessToken()).put(form(new Form()));
    }

    private AccessTokenResponse getAccessTokenResponse() {

        final AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        accessTokenResponse.setTokenType(BASIC_TOKEN_TYPE);

        final String username = this.plugin.getAuthorization().get(USERNAME);
        final String password = this.plugin.getAuthorization().get(PASSWORD);
        final String encodedToken = getEncoder().encodeToString((username + ":" + password).getBytes(UTF_8));
        accessTokenResponse.setAccessToken(encodedToken);

        return accessTokenResponse;
    }

    private void processResponse(final Response response) throws MojoExecutionException {

        if (response.getStatusInfo().getFamily() == SUCCESSFUL) {
            this.plugin.getLog().info(format("Queue [%s] successfully created, status: [%d]", this.plugin.getResource(), response.getStatus()));
        } else {
            this.plugin.getLog().warn(format("Error code: [%d]", response.getStatus()));
            final ErrorInfo errorInfo = new ErrorInfo(response.getStatus(), response.getEntity().toString());
            throw new MojoExecutionException(format("Unable to create queue: %s", errorInfo.toString()));
        }
    }
}
