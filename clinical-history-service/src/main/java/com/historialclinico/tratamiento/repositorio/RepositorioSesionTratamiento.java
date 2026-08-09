package com.historialclinico.tratamiento.repositorio;

import com.historialclinico.tratamiento.modelo.SesionTratamiento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface RepositorioSesionTratamiento extends JpaRepository<SesionTratamiento, Long> {
    boolean existsByIdAndTratamientoIdAndTratamientoPacienteIdAndTratamientoPacienteIdProfesional(
            Long id, Long idTratamiento, Long idPaciente, Long idProfesional);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from SesionTratamiento s where s.id = :id and s.tratamiento.id = :idTratamiento "
            + "and s.tratamiento.paciente.id = :idPaciente and s.tratamiento.paciente.idProfesional = :idProfesional")
    Optional<SesionTratamiento> buscarParaRectificar(@Param("id") Long id, @Param("idTratamiento") Long idTratamiento,
            @Param("idPaciente") Long idPaciente, @Param("idProfesional") Long idProfesional);
}
