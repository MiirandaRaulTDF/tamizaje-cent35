# Tamizaje · Spring Boot, Vue.js y Bootstrap

Esta rama integra toda la aplicación en **un único proyecto Spring Boot**. La interfaz Vue 3.5.44 y Bootstrap 5.3.8 está en `src/main/resources/static` y utiliza la API del mismo servidor. `bootRun` y el JAR ejecutable sirven la interfaz y `/api` desde un solo puerto. No requiere Node.js, npm, Vite ni una compilación de frontend. Las bibliotecas y sus licencias se incluyen localmente; el navegador no necesita descargar recursos de una CDN.

## Probar con base integrada

Requiere **JDK 17** (recomendado para el wrapper Gradle 7.6.4). Desde la raíz:

```sh
# macOS / Linux
bash gradlew bootRun --args='--spring.profiles.active=demo'
```

```bat
:: Windows
 gradlew.bat bootRun --args="--spring.profiles.active=demo"
```

Después de `Started TamizajeApplication`, escribí `http://localhost:8080` en el navegador de esa computadora. Conservá abierta la terminal. El perfil `demo` usa H2, escucha solo en la computadora local y conserva los registros en `.tamizaje-demo/`. Usá datos ficticios.

Para obtener **un único JAR con frontend, API y bibliotecas**:

```sh
bash gradlew bootJar
java -jar build/libs/tamizaje-0.0.1-SNAPSHOT.jar --spring.profiles.active=demo
```

En Windows, reemplazá `bash gradlew` por `gradlew.bat`.

## Usar MySQL

Sin el perfil `demo`, se conserva la conexión definida en `src/main/resources/application.properties`: MySQL en el puerto 3307, base `tamizaje`. Configurá la conexión mediante `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME` y `SPRING_DATASOURCE_PASSWORD` si tu instalación usa otros datos; no guardes credenciales en el código. Iniciá con `bash gradlew bootRun` o el JAR sin `--spring.profiles.active=demo`.

## Flujo de prueba

La pantalla inicial guía la atención **paso a paso**, mostrando una sección a la vez:

1. Agregá el enfermero (nombre, apellido y turno), o elegí uno registrado.
2. Agregá el paciente (fecha de nacimiento y sexo), o elegí uno registrado.
3. Confirmá el código de ficha y el consentimiento. El supervisor es opcional.
4. Ingresá peso, talla y cintura en **Medidas**.
5. Registrá la **Presión arterial**.
6. Completá **Hábitos y antecedentes**.
7. En **Revisar y guardar**, verificá el resumen. Los botones **Editar** permiten volver a cada sección. Presioná **Guardar atención y calcular resultado** para confirmar.

**Continuar** valida los datos del paso actual. **Anterior** y los pasos ya recorridos permiten volver conservando lo ingresado. No se crean registros al avanzar: el enfermero, el paciente, la ficha, los controles y el resultado se guardan juntos en una transacción al confirmar el último paso. Si hay un error, no quedan altas parciales y se conservan los datos para corregirlos.

Al guardar, se muestra el informe para revisar e imprimir. **Nueva atención** permite continuar con otro paciente reutilizando el enfermero recién registrado.

Los listados anteriores permiten consultar registros y abrir fichas para agregar/editar/eliminar tomas, recalcular e imprimir resultados. La ficha se puede reabrir al recargar usando su identificador en el fragmento de URL.

La API original permite crear y consultar antropometría/cuestionario, sin edición; los registros existentes se muestran como solo lectura para evitar duplicados. El resultado es un cálculo guardado: recalculá después de cambiar controles. FINDRISC mantiene la variante del backend sin cintura ni glucemia previa. La aplicación aún no incluye autenticación.

## Estructura y validación

- `src/main/resources/static/index.html`: plantilla Vue y componentes Bootstrap.
- `src/main/resources/static/js/app.js`: estado, formularios y flujo de evaluación.
- `src/main/resources/static/js/api.js`: cliente del mismo origen y validación de datos.
- `src/main/resources/static/vendor/`: versiones fijas, licencias y hashes SHA-256 de bibliotecas locales.
- `src/main/resources/application-demo.properties`: configuración de prueba local.

```sh
# Integración web y perfil demo, sin necesitar MySQL
bash gradlew test --tests cent35.WebUiIntegrationTests --tests cent35.TamizajeCompletoIntegrationTests

# Toda la suite Java; la prueba original de contexto usa MySQL por defecto
bash gradlew test

# Validadores JavaScript (opcional; Node.js 18+, sin npm install)
node --test src/test/js/*.test.mjs
```

En desarrollo, `bootRun` ya usa `sourceResources`: se editan los archivos estáticos y se recarga el navegador. Para distribuir cambios, reconstruí el JAR.
