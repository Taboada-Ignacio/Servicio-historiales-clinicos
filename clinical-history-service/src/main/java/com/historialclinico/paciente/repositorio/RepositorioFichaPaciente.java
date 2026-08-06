package com.historialclinico.paciente.repositorio;

import com.historialclinico.paciente.modelo.FichaPaciente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RepositorioFichaPaciente extends JpaRepository<FichaPaciente, Long> {
}
