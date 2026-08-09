package com.historialclinico.fichamedica.controlador;

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
class ControladorFichaMedicaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void realizaCrudCompletoDeFichaMedicaAnidada() throws Exception {
        String cuerpoCreado = mockMvc.perform(post("/api/v1/profesionales/25/fichas-medicas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(crearRequest()))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.idProfesional").value(25))
                .andExpect(jsonPath("$.detalles[0].campos[0].permiteSeleccionMultiple").value(false))
                .andExpect(jsonPath("$.detalles[0].campos[0].opciones", hasSize(3)))
                .andReturn().getResponse().getContentAsString();

        Number valorIdFicha = com.jayway.jsonpath.JsonPath.read(cuerpoCreado, "$.id");
        long idFicha = valorIdFicha.longValue();

        mockMvc.perform(get("/api/v1/profesionales/25/fichas-medicas/{idFicha}", idFicha))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Historia clínica general"));

        mockMvc.perform(get("/api/v1/profesionales/25/fichas-medicas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(put("/api/v1/profesionales/25/fichas-medicas/{idFicha}", idFicha)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(actualizarRequest()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ficha actualizada"))
                .andExpect(jsonPath("$.detalles[0].titulo").value("Hábitos"));

        mockMvc.perform(delete("/api/v1/profesionales/25/fichas-medicas/{idFicha}", idFicha))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/profesionales/25/fichas-medicas/{idFicha}", idFicha))
                .andExpect(status().isNotFound());
    }

    @Test
    void rechazaCampoSinOpciones() throws Exception {
        mockMvc.perform(post("/api/v1/profesionales/25/fichas-medicas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ficha inválida",
                                  "detalles": [{
                                    "titulo": "Sección",
                                    "orden": 0,
                                    "campos": [{"titulo": "Campo", "orden": 0, "opciones": []}]
                                  }]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.violaciones[0].campo").exists());
    }

    @Test
    void permiteOmitirTituloEnUnaUnicaOpcionDeEntrada() throws Exception {
        mockMvc.perform(post("/api/v1/profesionales/26/fichas-medicas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ficha de texto libre",
                                  "detalles": [{
                                    "titulo": "Observaciones",
                                    "orden": 0,
                                    "campos": [{
                                      "titulo": "Descripción general",
                                      "orden": 0,
                                      "opciones": [{"tipo": "ENTRADA", "orden": 0}]
                                    }]
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detalles[0].campos[0].opciones[0].titulo").doesNotExist());
    }

    @Test
    void eliminaPlantillaUtilizadaSinPerderLaInstanciaClinica() throws Exception {
        String plantilla = mockMvc.perform(post("/api/v1/profesionales/28/fichas-medicas")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Control descartable","descripcion":"Plantilla original","detalles":[{
                          "titulo":"Evaluación","orden":0,"campos":[{"titulo":"Dolor","orden":0,
                          "opciones":[{"tipo":"ENTRADA","orden":0}]}]}]}
                        """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long idFicha = ((Number) com.jayway.jsonpath.JsonPath.read(plantilla, "$.id")).longValue();
        long idOpcion = ((Number) com.jayway.jsonpath.JsonPath.read(
                plantilla, "$.detalles[0].campos[0].opciones[0].id")).longValue();

        String paciente = mockMvc.perform(post("/api/v1/profesionales/28/pacientes")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Ana","apellido":"Prueba","dni":"38111228","fechaNacimiento":"1990-01-01",
                         "sexo":"FEMENINO","fichas":[{"idFichaMedica":%d,
                         "respuestas":[{"idOpcion":%d,"valor":"Dolor leve"}]}]}
                        """.formatted(idFicha, idOpcion)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long idPaciente = ((Number) com.jayway.jsonpath.JsonPath.read(paciente, "$.id")).longValue();

        mockMvc.perform(delete("/api/v1/profesionales/28/fichas-medicas/{idFicha}", idFicha))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/profesionales/28/pacientes/{idPaciente}", idPaciente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fichas[0].nombreFicha").value("Control descartable"))
                .andExpect(jsonPath("$.fichas[0].respuestas[0].tituloDetalle").value("Evaluación"))
                .andExpect(jsonPath("$.fichas[0].respuestas[0].tituloCampo").value("Dolor"))
                .andExpect(jsonPath("$.fichas[0].respuestas[0].valor").value("Dolor leve"));
    }

    @Test
    void creaCampoSiNoConOpcionesAdicionales() throws Exception {
        mockMvc.perform(post("/api/v1/profesionales/27/fichas-medicas")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nombre": "Ficha binaria",
                                  "detalles": [{
                                    "titulo": "Antecedentes",
                                    "orden": 0,
                                    "campos": [{
                                      "titulo": "¿Tiene alergias?",
                                      "orden": 0,
                                      "permiteSeleccionMultiple": true,
                                      "opciones": [
                                        {"tipo": "SI_NO", "orden": 0},
                                        {"titulo": "¿Cuál?", "tipo": "ENTRADA", "orden": 1},
                                        {"titulo": "Confirmado", "tipo": "SELECCION", "orden": 2}
                                      ]
                                    }]
                                  }]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.detalles[0].campos[0].opciones", hasSize(3)))
                .andExpect(jsonPath("$.detalles[0].campos[0].opciones[0].tipo").value("SI_NO"))
                .andExpect(jsonPath("$.detalles[0].campos[0].opciones[1].tipo").value("ENTRADA"))
                .andExpect(jsonPath("$.detalles[0].campos[0].opciones[2].tipo").value("SELECCION"));
    }

    private String crearRequest() {
        return """
                {
                  "nombre": "Historia clínica general",
                  "descripcion": "Plantilla inicial",
                  "detalles": [{
                    "titulo": "Antecedentes personales no patológicos",
                    "orden": 0,
                    "campos": [{
                      "titulo": "Tabaquismo",
                      "orden": 0,
                      "permiteSeleccionMultiple": false,
                      "opciones": [
                        {"titulo": "Sí", "tipo": "SELECCION", "orden": 0, "grupoExclusion": "smoking"},
                        {"titulo": "No", "tipo": "SELECCION", "orden": 1, "grupoExclusion": "smoking"},
                        {"titulo": "¿Cuántos por día?", "tipo": "ENTRADA", "orden": 2}
                      ]
                    }]
                  }]
                }
                """;
    }

    private String actualizarRequest() {
        return """
                {
                  "nombre": "Ficha actualizada",
                  "detalles": [{
                    "titulo": "Hábitos",
                    "orden": 0,
                    "campos": [{
                      "titulo": "Actividad física",
                      "orden": 0,
                      "permiteSeleccionMultiple": true,
                      "opciones": [{"titulo": "Descripción", "tipo": "ENTRADA", "orden": 0}]
                    }]
                  }]
                }
                """;
    }
}
