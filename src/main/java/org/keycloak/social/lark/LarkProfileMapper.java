package org.keycloak.social.lark;

import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;

public class LarkProfileMapper extends AbstractJsonUserAttributeMapper {

    public static final String PROVIDER_ID = "lark-profile-mapper";

    @Override
    public String[] getCompatibleProviders() {
        return new String[]{LarkIdentityProviderFactory.PROVIDER_ID};
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
}    