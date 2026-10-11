package cl.duoc.digitalfix.bff.security;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MultiTenantJwtResolverTest {

    private static final EntraTenant KEVIN = new EntraTenant("tenant-kevin", "api-kevin");
    private static final EntraTenant BORIS = new EntraTenant("tenant-boris", "api-boris");

    /** JWT con firma falsa: solo sirve para que el resolver lea el claim "iss". */
    static String unsignedToken(String issuer) {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes(StandardCharsets.UTF_8));
        String payload = b64.encodeToString(("{\"iss\":\"" + issuer + "\",\"aud\":\"x\"}").getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".c2lnbmF0dXJh";
    }

    @Test
    void confiaEnLosDosTenants_yUsaElIssuerV2() {
        assertThat(MultiTenantJwtResolver.trustedIssuers(List.of(KEVIN, BORIS)))
            .containsOnlyKeys(
                "https://login.microsoftonline.com/tenant-kevin/v2.0",
                "https://login.microsoftonline.com/tenant-boris/v2.0");
    }

    @Test
    void ignoraTenantSinConfigurar() {
        assertThat(MultiTenantJwtResolver.trustedIssuers(List.of(KEVIN, new EntraTenant("", ""))))
            .containsOnlyKeys("https://login.microsoftonline.com/tenant-kevin/v2.0");
    }

    @Test
    void sinTenants_noArranca() {
        assertThatThrownBy(() -> MultiTenantJwtResolver.trustedIssuers(List.of(new EntraTenant(null, null))))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void rechazaTokenDeIssuerNoConfiado() {
        AuthenticationManager manager = MultiTenantJwtResolver.create(List.of(KEVIN, BORIS))
            .resolve(new MockHttpServletRequest());
        String token = unsignedToken("https://login.microsoftonline.com/otro-tenant/v2.0");

        assertThatThrownBy(() -> manager.authenticate(new BearerTokenAuthenticationToken(token)))
            .isInstanceOf(InvalidBearerTokenException.class);
    }
}
