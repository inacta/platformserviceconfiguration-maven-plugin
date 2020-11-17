package ch.inacta.isp.platformserviceconfiguration.core.strategy;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;

import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

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

    private static final MediaType REQUEST_TYPE = APPLICATION_JSON_TYPE;
    private static final MediaType RESPONSE_TYPE = APPLICATION_OCTET_STREAM_TYPE;
    private static final String AUTHORIZATION_RESOURCE = "auth/realms/master/protocol/openid-connect/token";
    private static final String GRANT_TYPE = "password";
    private static final String CLIENT_ID = "admin_cli";

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

        authParams.put("grant_type", GRANT_TYPE);
        authParams.put("client_id", CLIENT_ID);

        this.webTarget = this.webTarget.path(AUTHORIZATION_RESOURCE);
        final Invocation.Builder builder = this.webTarget.request(APPLICATION_FORM_URLENCODED_TYPE).accept(APPLICATION_JSON);
        final Response response = builder.method("POST", Entity.form(getFormParameters(authParams)));
        return response.readEntity(AccessTokenResponse.class);
    }

    @Override
    public MediaType getRequestType() {

        return REQUEST_TYPE;
    }

    @Override
    public MediaType getResponseType() {

        return RESPONSE_TYPE;
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
