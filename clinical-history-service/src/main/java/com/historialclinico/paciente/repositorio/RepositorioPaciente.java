package com.historialclinico.paciente.repositorio;

import com.historialclinico.paciente.modelo.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RepositorioPaciente extends JpaRepository<Paciente, Long> {

    List<Paciente> findAllByIdProfesionalOrderByApellidoAscNombreAsc(Long idProfesional);

    Optional<Paciente> findByIdAndIdProfesional(Long id, Long idProfesional);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Paciente p where p.id = :id and p.idProfesional = :idProfesional")
    Optional<Paciente> buscarPropioParaActualizar(@Param("id") Long id,
            @Param("idProfesional") Long idProfesional);

    boolean existsByIdProfesionalAndDni(Long idProfesional, String dni);

    boolean existsByIdProfesionalAndDniAndIdNot(Long idProfesional, String dni, Long id);
}
