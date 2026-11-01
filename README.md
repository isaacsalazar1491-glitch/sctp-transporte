# SCTP · Sistema de Control de Transporte Público

Java 21 · JavaFX · PostgreSQL · JDBC · BCrypt. Capas: `controller` (JavaFX) → `service` → `dao` → PostgreSQL.

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
