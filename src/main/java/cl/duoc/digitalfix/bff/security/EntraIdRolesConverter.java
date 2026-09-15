package cl.duoc.digitalfix.bff.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entra ID entrega los App Roles asignados al usuario en el claim "roles"
 * (array de strings: "Admin", "Supervisor", "Cliente", "Auditor"), NO en
 * "scope"/"scp" (eso son los delegated scopes, no los roles de negocio).
 *
 * Este converter los transforma en authorities "ROLE_Admin", "ROLE_Supervisor",
 * etc. para poder usar .hasRole("Admin") o @PreAuthorize("hasRole('Admin')")
 * en el resto de la app, conservando el Jwt original como principal
 * (accesible vía @AuthenticationPrincipal Jwt jwt en los controllers).
 */
public class EntraIdRolesConverter implements Converter<Jwt, JwtAuthenticationToken> {

    @Override
    public JwtAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = extractRoles(jwt).stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                .collect(Collectors.toList());

        return new JwtAuthenticationToken(jwt, authorities);
    }

    @SuppressWarnings("unchecked")
    private List<String> extractRoles(Jwt jwt) {
        Object roles = jwt.getClaims().get("roles");
        if (roles instanceof List<?> list) {
            return (List<String>) list;
        }
        return List.of();
    }
}
