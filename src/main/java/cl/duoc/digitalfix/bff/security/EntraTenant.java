package cl.duoc.digitalfix.bff.security;

import org.springframework.util.StringUtils;

/**
 * Tenant de Microsoft Entra ID en el que el BFF confia.
 *
 * @param tenantId Directory (tenant) ID
 * @param audience Application (client) ID del registro "digitalfix-api" de ESE tenant
 *                 (claim "aud" de los access tokens v2.0)
 */
public record EntraTenant(String tenantId, String audience) {

    /** Issuer de los access tokens v2.0 del tenant (requestedAccessTokenVersion = 2). */
    public String issuer() {
        return "https://login.microsoftonline.com/" + tenantId + "/v2.0";
    }

    /** Un tenant sin IDs (ej. AZURE_TENANT_ID_2 vacio) se ignora. */
    public boolean configured() {
        return StringUtils.hasText(tenantId) && StringUtils.hasText(audience);
    }
}
