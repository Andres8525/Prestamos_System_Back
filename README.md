# Prestamos System — Backend

API REST (Spring Boot 3.3 · Java 21 · JPA) para la **gestión de microcréditos y rutas de cobro diario**:
registro de clientes, emisión de préstamos con cálculo automático de intereses, abonos diarios en la calle,
cuadre de caja por agente cobrador y **scoring de riesgo con Machine Learning**.

## Ejecutar

```bash
mvn spring-boot:run          # http://localhost:8081  (H2 en ./data, consola en /h2-console)
mvn test                     # pruebas unitarias e integración
```

En desarrollo se carga una cartera de demostración (3 cobradores, 15 clientes, ~27 préstamos con abonos
históricos) si no hay cobradores registrados. Se desactiva con `APP_SEED_ENABLED=false`.

### Variables de entorno (perfil `prod`, PostgreSQL)

En local se leen de un archivo `.env` en esta carpeta (copiar `.env.example`; el `.env` no se sube a git).
En Render se configuran en *Environment* del Web Service.

| Variable | Descripción |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod` |
| `DATABASE_URL` | *Internal Database URL* de Render (`postgresql://usuario:clave@host/base`) o URL JDBC |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | solo si `DATABASE_URL` no incluye usuario y clave |
| `ALLOWED_ORIGINS` | orígenes CORS separados por coma (ej. `https://*.vercel.app`) |
| `JWT_SECRET` | clave para firmar las sesiones (32+ caracteres). Si falta, se genera una y se guarda en la base de datos |
| `ML_CRON` | horario del reentrenamiento diario del modelo (por defecto `0 0 3 * * *`) |
| `APP_SEED_ENABLED` | `true` para cargar la cartera de demostración si la base está vacía |
| `KEEPALIVE_ENABLED` / `KEEPALIVE_URL` | keep-alive (activo por defecto). En Render la URL se toma sola de `RENDER_EXTERNAL_URL` |
| `WORKERS_THREADS` | hilos del pool de workers (por defecto 2) |
| `GRPC_ENABLED` / `GRPC_PORT` | servidor gRPC (por defecto activo en el puerto 9090) |
| `GROQ_API_KEY` | clave de [Groq](https://console.groq.com/keys) para el asistente de IA. Sin ella, el asistente responde en modo local |
| `GROQ_MODEL` | modelo de Groq (por defecto `llama-3.3-70b-versatile`) |
| `GROQ_RATE_LIMIT` | consultas al asistente por usuario y minuto (por defecto 10) |

Ejecutar en local contra PostgreSQL: `mvn spring-boot:run -Dspring-boot.run.profiles=prod`
(desde fuera de Render use la *External Database URL*; el hostname interno solo resuelve dentro de Render).

### Despliegue en Render

1. *New + → Blueprint* y seleccionar este repositorio (usa `render.yaml`), o *New + → Web Service* con runtime **Docker**.
2. Ingresar `DATABASE_URL` con la *Internal Database URL* de la base (misma región: Oregon).
3. Desplegar y verificar `https://<servicio>.onrender.com/api/health` (Health Check Path: `/api/health`).

### Servidor siempre activo (sin esperar a que despierte)

El plan gratuito de Render suspende el servicio tras 15 minutos sin tráfico, y el siguiente login espera ≈ 1 minuto.
El proyecto lo evita con tres mecanismos complementarios:

1. **Keep-alive interno** (`KeepAliveService`): cada 10 minutos el backend llama a su propia URL pública
   (`RENDER_EXTERNAL_URL`/api/ping). Mientras el proceso esté vivo, nunca llega a los 15 minutos de inactividad.
2. **Workflow de GitHub Actions** (`.github/workflows/keep-alive.yml`): respaldo externo cada 10 minutos que además
   **despierta** el servicio si se llegó a suspender (por un reinicio o un deploy). Defina la variable de repositorio
   `BACKEND_URL` (Settings → Secrets and variables → Actions → Variables) si la URL no es `https://cobro-backend.onrender.com`.
3. **Frontend**: al abrir la app despierta el servidor y, mientras está abierta, lo llama cada pocos minutos.

> Un servicio gratuito encendido todo el mes consume ≈ 730 de las 750 horas gratuitas mensuales de Render: alcanza
> para **un** servicio. Para garantía total (sin suspensiones ni límite de horas) use el plan *Starter*.

## Módulos y endpoints principales

Toda la API requiere `Authorization: Bearer <token>`, excepto `/api/auth/verificar-email`, `/api/auth/login`, `/api/auth/register`, `/api/health` y `/api/ping`.

| Módulo | Endpoints |
|---|---|
| Autenticación | `POST /api/auth/verificar-email`, `POST /api/auth/register`, `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout` |
| Cobradores | `GET/POST /api/cobradores`, `PUT/DELETE /api/cobradores/{id}` |
| Clientes | `GET/POST /api/clientes`, `GET /api/clientes/cobrador/{id}` (asignación a ruta con `cobradorId` y `ordenRuta`) |
| Préstamos | `POST /api/prestamos/simular`, `POST /api/prestamos`, `GET /api/prestamos[/cobrador/{id}]`, `POST /api/prestamos/actualizar-estados` |
| Cuotas | `GET /api/pagos/prestamo/{id}`, `GET /api/pagos/atrasados`, `POST /api/pagos/{id}/pagar` |
| Abonos | `POST /api/abonos`, `GET /api/abonos?fecha=`, `GET /api/abonos/prestamo/{id}` |
| Ruta de cobro | `GET /api/rutas/cobrador/{id}?fecha=` |
| Caja | `GET /api/caja/cobrador/{id}?fecha=`, `POST /api/caja/cierre`, `GET /api/caja/cobrador/{id}/historial` |
| Scoring IA | `GET /api/scoring/cliente/{id}`, `GET /api/scoring/modelo`, `POST /api/scoring/entrenar` |
| Machine Learning | `GET /api/ml/modelo`, `POST /api/ml/entrenar`, `GET /api/ml/entrenamientos`, `GET /api/ml/predicciones/cliente/{id}`, `POST /api/ml/simular` |
| Workers | `GET /api/workers/estado`, `GET/POST /api/workers/tareas`, `GET/DELETE /api/workers/tareas/{id}`, `POST /api/workers/tareas/{id}/reintentar`, `GET /api/workers/tareas/{id}/resultado` |
| gRPC (info) | `GET /api/grpc/info`, `GET /api/grpc/contratos` |
| Tiempo real | `WS /ws/eventos?token=…&desde=…`, `GET /api/tiempo-real/estado` |
| Búsqueda | `GET /api/clientes/buscar?q=&limite=`, `GET /api/rutas/cobrador/{id}/priorizada`, `GET /api/panel/antiguedad` |
| Asistente IA (Groq) | `GET /api/asistente/estado`, `POST /api/asistente/explicar/cliente/{id}`, `POST /api/asistente/preguntar` |

## Workers (tareas en segundo plano)

Patrón **productor-consumidor** con cola persistida:

- `TareaProceso` (tabla `tareas_proceso`) guarda cada tarea, su estado (`PENDIENTE → EN_PROCESO → COMPLETADA | FALLIDA`,
  o `CANCELADA`), intentos, worker que la atendió, duración y resultado.
- `ColaTareas`: cola en memoria con prioridad (mayor primero; a igual prioridad, orden de llegada).
- `WorkerPool`: N hilos (`WORKERS_THREADS`) que consumen la cola. Un fallo se reintenta con espera exponencial
  (2 s, 4 s, 8 s) hasta 3 intentos. Al reiniciar el servidor, las tareas pendientes vuelven a la cola.
- `ManejadorTarea` (patrón *Strategy*): un worker por tipo — `RECALCULAR_MORA`, `SCORING_CARTERA`,
  `REENTRENAR_MODELO`, `REPORTE_CARTERA` (CSV descargable) e `INFORME_IA` (informe redactado con Groq).
- Productores: la API (`POST /api/workers/tareas`), `ProgramadorTareas` (mora cada hora) y el reentrenamiento diario de ML.

## WebSocket (tiempo real)

Canal `/ws/eventos` (Spring WebSocket). El token de sesión va en el parámetro `token` porque el navegador no
permite cabeceras propias en el handshake; sin token válido la conexión se rechaza con 401.

| Evento | Cuándo |
|---|---|
| `ABONO_REGISTRADO` | se confirma un abono (REST o gRPC) |
| `PRESTAMO_CREADO` | se aprueba un préstamo (incluye nivel de riesgo) |
| `CAJA_CERRADA` | un cobrador cuadra su caja |
| `TAREA_ACTUALIZADA` | una tarea de los workers cambia de estado |
| `MODELO_ENTRENADO` | se reentrena el modelo de ML |
| `BIENVENIDA`, `LATIDO`, `CONEXIONES` | mensajes de sistema (conexión, latido cada 25 s, usuarios conectados) |

- Los servicios publican un `EventoNegocio` con `ApplicationEventPublisher`; `OyenteEventosNegocio` lo difunde
  **después del commit** (`@TransactionalEventListener`), así nunca se anuncia algo que terminó en rollback.
- Cada evento lleva un `id` creciente. Al reconectar, el cliente envía `desde=<último id>` y recibe lo que se perdió
  (historial circular de los últimos 50).
- Mensajes del cliente: `{"accion":"ping"}`, `{"accion":"suscribir","tipos":[...]}`, `{"accion":"desuscribir","tipos":[...]}`.
- En Render funciona sobre el mismo puerto HTTPS (`wss://<servicio>.onrender.com/ws/eventos`).

## gRPC

Servidor gRPC embebido en el puerto `GRPC_PORT` (9090), con contratos en `src/main/proto/crediruta/v1`
(las clases se generan con `protoc` al compilar). Implementa los cuatro patrones de comunicación:

| Servicio | Método | Patrón |
|---|---|---|
| `ScoringService` | `EvaluarCliente`, `Simular` | Unario |
| `ScoringService` | `EvaluarCartera` | Streaming de servidor |
| `CobranzaService` | `ObtenerRuta` | Streaming de servidor |
| `CobranzaService` | `RegistrarAbonos` | Streaming de cliente (lote de abonos tomados sin conexión) |
| `CobranzaService` | `SincronizarCobros` | Bidireccional (confirmación inmediata de cada abono) |

- **Autenticación**: el mismo token de la API REST en el metadato `authorization: Bearer <token>`.
- **Interceptores**: autenticación y registro de llamadas (métricas por método visibles en `/api/grpc/info`).
- **Health** estándar (`grpc.health.v1.Health`) y **reflection** (para `grpcurl` o Postman), ambos públicos.

```bash
grpcurl -plaintext localhost:9090 list
grpcurl -plaintext -H "authorization: Bearer $TOKEN" -d '{"cobrador_id": 1}'   localhost:9090 crediruta.v1.CobranzaService/ObtenerRuta
```

> Los navegadores no hablan gRPC nativo, por eso el frontend usa la API REST. En Render solo se publica el puerto
> HTTP: para consumir gRPC desde fuera se necesita un *Private Service* o un proxy (por ejemplo, Envoy con gRPC-Web).

## Asistente de IA (Groq)

`GroqClient` llama a la API de chat de Groq (compatible con OpenAI) y `AsistenteIaService` la usa para:
explicar en lenguaje natural el scoring de un cliente, responder preguntas sobre la cartera y redactar el
informe ejecutivo (worker `INFORME_IA`).

1. Cree una clave en <https://console.groq.com/keys>.
2. Defínala como `GROQ_API_KEY` en el backend (`.env` en local, *Environment* en Render). **Nunca** en el frontend.
3. Opcional: `GROQ_MODEL`, `GROQ_RATE_LIMIT`.

A Groq solo se envían indicadores anónimos o agregados (variables de pago, probabilidades, totales); nunca nombres,
cédulas, teléfonos ni direcciones. Sin clave, el asistente responde con un texto generado localmente.

## Reglas de negocio

- **Intereses:** interés simple `total = capital × (1 + tasa/100)`; tasa por defecto 20 %, cuotas por defecto
  40 diarias o 4 semanales (configurables). La última cuota absorbe el redondeo.
- **Planes (patrón Strategy):** `PlanDiario` (atraso desde 4 días) y `PlanSemanal` (atraso desde 7 días).
- **Abonos:** pueden ser parciales o cubrir varias cuotas; se aplican FIFO a la cuota impaga más antigua.
  Un abono no puede superar el saldo del préstamo. Al cubrir todas las cuotas el préstamo queda `PAGADO`.
- **Caja:** `efectivo esperado = base + cobrado − prestado − gastos`; `diferencia = entregado − esperado`
  (CUADRADA / SOBRANTE / FALTANTE). Con la caja cerrada no se aceptan abonos ni desembolsos ese día.

- **Autenticación:** contraseñas con PBKDF2-HMAC-SHA256 y sal por usuario; sesiones JWT HS256 de 8 horas.
  En desarrollo se crea el usuario `admin@cobro.com` / `Admin123*` (en producción solo si se define
  `DEMO_ADMIN_PASSWORD`).

## Machine Learning: scoring de riesgo

El objetivo es predecir la **probabilidad de incumplimiento (default)** de un cliente a partir de su
historial de abonos, para alertar al prestamista antes de aprobar una renovación.

**Variables** (calculadas del historial de cuotas y abonos): días promedio y máximo de atraso,
proporción de cuotas atrasadas, frecuencia de abonos incompletos, regularidad de los abonos,
proporción del saldo vencido sin pagar y préstamos anteriores cancelados.

**Etiqueta:** un préstamo con desenlace conocido es 1 si alguna cuota superó 30 días de mora y 0 si se
canceló. Sus variables se miden a mitad del plazo, para que el modelo aprenda a anticipar el desenlace
sin ver el futuro. La cartera real se complementa con 800 casos sintéticos reproducibles (las muestras
reales pesan ×5).

**Algoritmos** (interfaz común `ModeloClasificacion`, implementados en Java sin librerías externas):

| Algoritmo | Clase | Explicación de cada predicción |
|---|---|---|
| Regresión logística (descenso de gradiente + L2) | `RegresionLogistica` | peso × valor estandarizado |
| Árbol de decisión CART (índice de Gini, profundidad 4) | `ArbolDecision` | cambio de probabilidad en el camino del árbol |

**Entrenamiento y selección:**
1. Se separa un 20 % estratificado como conjunto de **prueba**.
2. Cada algoritmo se evalúa con **validación cruzada de 5 pliegues** sobre el 80 % restante.
3. Se elige el de mejor AUC promedio, se mide en prueba (matriz de confusión, exactitud, precisión,
   sensibilidad, especificidad, F1, AUC-ROC, curva ROC y Brier score) y se reentrena con todos los datos.
4. Cada entrenamiento queda registrado (`entrenamientos_modelo`). Se reentrena al iniciar, con
   `POST /api/ml/entrenar` y cada día a las 03:00, y en ese momento se guarda una predicción de seguimiento
   de cada cliente activo (`predicciones_riesgo`).

**Uso:** antes de aprobar una **renovación**, si la probabilidad es ≥ 60 % (riesgo ALTO) la API responde
`409` con el scoring y solo aprueba si se envía `forzarAprobacion: true`.
Umbrales: < 30 % BAJO (aprobar), 30–60 % MEDIO (revisar), ≥ 60 % ALTO (rechazar).
