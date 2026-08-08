package com.historialclinico.paciente.controlador;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ControladorPacienteIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void actualizaAislaYPreservaElPacienteSinBorradoFisico() throws Exception {
        String respuesta = mockMvc.perform(post("/api/v1/profesionales/10/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud("Ana", "Pérez", "30111222")))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.idProfesional").value(10))
                .andExpect(jsonPath("$.dni").value("30111222"))
                .andReturn().getResponse().getContentAsString();

        long idPaciente = ((Number) JsonPath.read(respuesta, "$.id")).longValue();

        mockMvc.perform(get("/api/v1/profesionales/10/pacientes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/profesionales/11/pacientes/{idPaciente}", idPaciente))
                .andExpect(status().isNotFound());

        mockMvc.perform(put("/api/v1/profesionales/10/pacientes/{idPaciente}", idPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud("Ana María", "Pérez", "30111222")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana María"));

        mockMvc.perform(delete("/api/v1/profesionales/10/pacientes/{idPaciente}", idPaciente))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/profesionales/10/pacientes/{idPaciente}", idPaciente))
                .andExpect(status().isOk());
    }

    @Test
    void rechazaDniDuplicadoParaElMismoProfesional() throws Exception {
        mockMvc.perform(post("/api/v1/profesionales/20/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud("Juan", "López", "28123456")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/profesionales/20/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud("Pedro", "Gómez", "28123456")))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/profesionales/21/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(solicitud("Pedro", "Gómez", "28123456")))
                .andExpect(status().isCreated());
    }

    @Test
    void validaDatosObligatoriosYFechaPasada() throws Exception {
        mockMvc.perform(post("/api/v1/profesionales/30/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"", "apellido":"", "dni":"ABC", "fechaNacimiento":"2999-01-01"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violaciones").isArray());
    }

    @Test
    void permiteTelefonoVacioYRechazaLetrasEnTelefono() throws Exception {
        mockMvc.perform(post("/api/v1/profesionales/31/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "María",
                                  "apellido": "Suárez",
                                  "dni": "33123456",
                                  "telefono": "",
                                  "fechaNacimiento": "1990-05-20",
                                  "sexo": "FEMENINO"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.telefono").doesNotExist());

        mockMvc.perform(post("/api/v1/profesionales/31/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Laura",
                                  "apellido": "Suárez",
                                  "dni": "34123456",
                                  "telefono": "11ABC123",
                                  "fechaNacimiento": "1990-05-20",
                                  "sexo": "FEMENINO"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void registraPacienteConVariasFichasYRespuestasEnUnaTransaccion() throws Exception {
        FichaCreada primera = crearFicha(40, "Admisión", "Motivo de consulta");
        FichaCreada segunda = crearFicha(40, "Antecedentes", "Alergias");

        mockMvc.perform(post("/api/v1/profesionales/40/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Lucía",
                                  "apellido": "Martínez",
                                  "dni": "32123456",
                                  "fechaNacimiento": "1992-07-14",
                                  "sexo": "FEMENINO",
                                  "fichas": [
                                    {"idFichaMedica": %d, "respuestas": [
                                      {"idOpcion": %d, "valor": "Control inicial"}
                                    ]},
                                    {"idFichaMedica": %d, "respuestas": [
                                      {"idOpcion": %d, "valor": ""}
                                    ]}
                                  ]
                                }
                                """.formatted(primera.idFicha(), primera.idOpcion(), segunda.idFicha(), segunda.idOpcion())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fichas", hasSize(2)))
                .andExpect(jsonPath("$.fichas[0].respuestas[0].tituloDetalle").value("Datos"))
                .andExpect(jsonPath("$.fichas[0].respuestas[0].tituloCampo").value("Motivo de consulta"))
                .andExpect(jsonPath("$.fichas[0].respuestas[0].valor").value("Control inicial"))
                .andExpect(jsonPath("$.fichas[1].respuestas[0].valor").value("No aplica"));
    }

    @Test
    void actualizaDatosPersonalesYRespuestasDeLasFichas() throws Exception {
        FichaCreada ficha = crearFicha(41, "Control", "Evolución");
        String creado = mockMvc.perform(post("/api/v1/profesionales/41/pacientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sofía","apellido":"Díaz","dni":"32111999",
                                 "fechaNacimiento":"1992-07-14","sexo":"FEMENINO","fichas":[
                                   {"idFichaMedica":%d,"respuestas":[{"idOpcion":%d,"valor":"Inicial"}]}
                                 ]}
                                """.formatted(ficha.idFicha(), ficha.idOpcion())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long idPaciente = ((Number) JsonPath.read(creado, "$.id")).longValue();
        long idFichaPaciente = ((Number) JsonPath.read(creado, "$.fichas[0].id")).longValue();

        mockMvc.perform(put("/api/v1/profesionales/41/pacientes/{idPaciente}", idPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sofía Elena","apellido":"Díaz","dni":"32111999",
                                 "fechaNacimiento":"1992-07-14","sexo":"FEMENINO","fichas":[
                                   {"idFichaPaciente":%d,"idFichaMedica":%d,
                                    "respuestas":[{"idOpcion":%d,"valor":"Evolución favorable"}]}
                                 ]}
                                """.formatted(idFichaPaciente, ficha.idFicha(), ficha.idOpcion())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Sofía Elena"))
                .andExpect(jsonPath("$.fichas[0].respuestas[0].valor").value("Evolución favorable"));

        FichaCreada nuevaFicha = crearFicha(41, "Antecedentes", "Observación");
        mockMvc.perform(put("/api/v1/profesionales/41/pacientes/{idPaciente}", idPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Sofía Elena","apellido":"Díaz","dni":"32111999",
                                 "fechaNacimiento":"1992-07-14","sexo":"FEMENINO","fichas":[
                                   {"idFichaMedica":%d,
                                    "respuestas":[{"idOpcion":%d,"valor":"Nueva ficha"}]}
                                 ]}
                                """.formatted(nuevaFicha.idFicha(), nuevaFicha.idOpcion())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fichas", hasSize(1)))
                .andExpect(jsonPath("$.fichas[0].idFichaMedica").value(nuevaFicha.idFicha()))
                .andExpect(jsonPath("$.fichas[0].respuestas[0].valor").value("Nueva ficha"));
    }

    private FichaCreada crearFicha(long idProfesional, String nombre, String tituloOpcion) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/profesionales/{idProfesional}/fichas-medicas", idProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "%s",
                                  "detalles": [{
                                    "titulo": "Datos",
                                    "orden": 0,
                                    "campos": [{
                                      "titulo": "%s",
                                      "orden": 0,
                                      "opciones": [{"tipo": "ENTRADA", "orden": 0}]
                                    }]
                                  }]
                                }
                                """.formatted(nombre, tituloOpcion)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new FichaCreada(((Number) JsonPath.read(cuerpo, "$.id")).longValue(),
                ((Number) JsonPath.read(cuerpo, "$.detalles[0].campos[0].opciones[0].id")).longValue());
    }

    private record FichaCreada(long idFicha, long idOpcion) {}

    private String solicitud(String nombre, String apellido, String dni) {
        return """
                {
                  "nombre": "%s",
                  "apellido": "%s",
                  "dni": "%s",
                  "telefono": "541155551234",
                  "fechaNacimiento": "1990-05-20",
                  "sexo": "FEMENINO"
                }
                """.formatted(nombre, apellido, dni);
    }
}
