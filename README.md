# SCTP · Sistema de Control de Transporte Público

Java 21 · JavaFX · PostgreSQL · JDBC · BCrypt · Leaflet + OpenStreetMap (mapa gratuito, sin API key). Capas: `controller` (JavaFX) → `service` → `dao` → PostgreSQL.

## Base de datos
```
createdb -U postgres sctp
for f in 01_esquema_3fn 02_trigger_validacion 03_vistas_y_procedimientos 04_datos_prueba; do
  psql -U postgres -d sctp -f sql/$f.sql      # en ese orden
done
```
Copia `src/main/resources/db.properties.example` como `db.properties` (ignorado por git). Administrador de prueba: `admin` / `admin123`.

## API del backend para los controladores
| Servicio | Uso |
|---|---|
| `AuthService.autenticar(user, pass)` | Login (BCrypt). Lanza `ReglaNegocioException` con mensaje listo para mostrar |
| `AdministracionService` | CRUD de rutas, paradas, unidades, conductores, corridas; incidencias (`registrarIncidencia`, `cambiarEstadoIncidencia`); reportes |
| `ConsultaPublicaService` | `buscarRutas`, `horarios`, `proximasLlegadas`, `disponibilidad`, `incidencias`, `resumenIncidencias` (sin datos de conductores) |

Los errores de reglas de negocio (trigger, unicidad, checks) llegan como `ReglaNegocioException`; en el controlador basta `catch (ReglaNegocioException e)` y mostrar `e.getMessage()`.

## Reglas en la base de datos
- `trg_validar_corrida`: unidad DISPONIBLE/EN_RUTA, conductor activo, licencia vigente, parada de la misma ruta.
- `trg_incidencia_grave`: incidencia GRAVE no cerrada → unidad a MANTENIMIENTO (una unidad en BAJA no cambia). No se reactiva sola.
- `sp_cambiar_estado_incidencia`: ABIERTA → EN_ATENCION → CERRADA, sin saltos ni retrocesos; registra `fecha_cierre`.
- Vistas: `v_llegadas_estimadas`, `v_incidencias_publicas`, `v_unidades_por_estado`, `v_licencias_por_vencer`.
- Bajas lógicas; sin DELETE físico salvo paradas.

## Pruebas
Requieren una BD llamada `*_test` (las pruebas recrean el esquema; si no, se omiten). Copia `src/test/resources/db.properties.example` como `db.properties` y ejecuta `mvn test`.
Las consultas públicas usan hora fija (`-Dsctp.ahora=2026-10-26T07:06`); para una demo, usa esa misma opción en la JVM.

## Frontend (JavaFX) · rama `feature/frontend`
Pantallas basadas en los mockups `SCTP · Pantallas del frontend`.

| Pantalla | FXML / controlador | Servicio |
|---|---|---|
| Consulta pública (mapa, llegadas, disponibilidad, avisos) | `consulta_publica` / `ConsultaPublicaController` | `ConsultaPublicaService` |
| Acceso administrador | `login` / `LoginController` | `AuthService` |
| Estructura de administración (navegación) | `main` / `MainController` | — |
| Resumen de operación | `admin_dashboard` / `AdminDashboardController` | `AdministracionService` |
| Rutas y paradas (mapa para colocar paradas) | `rutas` / `RutasController` | `AdministracionService` |
| Unidades · Conductores · Corridas · Incidencias | `unidades`, `conductores`, `corridas`, `incidencias` | `AdministracionService` |

Código de apoyo en `ui/`: `Navegador` (cambio de vistas), `Sesion`, `Tareas` (la BD se consulta fuera del hilo de JavaFX), `Fmt` (formato y chips de color) y `MapaParadas` (mapa en un WebView). Estilos en `css/styles.css`.

**Ejecutar:** crea la BD (arriba), copia `db.properties` y corre `mvn javafx:run`. Para una demo con la hora de las pruebas agrega `-Dsctp.ahora=2026-10-26T07:06` a la JVM. Entra con `admin` / `admin123` desde "Acceso administrador".

### Mapa de paradas (OpenStreetMap, gratuito)
- Usa **Leaflet + teselas de OpenStreetMap** dentro de un `WebView` (`ui/MapaParadas`): sin API key ni cuenta. Requiere Internet (se carga Leaflet desde cdnjs y los mosaicos desde tile.openstreetmap.org). Muestra **solo las paradas** (puntos donde el camión sube/baja gente), sin trazar rutas.
- Pantalla **Rutas**: clic en el mapa = coloca una parada nueva; arrastrar un marcador = guarda su nueva ubicación; buscador de lugares con Nominatim (OSM).
- Consulta pública: el mapa muestra las paradas de la ruta; tocar un marcador elige esa parada.
- Opcional: `maps.properties` (ver `.example`) para el centro y zoom iniciales.
- Sin Internet el mapa muestra un aviso y en Rutas las coordenadas se pueden capturar a mano.
- Base de datos: `parada` ahora tiene `latitud` y `longitud` (opcionales). Si ya tienes la BD creada ejecuta `sql/05_coordenadas_paradas.sql`; si la recreas, `01` y `04` ya las incluyen.
