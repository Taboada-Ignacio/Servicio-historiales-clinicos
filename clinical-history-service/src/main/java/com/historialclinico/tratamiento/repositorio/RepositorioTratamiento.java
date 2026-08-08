package com.historialclinico.tratamiento.repositorio;
import com.historialclinico.tratamiento.modelo.Tratamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
public interface RepositorioTratamiento extends JpaRepository<Tratamiento, Long> {
    List<Tratamiento> findAllByPacienteIdAndPacienteIdProfesionalOrderByFechaCreacionDesc(Long idPaciente, Long idProfesional);
    List<Tratamiento> findAllByPacienteIdAndPacienteIdProfesionalAndCantidadSesionesFaltantesGreaterThanOrderByFechaCreacionDesc(
            Long idPaciente, Long idProfesional, int minimo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tratamiento t where t.id = :id and t.paciente.id = :idPaciente and t.paciente.idProfesional = :idProfesional")
    Optional<Tratamiento> buscarParaContinuar(@Param("id") Long id, @Param("idPaciente") Long idPaciente,
            @Param("idProfesional") Long idProfesional);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from Tratamiento t where t.id = :id and t.paciente.id = :idPaciente and t.paciente.idProfesional = :idProfesional")
    Optional<Tratamiento> buscarParaRectificar(@Param("id") Long id, @Param("idPaciente") Long idPaciente,
            @Param("idProfesional") Long idProfesional);
}
