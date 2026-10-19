package cent35.repositories;

import cent35.models.Tamizaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@RepositoryRestResource
public interface TamizajeRepository extends JpaRepository<Tamizaje, Long> {
    @Query("select t from Tamizaje t where t.paciente.id_paciente = :id")
    java.util.List<Tamizaje> buscarPorPaciente(@Param("id") Integer pacienteId);

    @Query("select t from Tamizaje t where t.operador.id = :id or t.supervisor.id = :id")
    java.util.List<Tamizaje> buscarPorEnfermero(@Param("id") Integer enfermeroId);

    @Query("select case when count(t) > 0 then true else false end from Tamizaje t where t.paciente.id_paciente = :id")
    boolean existePorPaciente(@Param("id") Integer pacienteId);

    @Query("select case when count(t) > 0 then true else false end from Tamizaje t where t.operador.id = :id or t.supervisor.id = :id")
    boolean existePorEnfermero(@Param("id") Integer enfermeroId);

    @Query("select case when count(t) > 0 then true else false end " +
           "from Tamizaje t where t.id_registro = :idRegistro")
    boolean existsByIdRegistro(@Param("idRegistro") String idRegistro);
}
