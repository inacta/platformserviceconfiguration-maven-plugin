package ch.inacta.isp.platformserviceconfiguration.core.strategy;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.Response.Status.Family.SUCCESSFUL;

import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import ch.inacta.isp.platformserviceconfiguration.core.model.AccessTokenResponse;

/**
 * Strategy to handle keycloak specific authorization.
 *
 * @author INACTA AG
 * @since 1.0.0
 */
public class KeycloakStrategy implements AuthorizationStrategy {

    private static final String AUTHORIZATION_RESOURCE = "auth/realms/master/protocol/openid-connect/token";
    private static final MediaType REQUEST_TYPE = APPLICATION_JSON_TYPE;
    private static final MediaType RESPONSE_TYPE = APPLICATION_OCTET_STREAM_TYPE;
    private static final String GRANT_TYPE = "password";
    private static final String CLIENT_ID = "admin-cli";

    private final Log logger;
    private WebTarget webTarget;

    /**
     * Default constructor
     * 
     * @param logger
     *            to write logs
     */
    public KeycloakStrategy(final Log logger) {

        this.logger = logger;
    }

    @Override
    public AccessTokenResponse authorize(final Map<String, String> authParams) throws MojoExecutionException {

        if (this.webTarget == null) {
            throw new MojoExecutionException("No webtarget has been set!");
        }

        if (!authParams.containsKey("grant_type")) {
            authParams.put("grant_type", GRANT_TYPE);
        }
        if (!authParams.containsKey("client_id")) {
            authParams.put("client_id", CLIENT_ID);
        }

        this.webTarget = this.webTarget.path(AUTHORIZATION_RESOURCE);
        final Invocation.Builder builder = this.webTarget.request(APPLICATION_FORM_URLENCODED_TYPE).accept(APPLICATION_JSON);
        final Response response = builder.method("POST", Entity.form(getFormParameters(authParams)));

        if (response.getStatusInfo().getFamily() == SUCCESSFUL) {
            return response.readEntity(AccessTokenResponse.class);
        } else {
            this.logger.error("Failed to authorize request!");
            this.logger.error(String.format("Endpoint: POST %s", this.webTarget.getUri().toString()));
            this.logger.error(String.format("Parameters: %s", StringUtils.join(authParams, ", ")));
            throw new MojoExecutionException("Failed to authorize request!");
        }
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

        return "KeycloakStrategy";
    }

    /**
     * Sets the webtarget to get the access token
     *
     * @param webTarget
     *            webtarget to the authorization endpoint
     */
    public void setWebTarget(final WebTarget webTarget) {

        this.webTarget = webTarget;
    }

    private Form getFormParameters(final Map<String, String> formParams) {

        final Form form = new Form();
        for (final Map.Entry<String, String> entry : formParams.entrySet()) {
            form.param(entry.getKey(), entry.getValue());
            this.logger.debug(String.format("Form-param [%s:%s]", entry.getKey(), entry.getValue()));
        }
        return form;
    }
}
