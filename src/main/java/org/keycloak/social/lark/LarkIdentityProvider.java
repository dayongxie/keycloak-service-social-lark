package org.keycloak.social.lark;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Strings;
import org.jboss.logging.Logger;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.AbstractOAuth2IdentityProvider;
import org.keycloak.broker.oidc.OAuth2IdentityProviderConfig;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.broker.provider.util.IdentityBrokerState;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.social.SocialIdentityProvider;
import org.keycloak.common.ClientConnection;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureSignerContext;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.http.HttpRequest;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jwk.RSAPublicJWK;
import org.keycloak.jose.jws.JWSBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakContext;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.services.ErrorPage;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.ClientSessionCode;
import org.keycloak.services.messages.Messages;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.util.JsonSerialization;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.keycloak.vault.VaultStringSecret;

import java.io.IOException;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LarkIdentityProvider extends AbstractOAuth2IdentityProvider<OAuth2IdentityProviderConfig>
        implements SocialIdentityProvider<OAuth2IdentityProviderConfig> {
    protected static final Logger logger = Logger.getLogger(LarkIdentityProvider.class);

    public static final String AUTH_URL = "https://accounts.feishu.cn/open-apis/authen/v1/authorize";
    public static final String DEFAULT_SCOPE = "contact:user.base:readonly";
    public static final String PROFILE_URL = "https://open.feishu.cn/open-apis/authen/v1/user_info?lang=zh_CN";
    public static final String TOKEN_URL = "https://open.feishu.cn/open-apis/authen/v2/oauth/token";

    public static final String USER_ATTRIBUTE_PHONE_NUMBER = "phone_number";

    public static final String RESPONSE_CODE_SUCCESS = "0";

    public LarkIdentityProvider(KeycloakSession session, OAuth2IdentityProviderConfig config) {
        super(session, config);
        config.setAuthorizationUrl(AUTH_URL);
        config.setTokenUrl(TOKEN_URL);
        config.setUserInfoUrl(PROFILE_URL);
    }

    public Object callback(RealmModel realm, IdentityProvider.AuthenticationCallback callback, EventBuilder event) {
        return new AbstractOAuth2IdentityProvider.Endpoint(callback, realm, event, this);
    }

    protected boolean supportsExternalExchange() {
        return true;
    }

    protected BrokeredIdentityContext extractIdentityFromProfile(EventBuilder event, JsonNode profile) {
        logger.info("extractIdentityFromProfile " + profile.toString());
        JsonNode userInfo = profile.get("data");
        String unionId = getJsonProperty(userInfo, "union_id");
        BrokeredIdentityContext user = new BrokeredIdentityContext(
                (unionId != null && !unionId.isEmpty() ? unionId : getJsonProperty(userInfo, "open_id")),
                getConfig());
        String name = getJsonProperty(userInfo, "name");
        String email = getJsonProperty(userInfo, "email");
        user.setUsername(name);
        user.setBrokerUserId(getJsonProperty(userInfo, "user_id"));
        user.setModelUsername(Optional.ofNullable(email).orElse("input your email"));

        user.setEmail(Optional.ofNullable(email).orElse("input your email"));
        user.setIdp(this);
        user.setUserAttribute(USER_ATTRIBUTE_PHONE_NUMBER, getJsonProperty(userInfo, "mobile"));

        LarkProfileMapper.storeUserProfileForMapper(user, profile, getConfig().getAlias());
        return user;
    }

    @Override
    protected BrokeredIdentityContext doGetFederatedIdentity(String accessToken) {
        try {
            JsonNode profile = SimpleHttp.doGet(getConfig().getUserInfoUrl(), session).header("Authorization", "Bearer " + accessToken).asJson();

            return extractIdentityFromProfile(null, profile);
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not obtain user profile from lark.", e);
        }
    }

    @Override
    protected String extractTokenFromResponse(String response, String tokenName) {
        if (response == null) {
            return null;
        } else if (response.startsWith("{")) {
            try {
                JsonNode node = mapper.readTree(response);
                if (node.has(tokenName)) {
                    String s = node.get(tokenName).textValue();
                    return s != null && !s.trim().isEmpty() ? s : null;
                } else {
                    return null;
                }
            } catch (IOException e) {
                throw new IdentityBrokerException("Could not extract token [" + tokenName + "] from response [" + response + "] due: " + e.getMessage(), e);
            }
        } else {
            Matcher matcher = Pattern.compile(tokenName + "=([^&]+)").matcher(response);
            return matcher.find() ? matcher.group(1) : null;
        }
    }

    protected UriBuilder createAuthorizationUrl(AuthenticationRequest request) {
        return super.createAuthorizationUrl(request);
    }

    protected String getDefaultScopes() {
        return DEFAULT_SCOPE;
    }
}