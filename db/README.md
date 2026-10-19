# Base de datos

Usar Neon como PostgreSQL administrado. La estructura se define con entidades JPA en `../backend/src/main/java/cent35/models/`.

No hay un script SQL que debas cargar manualmente: Hibernate está configurado con `spring.jpa.hibernate.ddl-auto=update`, por lo que genera las tablas faltantes cuando arranca el backend. En Railway, definir:

- `SPRING_DATASOURCE_URL=jdbc:postgresql://HOST/BASE?sslmode=require`
- `SPRING_DATASOURCE_USERNAME=USUARIO_NEON`
- `SPRING_DATASOURCE_PASSWORD=CONTRASEÑA_NEON`

No guardar secretos en el repositorio. La base demo H2 es local y sus datos no se transfieren automáticamente a Neon.
