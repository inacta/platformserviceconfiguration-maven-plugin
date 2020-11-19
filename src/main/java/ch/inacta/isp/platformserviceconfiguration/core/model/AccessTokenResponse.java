package ch.inacta.isp.platformserviceconfiguration.core.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Model of an access token response
 *
 * @author Inacta AG
 * @since 1.0.0
 */
public class AccessTokenResponse {

    @JsonProperty("access_token")
    String accessToken;
    @JsonProperty("expires_in")
    int expiresIn;
    @JsonProperty("refresh_expires_in")
    int refreshExpiresIn;
    @JsonProperty("refresh_token")
    String refreshToken;
    @JsonProperty("token_type")
    String tokenType;
    @JsonProperty("not-before-policy")
    int notBeforePolicy;
    @JsonProperty("session_state")
    String sessionState;
    String scope;

    /**
     * Gets the value of the accessToken property.
     *
     * @return possible object is {@link String}
     */
    public String getAccessToken() {

        return this.accessToken;
    }

    /**
     * Sets the value of the accessToken property
     *
     * @param accessToken
     *            allowed object is {@link String}
     * @return the {@link AccessTokenResponse}
     */
    public AccessTokenResponse setAccessToken(final String accessToken) {

        this.accessToken = accessToken;
        return this;
    }

    /**
     * Gets the value of the expiresIn property.
     *
     * @return possible object is int
     */
    public int getExpiresIn() {

        return this.expiresIn;
    }

    /**
     * Sets the value of the expiresIn property
     *
     * @param expiresIn
     *            allowed object is int
     * @return the {@link AccessTokenResponse}
     */
    public AccessTokenResponse setExpiresIn(final int expiresIn) {

        this.expiresIn = expiresIn;
        return this;
    }

    /**
     * Gets the value of the refreshExpiresIn property.
     *
     * @return possible object is int
     */
    public int getRefreshExpiresIn() {

        return this.refreshExpiresIn;
    }

    /**
     * Sets the value of the refreshExpiresIn property
     *
     * @param refreshExpiresIn
     *            allowed object is int
     * @return the {@link AccessTokenResponse}
     */
    public AccessTokenResponse setRefreshExpiresIn(final int refreshExpiresIn) {

        this.refreshExpiresIn = refreshExpiresIn;
        return this;
    }

    /**
     * Gets the value of the refreshToken property.
     *
     * @return possible object is {@link String}
     */
    public String getRefreshToken() {

        return this.refreshToken;
    }

    /**
     * Sets the value of the refreshToken property
     *
     * @param refreshToken
     *            allowed object is {@link String}
     * @return the {@link AccessTokenResponse}
     */
    public AccessTokenResponse setRefreshToken(final String refreshToken) {

        this.refreshToken = refreshToken;
        return this;
    }

    /**
     * Gets the value of the tokenType property.
     *
     * @return possible object is {@link String}
     */
    public String getTokenType() {

        return this.tokenType;
    }

    /**
     * Sets the value of the tokenType property
     *
     * @param tokenType
     *            allowed object is {@link String}
     * @return the {@link AccessTokenResponse}
     */
    public AccessTokenResponse setTokenType(final String tokenType) {

        this.tokenType = tokenType;
        return this;
    }

    /**
     * Gets the value of the notBeforePolicy property.
     *
     * @return possible object is int
     */
    public int getNotBeforePolicy() {

        return this.notBeforePolicy;
    }

    /**
     * Sets the value of the notBeforePolicy property
     *
     * @param notBeforePolicy
     *            allowed object is int
     * @return the {@link AccessTokenResponse}
     */
    public AccessTokenResponse setNotBeforePolicy(final int notBeforePolicy) {

        this.notBeforePolicy = notBeforePolicy;
        return this;
    }

    /**
     * Gets the value of the sessionState property.
     *
     * @return possible object is {@link String}
     */
    public String getSessionState() {

        return this.sessionState;
    }

    /**
     * Sets the value of the sessionState property
     *
     * @param sessionState
     *            allowed object is {@link String}
     * @return the {@link AccessTokenResponse}
     */
    public AccessTokenResponse setSessionState(final String sessionState) {

        this.sessionState = sessionState;
        return this;
    }

    /**
     * Gets the value of the scope property.
     *
     * @return possible object is {@link String}
     */
    public String getScope() {

        return this.scope;
    }

    /**
     * Sets the value of the scope property
     *
     * @param scope
     *            allowed object is {@link String}
     * @return the {@link AccessTokenResponse}
     */
    public AccessTokenResponse setScope(final String scope) {

        this.scope = scope;
        return this;
    }
}
