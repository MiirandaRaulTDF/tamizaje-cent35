# Tamizaje CENT35: despliegue para la muestra

Esta carpeta contiene el paquete preparado para publicar el sistema en internet. La aplicación es una sola unidad Spring Boot: **backend y frontend se despliegan juntos desde `backend/`**. El navegador recibe la interfaz web y esa interfaz consume la API del mismo servidor.

## Carpetas

- `backend/`: aplicación Spring Boot completa, incluidos `src/main/java` (API, entidades y lógica) y `src/main/resources/static` (Vue/Bootstrap, HTML, CSS y JavaScript). Esta es la carpeta que Railway debe construir.
- `frontend/`: explicación de dónde está la interfaz. No contiene una segunda aplicación independiente porque no hace falta para este proyecto.
- `db/`: instrucciones de creación de la base Neon. No hay que subir un SQL: Hibernate crea las tablas desde las entidades Java al iniciar.

## Arquitectura recomendada

- **Railway:** ejecuta la aplicación completa y le da una URL pública.
- **Neon:** aloja PostgreSQL.
- **Vercel:** no se necesita en esta versión; el frontend ya está servido por Spring Boot.

## Preparación de Neon

1. Crear un proyecto PostgreSQL en [Neon](https://console.neon.tech/).
2. En **Connect**, elegir la rama y la base de datos y copiar los datos de conexión.
3. Guardar host, base, usuario y contraseña. No poner la contraseña en archivos del proyecto ni en GitHub.
4. En Railway, cargar estas variables en el servicio backend:

   - `SPRING_DATASOURCE_URL`: `jdbc:postgresql://HOST_NEON/BASE?sslmode=require` (usar el host y la base que muestra Neon).
   - `SPRING_DATASOURCE_USERNAME`: usuario de Neon.
   - `SPRING_DATASOURCE_PASSWORD`: contraseña de Neon.

La conexión debe incluir SSL (`sslmode=require`). Si Neon ofrece un modo de conexión pooled y directo, para esta aplicación elegir el **pooled** es adecuado para un servicio pequeño con pocas conexiones.

## Subir el código a GitHub

Crear un repositorio vacío en GitHub y publicar **todo el contenido de esta carpeta**. Después, en Railway, conectar ese repositorio y configurar en el servicio el **Root Directory** como `/backend`. La configuración `backend/railpack.json` fija Java 17, compatible con Gradle 7.6.4. El archivo `railway.json` en la raíz define la compilación del JAR sin correr la suite de pruebas, que tiene una prueba de contexto configurada para MySQL local.

No subir `backend/.env` ni contraseñas. El ejemplo `backend/.env.example` solo muestra nombres y formatos; no contiene credenciales reales.

## Railway

1. Crear un proyecto y elegir **Deploy from GitHub repo**.
2. Seleccionar el repositorio publicado.
3. En configuración del servicio, establecer **Root Directory** = `/backend`.
4. En **Variables**, cargar las tres variables de Neon indicadas arriba.
5. Railway debe leer `railway.json` (si pide seleccionar el archivo de configuración, indicar `/railway.json`) y usar `/backend` como Root Directory. El build compila el JAR sin ejecutar pruebas y luego lo inicia. Revisar los logs del despliegue.
6. En **Settings → Networking**, generar un dominio público. Compartir ese enlace para que cualquier persona pueda abrir el sistema.

La aplicación toma el puerto de Railway (`PORT`) y localmente usa el 8080. No hay que crear un segundo servicio para el frontend.

## Tablas y datos

`spring.jpa.hibernate.ddl-auto=update` hace que Hibernate cree las tablas que faltan al iniciar y ajuste algunas diferencias simples entre las entidades y el esquema. No se necesita importar un archivo SQL. La aplicación crea el esquema, pero **no migra los datos de la base H2 demo ni los datos de MySQL**.

Usar solo datos ficticios para la muestra: el sistema no incluye autenticación y recoge información de salud. Al terminar la muestra, detener o eliminar el servicio Railway y borrar el proyecto Neon si ya no se usará.

## Modo local

Requiere JDK 17 y PostgreSQL local, con base `tamizaje` y credenciales configuradas. Para usar la base H2 de demostración en cambio, ejecutar desde `backend/`:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.14.7-hotspot'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

Abrir `http://localhost:8080`.

## Costos

Neon y Railway tienen planes gratuitos con límites de uso que pueden cambiar. Railway indica que su plan Free da US$1 de crédito mensual después de la prueba; eso no garantiza que un backend permanezca activo todo el mes. Revisar consumo y precio actual en sus páginas antes de la muestra. Para una exposición de un día puede ser suficiente, pero hay que probar el despliegue y la URL con anticipación.

- [Precios y límites de Railway](https://railway.com/pricing)
- [Prueba y plan Free de Railway](https://docs.railway.com/pricing/free-trial)
- [Precios de Neon](https://neon.com/pricing)
- [Guía de Spring Boot en Railway](https://docs.railway.com/guides/spring-boot)
- [Despliegue de repositorios con varias carpetas en Railway](https://docs.railway.com/deployments/monorepo)
