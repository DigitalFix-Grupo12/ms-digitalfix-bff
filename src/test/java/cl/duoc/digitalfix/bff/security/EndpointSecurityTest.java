package cl.duoc.digitalfix.bff.security;

import cl.duoc.digitalfix.bff.config.SecurityConfig;
import cl.duoc.digitalfix.bff.controller.ReportController;
import cl.duoc.digitalfix.bff.controller.WorkOrdersController;
import cl.duoc.digitalfix.bff.repository.WorkOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas basicas de seguridad del BFF: confirman que el filtro de JWT
 * bloquea peticiones sin token (401) y peticiones con rol insuficiente
 * (403), y que un rol correcto sí pasa (200). Es el corazón de lo que pide
 * la evaluación: "el backend debe responder a pruebas básicas".
 */
@WebMvcTest(controllers = {WorkOrdersController.class, ReportController.class})
@Import(SecurityConfig.class)
class EndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder; // evita resolver el issuer real de Entra ID en el test

    @MockBean
    private WorkOrderRepository workOrderRepository;

    @Test
    void sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/workorders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void conRolInsuficiente_devuelve403() throws Exception {
        mockMvc.perform(get("/api/report/kpis")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void conRolCorrecto_devuelve200() throws Exception {
        mockMvc.perform(get("/api/workorders")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
            .andExpect(status().isOk());
    }
}
