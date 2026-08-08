package com.historialclinico.epicrisis.controlador;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ControladorEpicrisisIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rectificaSinSobrescribirLaAuditoriaYRechazaVersionDesactualizada() throws Exception {
        long paciente = crearPaciente(75, "Lucía", "Méndez", "38999111");
        String creada = mockMvc.perform(post("/api/v1/profesionales/75/pacientes/{idPaciente}/epicrisis", paciente)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"observaciones\":\"Texto original\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.versionClinica").value(1))
                .andReturn().getResponse().getContentAsString();
        long epicrisis = ((Number) JsonPath.read(creada, "$.id")).longValue();
        String rectificacion = """
                {"rectificacion":{"versionEsperada":1,"tipoMotivo":"ERROR_TRANSCRIPCION",
                "motivo":"Se corrige un error de transcripción comprobado"},"observaciones":"Texto rectificado"}
                """;
        mockMvc.perform(post("/api/v1/profesionales/75/pacientes/{paciente}/epicrisis/{epicrisis}/rectificaciones",
                        paciente, epicrisis).header("X-Professional-Name", "Profesional de prueba")
                        .header("X-Professional-License", "MP 1234").header("X-Device-Id", "equipo-prueba")
                        .contentType(MediaType.APPLICATION_JSON).content(rectificacion))
                .andExpect(status().isOk()).andExpect(jsonPath("$.observaciones").value("Texto rectificado"))
                .andExpect(jsonPath("$.versionClinica").value(2)).andExpect(jsonPath("$.estadoRegistro").value("RECTIFICADO"));
        mockMvc.perform(get("/api/v1/profesionales/75/pacientes/{paciente}/epicrisis/{epicrisis}/auditoria",
                        paciente, epicrisis)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].antes.observaciones").value("Texto original"))
                .andExpect(jsonPath("$[0].despues.observaciones").value("Texto rectificado"))
                .andExpect(jsonPath("$[0].integridadValida").value(true));
        mockMvc.perform(post("/api/v1/profesionales/75/pacientes/{paciente}/epicrisis/{epicrisis}/rectificaciones",
                        paciente, epicrisis).contentType(MediaType.APPLICATION_JSON).content(rectificacion))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/api/v1/profesionales/76/pacientes/{paciente}/epicrisis/{epicrisis}/auditoria",
                        paciente, epicrisis)).andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/profesionales/75/pacientes/{paciente}/epicrisis/{epicrisis}/informe-auditoria",
                        paciente, epicrisis)).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(jsonPath("$.integridadCadenaValida").value(true));
    }

    @Test
    void registraYListaEpicrisisDelPaciente() throws Exception {
        long idPaciente = crearPaciente(70, "Elena", "Ruiz", "35111222");
        long[] ficha = crearFicha(70);
        long idFicha = ficha[0];
        long idOpcion = ficha[1];

        mockMvc.perform(post("/api/v1/profesionales/70/pacientes/{idPaciente}/epicrisis", idPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":\"Paciente con evolución favorable.\",\"idFichaSeguimiento\":%d,\"respuestasFichaSeguimiento\":[{\"idOpcion\":%d,\"valor\":\"Sin dolor\"}]}".formatted(idFicha, idOpcion)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.idPaciente").value(idPaciente))
                .andExpect(jsonPath("$.apellidoPaciente").value("Ruiz"))
                .andExpect(jsonPath("$.idFichaSeguimiento").value(idFicha))
                .andExpect(jsonPath("$.nombreFichaSeguimiento").value("Seguimiento general"))
                .andExpect(jsonPath("$.observaciones").value("Paciente con evolución favorable."))
                .andExpect(jsonPath("$.fechaHora").exists());

        mockMvc.perform(get("/api/v1/profesionales/70/pacientes/{idPaciente}/epicrisis", idPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/profesionales/70/pacientes/{idPaciente}", idPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fichas", hasSize(0)))
                .andExpect(jsonPath("$.epicrisis").doesNotExist());
    }

    @Test
    void completaObservacionesVaciasYAislaPorProfesional() throws Exception {
        long idPaciente = crearPaciente(71, "Mario", "Sosa", "36111222");

        mockMvc.perform(post("/api/v1/profesionales/71/pacientes/{idPaciente}/epicrisis", idPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":\"   \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.observaciones").value("Sin observaciones"));

        mockMvc.perform(post("/api/v1/profesionales/72/pacientes/{idPaciente}/epicrisis", idPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":\"No debe registrarse\"}"))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/profesionales/71/pacientes/{idPaciente}/epicrisis", idPaciente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observaciones\":\"%s\"}".formatted("x".repeat(1001))))
                .andExpect(status().isBadRequest());
    }

    private long crearPaciente(long idProfesional, String nombre, String apellido, String dni) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/profesionales/{idProfesional}/pacientes", idProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"%s",
                                  "apellido":"%s",
                                  "dni":"%s",
                                  "fechaNacimiento":"1990-01-01",
                                  "sexo":"NO_ESPECIFICA"
                                }
                                """.formatted(nombre, apellido, dni)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }

    private long[] crearFicha(long idProfesional) throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/profesionales/{idProfesional}/fichas-medicas", idProfesional)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre":"Seguimiento general",
                                  "detalles":[{
                                    "titulo":"Control",
                                    "orden":0,
                                    "campos":[{
                                      "titulo":"Evolución",
                                      "orden":0,
                                      "opciones":[{"tipo":"ENTRADA","orden":0}]
                                    }]
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return new long[]{((Number) JsonPath.read(cuerpo, "$.id")).longValue(),
                ((Number) JsonPath.read(cuerpo, "$.detalles[0].campos[0].opciones[0].id")).longValue()};
    }
}
