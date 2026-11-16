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

Ejecutar en local contra PostgreSQL: `mvn spring-boot:run -Dspring-boot.run.profiles=prod`
(desde fuera de Render use la *External Database URL*; el hostname interno solo resuelve dentro de Render).

### Despliegue en Render

1. *New + → Blueprint* y seleccionar este repositorio (usa `render.yaml`), o *New + → Web Service* con runtime **Docker**.
2. Ingresar `DATABASE_URL` con la *Internal Database URL* de la base (misma región: Oregon).
3. Desplegar y verificar `https://<servicio>.onrender.com/api/health` (Health Check Path: `/api/health`).

## Módulos y endpoints principales

Toda la API requiere `Authorization: Bearer <token>`, excepto `/api/auth/verificar-email`, `/api/auth/login`, `/api/auth/register` y `/api/health`.

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
