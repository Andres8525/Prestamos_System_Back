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
| `APP_SEED_ENABLED` | `true` para cargar la cartera de demostración si la base está vacía |

Ejecutar en local contra PostgreSQL: `mvn spring-boot:run -Dspring-boot.run.profiles=prod`
(desde fuera de Render use la *External Database URL*; el hostname interno solo resuelve dentro de Render).

### Despliegue en Render

1. *New + → Blueprint* y seleccionar este repositorio (usa `render.yaml`), o *New + → Web Service* con runtime **Docker**.
2. Ingresar `DATABASE_URL` con la *Internal Database URL* de la base (misma región: Oregon).
3. Desplegar y verificar `https://<servicio>.onrender.com/api/cobradores`.

## Módulos y endpoints principales

| Módulo | Endpoints |
|---|---|
| Cobradores | `GET/POST /api/cobradores`, `PUT/DELETE /api/cobradores/{id}` |
| Clientes | `GET/POST /api/clientes`, `GET /api/clientes/cobrador/{id}` (asignación a ruta con `cobradorId` y `ordenRuta`) |
| Préstamos | `POST /api/prestamos/simular`, `POST /api/prestamos`, `GET /api/prestamos[/cobrador/{id}]`, `POST /api/prestamos/actualizar-estados` |
| Cuotas | `GET /api/pagos/prestamo/{id}`, `GET /api/pagos/atrasados`, `POST /api/pagos/{id}/pagar` |
| Abonos | `POST /api/abonos`, `GET /api/abonos?fecha=`, `GET /api/abonos/prestamo/{id}` |
| Ruta de cobro | `GET /api/rutas/cobrador/{id}?fecha=` |
| Caja | `GET /api/caja/cobrador/{id}?fecha=`, `POST /api/caja/cierre`, `GET /api/caja/cobrador/{id}/historial` |
| Scoring IA | `GET /api/scoring/cliente/{id}`, `GET /api/scoring/modelo`, `POST /api/scoring/entrenar` |

## Reglas de negocio

- **Intereses:** interés simple `total = capital × (1 + tasa/100)`; tasa por defecto 20 %, cuotas por defecto
  40 diarias o 4 semanales (configurables). La última cuota absorbe el redondeo.
- **Planes (patrón Strategy):** `PlanDiario` (atraso desde 4 días) y `PlanSemanal` (atraso desde 7 días).
- **Abonos:** pueden ser parciales o cubrir varias cuotas; se aplican FIFO a la cuota impaga más antigua.
  Un abono no puede superar el saldo del préstamo. Al cubrir todas las cuotas el préstamo queda `PAGADO`.
- **Caja:** `efectivo esperado = base + cobrado − prestado − gastos`; `diferencia = entregado − esperado`
  (CUADRADA / SOBRANTE / FALTANTE). Con la caja cerrada no se aceptan abonos ni desembolsos ese día.

## Scoring de riesgo (IA)

Modelo de **regresión logística** implementado en Java (`ia/RegresionLogistica`), entrenado con descenso
de gradiente y regularización L2 sobre variables estandarizadas.

**Variables** (calculadas del historial de cuotas y abonos del cliente): días promedio y máximo de atraso,
proporción de cuotas atrasadas, frecuencia de abonos incompletos, regularidad de los abonos, proporción de
saldo vencido sin pagar y préstamos anteriores cancelados.

**Entrenamiento:** cada préstamo con desenlace conocido aporta una muestra con el comportamiento observado
hasta la mitad del plazo; etiqueta 1 si alguna cuota superó 30 días de mora, 0 si se canceló. Se complementa
con una cartera sintética reproducible (las muestras reales pesan ×5). Se entrena al iniciar y con
`POST /api/scoring/entrenar`.

**Uso:** antes de aprobar una **renovación**, si la probabilidad de incumplimiento es ≥ 60 % (riesgo ALTO)
la API responde `409` con el scoring y solo aprueba si se envía `forzarAprobacion: true`.
Umbrales: < 30 % BAJO (aprobar), 30–60 % MEDIO (revisar), ≥ 60 % ALTO (rechazar).
