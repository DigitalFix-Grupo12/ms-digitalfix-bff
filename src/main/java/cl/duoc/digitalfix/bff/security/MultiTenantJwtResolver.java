package cl.duoc.digitalfix.bff.security;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationManagerResolver;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Valida access tokens de VARIOS tenants de Entra ID (uno por integrante del equipo).
 *
 * Flujo por request:
 *  1. Se lee el claim "iss" del token (sin confiar aun en el).
 *  2. Si el issuer no esta en la lista de tenants configurados -> 401 (invalid issuer).
 *  3. Si esta, se valida con el decoder de ESE tenant: firma con sus claves JWKS,
 *     expiracion, issuer exacto y audience = client ID de su "digitalfix-api".
 *
 * Los decoders se crean en el primer uso (descarga el documento OIDC del tenant)
 * y quedan en cache; si la descarga falla, el request recibe 401 y se reintenta
 * en el siguiente.
 */
public final class MultiTenantJwtResolver {

    private MultiTenantJwtResolver() {
    }

    public static Map<String, EntraTenant> trustedIssuers(Collection<EntraTenant> tenants) {
        Map<String, EntraTenant> trusted = new LinkedHashMap<>();
        tenants.stream().filter(EntraTenant::configured).forEach(t -> trusted.putIfAbsent(t.issuer(), t));
        if (trusted.isEmpty()) {
            throw new IllegalStateException(
                "No hay tenants de Entra ID configurados (AZURE_TENANT_ID / AZURE_API_CLIENT_ID).");
        }
        return Map.copyOf(trusted);
    }

    public static JwtIssuerAuthenticationManagerResolver create(Collection<EntraTenant> tenants) {
        Map<String, EntraTenant> trusted = trustedIssuers(tenants);
        Map<String, AuthenticationManager> managers = new ConcurrentHashMap<>();

        AuthenticationManagerResolver<String> byIssuer = issuer -> {
            EntraTenant tenant = trusted.get(issuer);
            if (tenant == null) {
                return null; // JwtIssuerAuthenticationManagerResolver -> InvalidBearerTokenException (401)
            }
            return managers.computeIfAbsent(issuer, i -> authenticationManager(tenant));
        };
        return new JwtIssuerAuthenticationManagerResolver(byIssuer);
    }

    public static Set<String> issuers(Collection<EntraTenant> tenants) {
        return trustedIssuers(tenants).keySet();
    }

    private static AuthenticationManager authenticationManager(EntraTenant tenant) {
        NimbusJwtDecoder decoder;
        try {
            decoder = (NimbusJwtDecoder) JwtDecoders.fromIssuerLocation(tenant.issuer());
        } catch (RuntimeException e) {
            throw new AuthenticationServiceException("No se pudo obtener la configuracion OIDC de " + tenant.issuer(), e);
        }
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
            JwtValidators.createDefaultWithIssuer(tenant.issuer()),
            new JwtAudienceValidator(tenant.audience())));

        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(decoder);
        provider.setJwtAuthenticationConverter(new EntraIdRolesConverter());
        return provider::authenticate;
    }
}
