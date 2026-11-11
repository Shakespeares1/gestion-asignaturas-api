package com.gestionacademica.infrastructure.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import com.gestionacademica.application.port.output.PasswordHasherPort;
import com.gestionacademica.application.port.output.UsuarioRepositoryPort;
import com.gestionacademica.domain.model.Usuario;
import com.gestionacademica.domain.model.RolUsuario;
import com.gestionacademica.domain.model.EstadoCuenta;
import java.time.Instant;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestSecurityBeans.class)
class AuthorizationIntegrationTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private UsuarioRepositoryPort usuarios;
    @Autowired private PasswordHasherPort passwordHasher;

    @Test
    void peticionSinTokenNoAccedeAAsignaturas() throws Exception {
        mockMvc.perform(get("/api/asignaturas")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminPuedeGestionarUsuarios() throws Exception {
        mockMvc.perform(get("/api/usuarios").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void registroPublicoIgnoraIntentoDeElegirAdminYEstado() throws Exception {
        String email = "public-register-" + java.util.UUID.randomUUID() + "@example.org";
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombreCompleto\":\"Public User\",\"email\":\"" + email
                                + "\",\"password\":\"sample-only-password\",\"role\":\"ADMIN\",\"accountStatus\":\"INACTIVO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.accountStatus").value("ACTIVO"))
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
        Usuario created = usuarios.buscarPorEmail(email).orElseThrow();
        assertEquals(RolUsuario.USER, created.getRole());
        assertEquals(EstadoCuenta.ACTIVO, created.getAccountStatus());
    }

    @Test
    void userPuedeConsultarAsignaturasPeroNoUsuarios() throws Exception {
        mockMvc.perform(get("/api/asignaturas").with(user("student").roles("USER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/usuarios").with(user("student").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void loginDevuelveJwtYDatosPublicosSinHashes() throws Exception {
        usuarios.guardar(Usuario.registrar("API User", "api-user@example.org",
                passwordHasher.hash("correct-password"), Instant.now()));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"api-user@example.org\",\"password\":\"correct-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("test-token"))
                .andExpect(jsonPath("$.user.email").value("api-user@example.org"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void loginExcesivoDevuelve429ConRetryAfterSinExponerLaCuenta() throws Exception {
        String email = "throttle-" + java.util.UUID.randomUUID() + "@example.org";
        String body = "{\"email\":\"" + email + "\",\"password\":\"wrong-password\"}";
        for (int i = 0; i < 8; i++) {
            mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isUnauthorized());
        }
        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().exists("Retry-After"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(email))));
    }

    @Test
    void limitadorIpActuaAntesDeValidarElCuerpo() throws Exception {
        org.springframework.test.web.servlet.request.RequestPostProcessor sourceIp = request -> {
            request.setRemoteAddr("198.51.100.77");
            return request;
        };
        for (int i = 0; i < 30; i++) {
            mockMvc.perform(post("/api/auth/login").with(sourceIp)
                            .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/auth/login").with(sourceIp)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().exists("Retry-After"));
    }

    @Test
    void registroPublicoAplicaLimitePorCuentaYDevuelve429() throws Exception {
        String email = "register-limit-" + java.util.UUID.randomUUID() + "@example.org";
        String body = "{\"nombreCompleto\":\"Rate limited\",\"email\":\"" + email
                + "\",\"password\":\"long-valid-password\"}";
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/usuarios").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().exists("Retry-After"));
    }

    @Test
    void recuperacionLimitadaMantieneRespuestaGenericaYDa429AlExceso() throws Exception {
        String email = "unknown-limit-" + java.util.UUID.randomUUID() + "@example.org";
        String body = "{\"email\":\"" + email + "\"}";
        String accepted = null;
        for (int i = 0; i < 3; i++) {
            String response = mockMvc.perform(post("/api/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isServiceUnavailable()).andReturn().getResponse().getContentAsString();
            if (accepted == null) accepted = response;
            else assertEquals(accepted, response);
        }
        mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().exists("Retry-After"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString(email))));
    }

    @Test
    void restablecimientoTieneLimitePorIp() throws Exception {
        String body = "{\"token\":\"fake-reset-token\",\"newPassword\":\"long-new-password\"}";
        for (int i = 0; i < 12; i++) {
            mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest());
        }
        mockMvc.perform(post("/api/auth/reset-password").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isTooManyRequests())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().exists("Retry-After"));
    }

    @Test
    void recuperacionInformaServicioCorreoNoDisponibleSinConfigurar() throws Exception {
        String registered = "mail-disabled-" + java.util.UUID.randomUUID() + "@example.org";
        usuarios.guardar(Usuario.registrar("Mail test", registered,
                passwordHasher.hash("sample-only-password"), Instant.now()));
        var unknown = mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.org\"}"))
                .andExpect(status().isServiceUnavailable()).andReturn().getResponse().getContentAsString();
        var known = mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + registered + "\"}"))
                .andExpect(status().isServiceUnavailable()).andReturn().getResponse().getContentAsString();
        assertEquals(unknown, known);
        org.junit.jupiter.api.Assertions.assertFalse(known.contains(registered));
    }

    @Test
    void usuarioNoPuedeCrearAsignaturas() throws Exception {
        mockMvc.perform(post("/api/asignaturas").with(user("student").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void userNoPuedeCambiarRole() throws Exception {
        Usuario saved = usuarios.guardar(Usuario.registrar("Protected", "protected@example.org",
                passwordHasher.hash("correct-password"), Instant.now()));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/usuarios/{id}/access", saved.getId()).with(user("student").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"accountStatus\":\"ACTIVO\"}"))
                .andExpect(status().isForbidden());
        assertEquals(RolUsuario.USER, usuarios.buscarPorId(saved.getId()).orElseThrow().getRole());
    }

    @Test
    void adminPuedeCambiarRoleYEstado() throws Exception {
        Usuario saved = usuarios.guardar(Usuario.registrar("Managed", "managed@example.org",
                passwordHasher.hash("correct-password"), Instant.now()));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/usuarios/{id}/access", saved.getId()).with(user("admin").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"ADMIN\",\"accountStatus\":\"INACTIVO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.accountStatus").value("INACTIVO"));
        Usuario updated = usuarios.buscarPorId(saved.getId()).orElseThrow();
        assertEquals(RolUsuario.ADMIN, updated.getRole());
        assertEquals(EstadoCuenta.INACTIVO, updated.getAccountStatus());
    }

    @Test
    void reportesAsignaturasPermitenLecturaAUserYAdmin() throws Exception {
        mockMvc.perform(get("/api/reportes/asignaturas/carrera-semestre-area")
                        .with(user("student").roles("USER")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/reportes/asignaturas/creditos-profesor-nombre")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void reportesUsuariosExigenAdminYNoExponenCamposSecretos() throws Exception {
        Instant created = Instant.now();
        usuarios.guardar(new Usuario(null, "Private report user", "report-private@example.org", "{bcrypt}private-hash",
                RolUsuario.ADMIN, EstadoCuenta.ACTIVO, created, "private-reset-hash", created.plusSeconds(60)));

        mockMvc.perform(get("/api/reportes/usuarios/rol-estado?role=ADMIN&accountStatus=ACTIVO")
                        .with(user("student").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/reportes/usuarios/rol-estado?role=ADMIN&accountStatus=ACTIVO")
                        .with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].email").value("report-private@example.org"))
                .andExpect(jsonPath("$.items[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$.items[0].resetTokenHash").doesNotExist())
                .andExpect(jsonPath("$.items[0].resetTokenExpiry").doesNotExist());
    }

    @Test
    void openApiEsPublicoDescribeRutasYDeclaraBearerJwt() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.servers[0].url").value("/"))
                .andExpect(jsonPath("$.paths['/api/asignaturas']").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/reportes/usuarios/rol-estado']").exists())
                .andExpect(jsonPath("$.paths['/api/asignaturas'].get.responses['200'].content['application/json'].schema").exists())
                .andExpect(jsonPath("$.paths['/api/auth/login'].post.responses['200'].content['application/json'].schema").exists())
                .andExpect(jsonPath("$.paths['/api/usuarios'].get.security[0].bearerAuth").exists())
                .andExpect(jsonPath("$.paths['/api/usuarios'].post.security").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.UsuarioResponse.properties.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.UsuarioResponse.properties.resetTokenHash").doesNotExist())
                .andExpect(jsonPath("$.components.schemas.CrearUsuarioRequest.properties.role").doesNotExist());
    }

    @Test
    void swaggerUiEsPublica() throws Exception {
        mockMvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void corsNoPermiteUnOrigenArbitrarioPorDefecto() throws Exception {
        mockMvc.perform(get("/api/asignaturas").header("Origin", "https://untrusted.example")
                        .with(user("student").roles("USER")))
                .andExpect(status().isForbidden())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .header().doesNotExist("Access-Control-Allow-Origin"));
    }
}
