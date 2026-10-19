package cent35.repositories;

import cent35.models.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.rest.core.annotation.RepositoryRestResource;

@RepositoryRestResource
public interface PacienteRepository extends JpaRepository<Paciente, Integer> {
}