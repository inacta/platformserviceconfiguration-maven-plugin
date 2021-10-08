package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static ch.inacta.maven.platformserviceconfiguration.core.strategy.ResourceMode.CREATE;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;
import static java.util.Base64.getEncoder;
import static javax.ws.rs.client.ClientBuilder.newClient;
import static javax.ws.rs.client.Entity.json;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;
import static org.apache.commons.lang3.StringUtils.join;
import static org.keycloak.OAuth2Constants.PASSWORD;
import static org.keycloak.OAuth2Constants.USERNAME;

import java.util.Optional;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Response;

import org.apache.maven.plugin.MojoExecutionException;
import org.glassfish.jersey.jackson.JacksonFeature;

import ch.inacta.maven.platformserviceconfiguration.core.Plugin;
import ch.inacta.maven.platformserviceconfiguration.core.model.AccessTokenResponse;

/**
 * Strategy to handle RabbitMQ specific configuration tasks.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
class RabbitMQStrategy {

    private static final String BASIC_TOKEN_TYPE = "Basic";
    private final Plugin plugin;
    private final RabbitMQResource resource;

    /**
     * Default constructor.
     *
     * @param plugin
     *            this plugin with all the called parameters
     */
    RabbitMQStrategy(final Plugin plugin) throws MojoExecutionException {

        this.plugin = plugin;
        this.resource = RabbitMQResource.fromString(this.plugin.getResource()).orElseThrow(
                () -> new MojoExecutionException(format("Tag 'resource' must be one of the values: [%s]", join(RabbitMQResource.values(), ", "))));
    }

    /**
     * Simple REST call to create or delete the specified resource.
     */
    void invokeAPI() throws MojoExecutionException {

        final Response response = executeRequest();
        processResponse(response);
    }

    private Response executeRequest() {

        final AccessTokenResponse accessTokenResponse = getAccessTokenResponse();
        final Invocation.Builder builder = newClient().register(JacksonFeature.class).target(this.plugin.getEndpoint())
                .path(this.resource.getPath() + this.plugin.getResourceName()).request(APPLICATION_JSON_TYPE)
                .header(AUTHORIZATION, accessTokenResponse.getTokenType() + " " + accessTokenResponse.getAccessToken());
        return this.plugin.getMode() == CREATE ? builder.put(this.resource.getEntity()) : builder.delete();
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
            this.plugin.getLog().info(format("- %s [%s] with name [%s] was successful, status code: [%d]", this.plugin.getMode().name(),
                    this.plugin.getResource(), this.plugin.getResourceName(), response.getStatus()));
        } else {
            this.plugin.getLog().warn(format("- %s [%s] with name [%s] was not successful, error code: [%d]", this.plugin.getMode().name(),
                    this.plugin.getResource(), this.plugin.getResourceName(), response.getStatus()));
            throw new MojoExecutionException(format("Unable to %s [%s] with name [%s]", this.plugin.getMode().name(), this.plugin.getResource(),
                    this.plugin.getResourceName()));
        }
    }

    private enum RabbitMQResource {

        QUEUE {

            @Override
            String getPath() {

                return "api/queues/";
            }

            @Override
            Entity<String> getEntity() {

                return json("{}");
            }
        };

        /**
         * Gets the path for the specified resource.
         *
         * @return path
         */
        abstract String getPath();

        /**
         * Gets the request entity.
         *
         * @return request entity
         */
        abstract Entity<String> getEntity();

        private static Optional<RabbitMQResource> fromString(final String resource) {

            return stream(RabbitMQResource.values()).filter(keycloakResource -> keycloakResource.toString().equalsIgnoreCase(resource)).findAny();
        }
    }
}