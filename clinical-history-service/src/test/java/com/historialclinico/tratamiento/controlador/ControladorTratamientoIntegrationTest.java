package com.historialclinico.tratamiento.controlador;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import com.historialclinico.paciente.repositorio.RepositorioPaciente;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @Transactional
class ControladorTratamientoIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired RepositorioPaciente repositorioPacientes;

    @Test
    void rectificaTratamientoYSesionConAuditoriaIndependiente() throws Exception {
        long paciente = crearPaciente();
        String creado = mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos", paciente)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                        {"nombre":"Plan original","descripcion":"Descripción original","cantidadSesionesTotal":3,
                         "primeraSesion":{"observaciones":"Observación original"}}
                        """))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long tratamiento = ((Number) JsonPath.read(creado, "$.id")).longValue();
        long sesion = ((Number) JsonPath.read(creado, "$.sesiones[0].id")).longValue();

        mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos/{tratamiento}/rectificaciones",
                        paciente, tratamiento).contentType(MediaType.APPLICATION_JSON).content("""
                        {"rectificacion":{"versionEsperada":1,"tipoMotivo":"ACLARACION",
                         "motivo":"Se amplía la planificación clínica del tratamiento"},
                         "nombre":"Plan rectificado","descripcion":"Descripción rectificada","cantidadSesionesTotal":4}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versionClinica").value(2))
                .andExpect(jsonPath("$.nombre").value("Plan rectificado"))
                .andExpect(jsonPath("$.cantidadSesionesFaltantes").value(3));

        mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos/{tratamiento}/sesiones/{sesion}/rectificaciones",
                        paciente, tratamiento, sesion).contentType(MediaType.APPLICATION_JSON).content("""
                        {"rectificacion":{"versionEsperada":1,"tipoMotivo":"DATO_CLINICO_INCORRECTO",
                         "motivo":"Se rectifica la observación clínica registrada"},"observaciones":"Observación rectificada"}
                        """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.versionClinica").value(2))
                .andExpect(jsonPath("$.observaciones").value("Observación rectificada"));

        mockMvc.perform(get("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos/{tratamiento}/auditoria",
                        paciente, tratamiento)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].antes.nombre").value("Plan original"))
                .andExpect(jsonPath("$[0].despues.nombre").value("Plan rectificado"));
        mockMvc.perform(get("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos/{tratamiento}/sesiones/{sesion}/auditoria",
                        paciente, tratamiento, sesion)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].antes.observaciones").value("Observación original"))
                .andExpect(jsonPath("$[0].integridadValida").value(true));
    }

    @Test
    void creaTratamientoConPrimeraSesionYFicha() throws Exception {
        long paciente = crearPaciente();
        long[] ficha = crearFicha();
        mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos", paciente)
                .contentType(MediaType.APPLICATION_JSON).content("""
                    {"nombre":"Rehabilitación de rodilla","descripcion":"Plan progresivo","cantidadSesionesTotal":10,
                     "primeraSesion":{"observaciones":"Evaluación inicial","idFichaSeguimiento":%d,
                     "respuestasFichaSeguimiento":[{"idOpcion":%d,"valor":"Dolor leve"}]}}
                    """.formatted(ficha[0], ficha[1])))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.cantidadSesionesTotal").value(10))
                .andExpect(jsonPath("$.cantidadSesionesFaltantes").value(9))
                .andExpect(jsonPath("$.sesiones", hasSize(1)))
                .andExpect(jsonPath("$.sesiones[0].nroSesion").value(1))
                .andExpect(jsonPath("$.sesiones[0].idFichaSeguimiento").value(ficha[0]));
        mockMvc.perform(get("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos", paciente))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void creaTratamientoSinSesionYValidaDatos() throws Exception {
        long paciente = crearPaciente();
        mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos", paciente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Kinesiología\",\"cantidadSesionesTotal\":5}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.cantidadSesionesFaltantes").value(5))
                .andExpect(jsonPath("$.sesiones", hasSize(0)));
        mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos", paciente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\" \",\"cantidadSesionesTotal\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listaSoloPendientesYRegistraLaSiguienteSesion() throws Exception {
        long paciente = crearPaciente();
        String creado = mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos", paciente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Terapia manual\",\"cantidadSesionesTotal\":1}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long idTratamiento = ((Number) JsonPath.read(creado, "$.id")).longValue();

        mockMvc.perform(get("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos/sin-terminar", paciente))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].nombre").value("Terapia manual"));

        mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos/{tratamiento}/sesiones",
                        paciente, idTratamiento).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observaciones\":\"Buena tolerancia a la sesión\"}"))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.cantidadSesionesFaltantes").value(0))
                .andExpect(jsonPath("$.sesiones", hasSize(1)))
                .andExpect(jsonPath("$.sesiones[0].nroSesion").value(1));

        mockMvc.perform(get("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos/sin-terminar", paciente))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos/{tratamiento}/sesiones",
                        paciente, idTratamiento).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observaciones\":\"No debe registrarse\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void registraFichaAlContinuarUnTratamientoPersistido() throws Exception {
        long paciente = crearPaciente();
        long[] ficha = crearFicha();
        String creado = mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos", paciente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Reeducación postural\",\"cantidadSesionesTotal\":3}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long idTratamiento = ((Number) JsonPath.read(creado, "$.id")).longValue();

        mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos/{tratamiento}/sesiones",
                        paciente, idTratamiento).contentType(MediaType.APPLICATION_JSON).content("""
                    {"observaciones":"Se trabajó movilidad y postura","idFichaSeguimiento":%d,
                     "respuestasFichaSeguimiento":[{"idOpcion":%d,"valor":"Evolución favorable"}]}
                    """.formatted(ficha[0], ficha[1])))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sesiones[0].idFichaSeguimiento").value(ficha[0]))
                .andExpect(jsonPath("$.cantidadSesionesFaltantes").value(2));

        // Reproduce el flush que ocurre al cerrar una transacción HTTP real.
        repositorioPacientes.flush();
    }

    @Test
    void registraSesionSinObservacionesConTextoPredeterminado() throws Exception {
        long paciente = crearPaciente();
        String creado = mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos", paciente)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Movilidad\",\"cantidadSesionesTotal\":2}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long idTratamiento = ((Number) JsonPath.read(creado, "$.id")).longValue();

        mockMvc.perform(post("/api/v1/profesionales/90/pacientes/{paciente}/tratamientos/{tratamiento}/sesiones",
                        paciente, idTratamiento).contentType(MediaType.APPLICATION_JSON)
                .content("{\"observaciones\":\"   \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sesiones[0].observaciones").value("Sin observaciones"));
    }

    private long crearPaciente() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/profesionales/90/pacientes").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Lucía\",\"apellido\":\"Méndez\",\"dni\":\"40111222\",\"fechaNacimiento\":\"1995-02-03\",\"sexo\":\"FEMENINO\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(cuerpo, "$.id")).longValue();
    }
    private long[] crearFicha() throws Exception {
        String cuerpo = mockMvc.perform(post("/api/v1/profesionales/90/fichas-medicas").contentType(MediaType.APPLICATION_JSON)
                .content("{\"nombre\":\"Control de sesión\",\"detalles\":[{\"titulo\":\"Estado\",\"orden\":0,\"campos\":[{\"titulo\":\"Dolor\",\"orden\":0,\"opciones\":[{\"tipo\":\"ENTRADA\",\"orden\":0}]}]}]}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return new long[]{((Number)JsonPath.read(cuerpo,"$.id")).longValue(), ((Number)JsonPath.read(cuerpo,"$.detalles[0].campos[0].opciones[0].id")).longValue()};
    }
}
