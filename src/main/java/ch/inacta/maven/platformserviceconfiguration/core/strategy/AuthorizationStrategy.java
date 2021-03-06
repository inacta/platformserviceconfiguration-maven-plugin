package ch.inacta.maven.platformserviceconfiguration.core.strategy;

import java.util.Map;

import javax.ws.rs.core.MediaType;

import org.apache.maven.plugin.MojoExecutionException;

import ch.inacta.maven.platformserviceconfiguration.core.model.AccessTokenResponse;

/**
 * Interface for authorization strategies
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public interface AuthorizationStrategy {

    /**
     * Authorizes the request.
     * 
     * @param authParams
     *            needed authorization parameters
     * @return AccessTokenResponse
     * @throws MojoExecutionException
     *             if authorization fails
     */
    AccessTokenResponse authorize(Map<String, String> authParams) throws MojoExecutionException;

    /**
     * Gets the default request type
     * 
     * @return MediaType
     */
    MediaType getRequestType();

    /**
     * Gets the default response type
     *
     * @return MediaType
     */
    MediaType getResponseType();

    /**
     * Gets the name of the strategy.
     * 
     * @return strategy name
     */
    String getStrategyName();
}
