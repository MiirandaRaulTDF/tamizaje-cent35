package cent35.models.enums;

public enum AntecedentesFamiliares {
    NO("Sin antecedentes"),
    SEGUNDO_GRADO("Abuelos, tíos o primos"),
    PRIMER_GRADO("Padres, hermanos o hijos");

    private final String descripcion;

    AntecedentesFamiliares(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}