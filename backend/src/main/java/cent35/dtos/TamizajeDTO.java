package cent35.dtos;

import cent35.models.Tamizaje;
import java.time.LocalDateTime;

public class TamizajeDTO {

    private long id;
    private boolean consent;
    private LocalDateTime timestamp;
    private String id_registro;
    private String supervisorNombre;

    public TamizajeDTO(Tamizaje tamizaje) {
        this.id = tamizaje.getId();
        this.consent = tamizaje.isConsent();
        this.timestamp = tamizaje.getTimestamp();
        this.id_registro = tamizaje.getId_registro();
        this.supervisorNombre = tamizaje.getSupervisorNombre();
    }

    public long getId() { return id; }
    public boolean isConsent() { return consent; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getId_registro() { return id_registro; }
    public String getSupervisorNombre() { return supervisorNombre; }
}
