package cent35.repositories;

import cent35.models.Enfermero;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface EnfermeroRepository extends JpaRepository<Enfermero, Integer> {

}