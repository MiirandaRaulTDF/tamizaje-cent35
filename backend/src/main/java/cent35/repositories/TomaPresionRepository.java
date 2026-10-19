package cent35.repositories;

import cent35.models.TomaPresion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;
import java.util.List;

@RepositoryRestResource
public interface TomaPresionRepository extends JpaRepository<TomaPresion, Long> {

    List<TomaPresion> findByTamizajeIdOrderByNumeroTomaAsc(Long idTamizaje);

    long countByTamizajeId(Long idTamizaje);
}