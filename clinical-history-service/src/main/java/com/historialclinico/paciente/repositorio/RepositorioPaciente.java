package com.historialclinico.paciente.repositorio;

import com.historialclinico.paciente.modelo.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RepositorioPaciente extends JpaRepository<Paciente, Long> {

    List<Paciente> findAllByIdProfesionalOrderByApellidoAscNombreAsc(Long idProfesional);

    Optional<Paciente> findByIdAndIdProfesional(Long id, Long idProfesional);

    boolean existsByIdProfesionalAndDni(Long idProfesional, String dni);

    boolean existsByIdProfesionalAndDniAndIdNot(Long idProfesional, String dni, Long id);
}
