package cl.duoc.digitalfix.bff.security;

import cl.duoc.digitalfix.bff.config.SecurityConfig;
import cl.duoc.digitalfix.bff.controller.AuditController;
import cl.duoc.digitalfix.bff.controller.ReportController;
import cl.duoc.digitalfix.bff.controller.WorkOrdersController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Seguridad del BFF (gateway):
 *  - sin token -> 401, rol insuficiente -> 403, ANTES de tocar aguas abajo.
 *  - rol correcto -> la peticion SI se reenvia; como en el test no hay
 *    microservicio escuchando (puertos 190xx), el BFF responde 503 JSON.
 *    Eso prueba que la autorizacion dejo pasar y que el proxy degrada bien.
 */
@WebMvcTest(controllers = {WorkOrdersController.class, ReportController.class, AuditController.class})
@Import(SecurityConfig.class)
class EndpointSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder; // evita resolver el issuer real de Entra ID en el test

    @Test
    void sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/workorders"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void clienteNoVeReportes_403() throws Exception {
        mockMvc.perform(get("/api/report/kpis")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void clienteNoCambiaEstados_403() throws Exception {
        mockMvc.perform(put("/api/workorders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"ASIGNADA\",\"tecnicoId\":\"t1\"}")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Cliente"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void supervisorNoVeAuditoria_403() throws Exception {
        mockMvc.perform(get("/api/audit")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Supervisor"))))
            .andExpect(status().isForbidden());
    }

    @Test
    void rolCorrecto_seReenvia_yServicioCaidoDa503() throws Exception {
        mockMvc.perform(get("/api/workorders")
                .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Supervisor"))))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.message").value("Servicio workorders no disponible"));
    }
}
