package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getEncoder;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.maven.plugin.MojoExecutionException;

import ch.inacta.maven.platformserviceconfiguration.core.model.AccessTokenResponse;

/**
 * Strategy to handle rabbitmq specific authorization.
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class RabbitMQStrategy implements AuthorizationStrategy {

    private static final MediaType REQUEST_TYPE = APPLICATION_JSON_TYPE;
    private static final MediaType RESPONSE_TYPE = APPLICATION_JSON_TYPE;

    @Override
    public AccessTokenResponse authorize(final Map<String, String> authParams) throws MojoExecutionException {

        if (!authParams.containsKey("username")) {
            throw new MojoExecutionException("Tag 'username' has to be defined in authorization!");
        }
        if (!authParams.containsKey("password")) {
            throw new MojoExecutionException("Tag 'password' has to be defined in authorization!");
        }

        final String username = authParams.get("username");
        final String password = authParams.get("password");

        final AccessTokenResponse accessTokenResponse = new AccessTokenResponse();
        accessTokenResponse.setTokenType("Basic");

        final String encodedToken = getEncoder().encodeToString((username + ":" + password).getBytes(UTF_8));
        accessTokenResponse.setAccessToken(encodedToken);

        return accessTokenResponse;
    }

    @Override
    public MediaType getRequestType() {

        return REQUEST_TYPE;
    }

    @Override
    public MediaType getResponseType() {

        return RESPONSE_TYPE;
    }

    @Override
    public String getStrategyName() {

        return "RabbitMQStrategy";
    }
}
