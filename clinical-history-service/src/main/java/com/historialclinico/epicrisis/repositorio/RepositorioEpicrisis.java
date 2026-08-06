package com.historialclinico.epicrisis.repositorio;

import com.historialclinico.epicrisis.modelo.Epicrisis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RepositorioEpicrisis extends JpaRepository<Epicrisis, Long> {
    List<Epicrisis> findAllByPacienteIdAndPacienteIdProfesionalOrderByFechaHoraDesc(
            Long idPaciente, Long idProfesional
    );
}
