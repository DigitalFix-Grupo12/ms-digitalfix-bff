package cl.duoc.digitalfix.bff.config;

import cl.duoc.digitalfix.bff.security.EntraTenantsProperties;
import cl.duoc.digitalfix.bff.security.MultiTenantJwtResolver;
import cl.duoc.digitalfix.bff.security.RestAuthEntryPoints;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(EntraTenantsProperties.class)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${digitalfix.security.allowed-origins}")
    private String allowedOrigins;

    /**
     * Valida los JWT de todos los tenants de Entra ID configurados (Kevin y Boris).
     * Por cada token: issuer en la lista de confianza + firma (JWKS del tenant) +
     * expiracion + audience = client ID de la "digitalfix-api" de ese tenant.
     * Ver MultiTenantJwtResolver y JwtAudienceValidator.
     */
    @Bean
    public JwtIssuerAuthenticationManagerResolver jwtIssuerResolver(EntraTenantsProperties properties) {
        log.info("Tenants Entra ID de confianza: {}", MultiTenantJwtResolver.issuers(properties.tenants()));
        return MultiTenantJwtResolver.create(properties.tenants());
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtIssuerAuthenticationManagerResolver jwtIssuerResolver) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionCreationPolicy(org.springframework.security.config.http.SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/api/audit/**").hasAnyRole("Admin", "Auditor")
                .requestMatchers("/api/report/**").hasRole("Admin")
                .requestMatchers("/api/rabbit/**").hasRole("Admin")
                .requestMatchers("/api/catalog/**").hasAnyRole("Admin", "Supervisor")
                .requestMatchers("/api/workorders/**").hasAnyRole("Admin", "Supervisor", "Cliente")
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationManagerResolver(jwtIssuerResolver)
                .authenticationEntryPoint(RestAuthEntryPoints.unauthorizedEntryPoint())
                .accessDeniedHandler(RestAuthEntryPoints.accessDeniedHandler()));

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(allowedOrigins.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
