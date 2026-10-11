package cl.duoc.digitalfix.bff.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/** digitalfix.security.tenants: tenants de Entra ID cuyos tokens acepta el BFF. */
@ConfigurationProperties("digitalfix.security")
public record EntraTenantsProperties(List<EntraTenant> tenants) {

    public EntraTenantsProperties {
        tenants = tenants == null ? List.of() : List.copyOf(tenants);
    }
}
