package com.historialclinico.fichamedica.repositorio;

import com.historialclinico.fichamedica.modelo.FichaMedica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RepositorioFichaMedica extends JpaRepository<FichaMedica, Long> {

    @Query("select count(f) from FichaMedica f where f.idProfesional = :idProfesional")
    long contarPorProfesional(@Param("idProfesional") Long idProfesional);

    @Query("select f from FichaMedica f where f.idProfesional = :idProfesional order by f.fechaCreacion asc")
    List<FichaMedica> buscarTodasPorProfesional(@Param("idProfesional") Long idProfesional);

    @Query("select f from FichaMedica f where f.id = :id and f.idProfesional = :idProfesional")
    Optional<FichaMedica> buscarPorIdYProfesional(@Param("id") Long id, @Param("idProfesional") Long idProfesional);
}
