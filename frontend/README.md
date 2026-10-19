# Frontend

La interfaz de Tamizaje CENT35 está hecha con Vue y Bootstrap, pero no es un proyecto separado con `package.json` ni requiere npm. Está servida por Spring Boot desde:

`backend/src/main/resources/static/`

Incluye `index.html`, JavaScript, CSS y bibliotecas locales. Railway publica frontend y API bajo el mismo dominio. No subir esta carpeta por separado a Vercel a menos que más adelante se decida separar la arquitectura y adaptar las URLs de la API y CORS.
