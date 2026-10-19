package cent35.repositories;

import cent35.models.ResultadosTamizaje;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface ResultadosTamizajeRepository extends JpaRepository<ResultadosTamizaje, Long> {
    java.util.Optional<ResultadosTamizaje> findByTamizajeId(Long tamizajeId);
    @EntityGraph(attributePaths = {
            "tamizaje",
            "tamizaje.paciente",
            "tamizaje.antropometria",
            "tamizaje.tomaPresiones"
    })
    java.util.Optional<ResultadosTamizaje> findByTokenConsulta(String tokenConsulta);
}
