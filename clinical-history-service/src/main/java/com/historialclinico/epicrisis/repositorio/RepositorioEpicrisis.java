package com.historialclinico.epicrisis.repositorio;

import com.historialclinico.epicrisis.modelo.Epicrisis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;

public interface RepositorioEpicrisis extends JpaRepository<Epicrisis, Long> {
    List<Epicrisis> findAllByPacienteIdAndPacienteIdProfesionalOrderByFechaHoraDesc(
            Long idPaciente, Long idProfesional
    );
    boolean existsByIdAndPacienteIdAndPacienteIdProfesional(Long id, Long idPaciente, Long idProfesional);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Epicrisis e where e.id = :id and e.paciente.id = :idPaciente and e.paciente.idProfesional = :idProfesional")
    Optional<Epicrisis> buscarParaRectificar(@Param("id") Long id, @Param("idPaciente") Long idPaciente,
            @Param("idProfesional") Long idProfesional);
}
