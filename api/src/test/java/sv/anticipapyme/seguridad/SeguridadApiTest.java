package sv.anticipapyme.seguridad;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:${DB_PUERTO_HOST:5432}/anticipapyme_test",
        "spring.jpa.hibernate.ddl-auto=create",
        "jwt.clave=clave-de-prueba-anticipapyme-para-tests-0123456789"
})
@AutoConfigureMockMvc
class SeguridadApiTest {

    private static final String ANTICIPO = """
            { "empresaCedenteId": 1, "facturaIds": [5, 6] }""";

    private static final long FACTURA_AJENA = 7L;

    @Autowired
    private MockMvc mvc;

    private String tokenDe(String usuario, String clave) throws Exception {
        String json = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"clave\":\"%s\"}".formatted(usuario, clave)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return json.split("\"token\":\"")[1].split("\"")[0];
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }

    @Test
    void clienteNoVeFacturaAjenaRecibe404() throws Exception {
        String cliente = tokenDe("panaderia", "Cliente2026");

        mvc.perform(get("/api/facturas/" + FACTURA_AJENA).header("Authorization", bearer(cliente)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"));
    }

    @Test
    void asesorSiVeEsaFactura() throws Exception {
        String asesor = tokenDe("asesor1", "Asesor2026");

        mvc.perform(get("/api/facturas/" + FACTURA_AJENA).header("Authorization", bearer(asesor)))
                .andExpect(status().isOk());
    }

    @Test
    void sinTokenDa401ConProblemDetail() throws Exception {
        mvc.perform(get("/api/facturas"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void tokenFalsoDa401() throws Exception {
        mvc.perform(get("/api/facturas").header("Authorization", bearer("esto.no.sirve")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("No autenticado"));
    }

    @Test
    void loginConClaveMalaDa401() throws Exception {
        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"asesor1\",\"clave\":\"incorrecta\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Credenciales invalidas"));
    }

    @Test
    void clienteLeePeroNoAnticipa() throws Exception {
        String cliente = tokenDe("panaderia", "Cliente2026");

        mvc.perform(get("/api/facturas").header("Authorization", bearer(cliente)))
                .andExpect(status().isOk());

        mvc.perform(post("/api/anticipos").header("Authorization", bearer(cliente))
                        .contentType(MediaType.APPLICATION_JSON).content(ANTICIPO))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Sin permiso"));
    }

    @Test
    void asesorAnticipaYLosDatosMalosDan400() throws Exception {
        String asesor = tokenDe("asesor1", "Asesor2026");

        mvc.perform(post("/api/anticipos").header("Authorization", bearer(asesor))
                        .contentType(MediaType.APPLICATION_JSON).content(ANTICIPO))
                .andExpect(status().isCreated());

        mvc.perform(post("/api/anticipos").header("Authorization", bearer(asesor))
                        .contentType(MediaType.APPLICATION_JSON).content("{ \"empresaCedenteId\": 1, \"facturaIds\": [] }"))
                .andExpect(status().isBadRequest());
    }
}