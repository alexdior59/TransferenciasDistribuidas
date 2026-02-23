#  Taller: Transacciones Distribuidas con Patrón SAGA

**Asignatura:** Arquitectura de Software (3384)  
**Institución:** Pontificia Universidad Javeriana  
**Autores:** Julián Díaz · Samuel Pachón · Juan Diego Barreto

---

## Descripción

Este proyecto implementa un sistema de transferencias bancarias entre dos motores de bases de datos heterogéneos (**PostgreSQL** y **MySQL**) usando el **patrón SAGA con orquestación centralizada**. El objetivo es demostrar cómo garantizar consistencia eventual en sistemas distribuidos sin recurrir al protocolo Two-Phase Commit (2PC/XA).

El sistema permite transferir fondos desde cuentas del **Banco Nacional** (PostgreSQL) hacia cuentas del **Banco Internacional** (MySQL), manejando los tres escenarios posibles: transferencia exitosa, fallo por saldo insuficiente, y compensación automática ante fallo en el destino.

---

## Stack Tecnológico

| Capa | Tecnología |
|------|-----------|
| Backend | Spring Boot 3.2.0 · Java 17 |
| ORM | Spring Data JPA · Hibernate |
| Banco Nacional | PostgreSQL 15 |
| Banco Internacional | MySQL 8.0 |
| Frontend | HTML5 · CSS3 · JavaScript (Vanilla) |
| Contenedores | Docker · Docker Compose |
| Build | Maven (Maven Wrapper incluido) |
| Utilidades | Lombok · Bean Validation |

---

## Prerequisitos

Antes de ejecutar el proyecto, asegúrate de tener instalado:

- **Java 17** o superior
- **Maven** (o usar el wrapper `./mvnw` incluido)
- **Docker Desktop** en ejecución

---

## Instalación y Ejecución

### 1. Clonar el repositorio

```bash
git clone <url-del-repositorio>
cd TransferenciasDistribuidas
```

### 2. Compilar el proyecto

```bash
./mvnw clean package -DskipTests
```

> En Windows usar `mvnw.cmd clean package -DskipTests`

### 3. Levantar la infraestructura con Docker

```bash
docker-compose up -d --build
```

Este comando levanta tres contenedores:
- `banco_nacional_db` — PostgreSQL 15 en el puerto `5432`
- `banco_internacional_db` — MySQL 8.0 en el puerto `33067`
- `transferencias_app` — Spring Boot en el puerto `8080`

> La aplicación espera automáticamente a que ambas bases de datos estén saludables antes de iniciar (healthchecks configurados).

### 4. Acceder a la interfaz

Abrir en el navegador:

```
http://localhost:8080
```

### 5. Detener el sistema

```bash
docker-compose down -v
```

> El flag `-v` elimina los volúmenes de datos. Omitirlo si se quiere persistir los datos entre reinicios.

---

## Arquitectura del Sistema

```
┌─────────────────────────────────────────────────────────┐
│                      FRONTEND                           │
│          HTML5 + CSS3 + JavaScript (Vanilla)            │
│              Fetch API → POST /api/transferencias       │
└─────────────────────┬───────────────────────────────────┘
                      │ HTTP REST
┌─────────────────────▼───────────────────────────────────┐
│              BACKEND (Orquestador SAGA)                 │
│                 Spring Boot 3.2 / Java 17               │
│                                                         │
│  TransferenciaController                                │
│         │                                               │
│  TransferenciaService  ◄── Orquestador SAGA             │
│     ├── BancoNacionalService    (TX Manager Nacional)   │
│     └── BancoInternacionalService (TX Manager Intl.)    │
└────────────┬────────────────────────┬───────────────────┘
             │ JPA / Hibernate        │ JPA / Hibernate
             │ PESSIMISTIC_WRITE      │ PESSIMISTIC_WRITE
┌────────────▼──────────┐  ┌──────────▼──────────────────┐
│    BANCO NACIONAL     │  │    BANCO INTERNACIONAL      │
│   PostgreSQL 15       │  │        MySQL 8.0            │
│   puerto: 5432        │  │      puerto: 33067          │
│                       │  │                             │
│  tabla: cuenta        │  │  tabla: cuenta              │
│  tabla: movimiento    │  │  tabla: movimiento          │
└───────────────────────┘  └─────────────────────────────┘
```

### Componentes principales

**`TransferenciaController`** — Expone el endpoint `POST /api/transferencias`. Valida el request con Bean Validation y delega al orquestador SAGA. Retorna `200 OK` en éxito o `400 Bad Request` ante fallo o reversión.

**`TransferenciaService`** — Es el orquestador central del patrón SAGA. Coordina la secuencia de transacciones locales y decide cuándo ejecutar la compensación. Genera una referencia UUID única por transferencia para trazabilidad.

**`BancoNacionalService`** — Gestiona operaciones sobre PostgreSQL con su propio `TransactionManager` (`nacionalTransactionManager`). Implementa `debitar()` y `revertirDebito()` (transacción compensatoria).

**`BancoInternacionalService`** — Gestiona operaciones sobre MySQL con su propio `TransactionManager` (`internacionalTransactionManager`). Implementa `acreditar()`.

**Configuración de DataSources** — `BancoNacionalDataSourceConfig` y `BancoInternacionalDataSourceConfig` configuran dos DataSources independientes con pools HikariCP (máximo 10 conexiones cada uno), EntityManagerFactories y TransactionManagers separados.

---

## Flujo del Patrón SAGA

```
INICIO
  │
  ├─ Paso 0: Generar referencia UUID única
  │
  ├─ Paso 1: debitar() en Banco Nacional (PostgreSQL)
  │   ├─ FALLO → Estado: FALLIDA (sin compensación, destino intacto)
  │   └─ ÉXITO → Estado: DEBITO_COMPLETADO
  │
  ├─ Paso 3: acreditar() en Banco Internacional (MySQL)
  │   ├─ FALLO → Ejecutar revertirDebito() en Banco Nacional
  │   │           Estado: REVERTIDA (compensación exitosa)
  │   └─ ÉXITO → Estado: CREDITO_COMPLETADO
  │
  └─ Paso 5: Estado: COMPLETADA
```

### Estados de una transferencia

| Estado | Descripción |
|--------|-------------|
| `INICIADA` | Transferencia registrada, aún no procesada |
| `DEBITO_COMPLETADO` | Débito exitoso en Banco Nacional |
| `CREDITO_COMPLETADO` | Crédito exitoso en Banco Internacional |
| `COMPLETADA` | Transferencia finalizada exitosamente |
| `FALLIDA` | Fallo en el débito origen, sin afectar el destino |
| `REVERTIDA` | Fallo en el crédito destino, débito compensado |

---

##  Datos de Prueba

### Banco Nacional — PostgreSQL

| Número de Cuenta | Titular | Saldo Inicial |
|-----------------|---------|---------------|
| `BN-001` | Juan Pérez | $5,000.00 |
| `BN-002` | María García | $10,000.00 |
| `BN-003` | Carlos Rodríguez | $2,500.00 |
| `BN-004` | Ana Martínez | $15,000.00 |

### Banco Internacional — MySQL

| Número de Cuenta | Titular | Saldo Inicial |
|-----------------|---------|---------------|
| `BI-001` | Laura Sánchez | $8,000.00 |
| `BI-002` | Pedro López | $3,000.00 |
| `BI-003` | Sofía Hernández | $12,000.00 |
| `BI-004` | Diego Torres | $6,000.00 |

---

## Escenarios de Prueba

### Escenario 1: Transferencia exitosa

Transferir un monto dentro del saldo disponible desde una cuenta nacional hacia una internacional.

```
Cuenta origen:  BN-001  (saldo: $5,000.00)
Cuenta destino: BI-001
Monto:          $1,000.00
Resultado esperado: TRANSFERENCIA COMPLETADA EXITOSAMENTE
```

**Resultado en BD:**
- `BN-001`: saldo disminuye en $1,000.00
- `BI-001`: saldo aumenta en $1,000.00
- Ambos registran movimiento con la misma `referencia_transferencia`

---

### Escenario 2: Fallo por saldo insuficiente

Intentar transferir un monto mayor al saldo disponible.

```
Cuenta origen:  BN-003  (saldo: $2,500.00)
Cuenta destino: BI-002
Monto:          $5,000.00
Resultado esperado: TRANSFERENCIA FALLIDA - Saldo insuficiente en la cuenta origen
```

**Resultado en BD:**
- Ninguna base de datos es modificada
- El fallo ocurre antes de tocar el destino

---

### Escenario 3: Compensación por fallo simulado en destino

Para simular este escenario, descomentar la línea del `throw` en `TransferenciaService.java`:

```java
// Aquí podrías simular un fallo lanzando una excepción para probar el Caso 3
// throw new RuntimeException("Fallo simulado en el banco destino");
internacionalService.acreditar(cuentaDestino, monto, referencia);
```

```
Cuenta origen:  BN-001
Cuenta destino: BI-001
Monto:          $500.00
Resultado esperado: TRANSFERENCIA REVERTIDA - Fallo en el banco de destino
```

**Resultado en BD:**
- El débito en `BN-001` es revertido automáticamente
- `BI-001` no registra ningún cambio
- Los logs muestran la ejecución de `revertirDebito()`

---

##  API REST

### Endpoint: Ejecutar transferencia

```http
POST /api/transferencias
Content-Type: application/json
```

**Request body:**

```json
{
  "cuentaOrigen": "BN-001",
  "cuentaDestino": "BI-001",
  "monto": 1000.00
}
```

**Respuestas:**

| HTTP Status | Body | Descripción |
|-------------|------|-------------|
| `200 OK` | `"TRANSFERENCIA COMPLETADA EXITOSAMENTE"` | Éxito total |
| `400 Bad Request` | `"TRANSFERENCIA FALLIDA - Saldo insuficiente..."` | Fallo en origen |
| `400 Bad Request` | `"TRANSFERENCIA REVERTIDA - Fallo en el banco de destino"` | Compensación ejecutada |

**Validaciones del request:**
- `cuentaOrigen`: obligatorio, no vacío
- `cuentaDestino`: obligatorio, no vacío
- `monto`: obligatorio, mayor a 0

---

##  Configuración

### Variables de entorno (Docker)

| Variable | Valor por defecto | Descripción |
|----------|------------------|-------------|
| `DB_NACIONAL_HOST` | `localhost` | Host de PostgreSQL |
| `DB_NACIONAL_PORT` | `5432` | Puerto de PostgreSQL |
| `DB_INTERNACIONAL_HOST` | `localhost` | Host de MySQL |
| `DB_INTERNACIONAL_PORT` | `3306` | Puerto de MySQL |
| `SPRING_PROFILES_ACTIVE` | — | Perfil activo (`docker` reduce verbosidad de logs) |

### Conexión directa a las bases de datos

**PostgreSQL (Banco Nacional):**
```
Host:     localhost
Puerto:   5432
Base:     banco_nacional
Usuario:  admin_nacional
Password: nacional123
```

**MySQL (Banco Internacional):**
```
Host:     localhost
Puerto:   33067   ← puerto externo mapeado
Base:     banco_internacional
Usuario:  admin_internacional
Password: internacional123
```

---

##  Decisiones de Diseño

### ¿Por qué no 2PC/XA?

El protocolo Two-Phase Commit fue descartado por tres razones concretas:

1. **Incompatibilidad entre motores** — PostgreSQL y MySQL tienen implementaciones XA con diferentes niveles de soporte y compatibilidad limitada entre sí.
2. **Acoplamiento y latencia** — 2PC requiere un Transaction Manager externo (Atomikos, Bitronix), introduciendo un punto de falla adicional y latencia sincrónica en todas las operaciones.
3. **Penalización en disponibilidad** — Durante la fase de preparación, los recursos quedan bloqueados hasta que todos los participantes confirmen, degradando la disponibilidad del sistema.

### ¿Por qué SAGA Orquestado?

El patrón SAGA divide la transferencia en transacciones locales independientes. Cada base de datos usa su propio gestor transaccional con propiedades ACID locales. La coordinación central en `TransferenciaService` simplifica el razonamiento sobre el flujo y centraliza la lógica de compensación.

El trade-off consciente es sacrificar **consistencia inmediata (fuerte)** a favor de **alta disponibilidad** y **rendimiento**, alcanzando **consistencia eventual**.

### Bloqueos pesimistas (`PESSIMISTIC_WRITE`)

Para manejar la concurrencia en transferencias simultáneas sobre la misma cuenta, se implementaron bloqueos a nivel de fila (`LockModeType.PESSIMISTIC_WRITE`) en los repositorios. Esto encola las transacciones concurrentes sobre una misma cuenta, previniendo race conditions e inconsistencias en los saldos sin necesitar coordinación entre bases de datos.

### Dos DataSources, dos TransactionManagers

La configuración de Spring Boot con múltiples DataSources requiere declarar explícitamente un `EntityManagerFactory` y un `TransactionManager` por cada fuente de datos. Los servicios usan `@Transactional(transactionManager = "nacionalTransactionManager")` o `"internacionalTransactionManager"` según corresponda, garantizando que cada transacción local sea gestionada por el motor correcto.

---

##  Estructura del Proyecto

```
src/main/java/com/universidad/
├── UniversidadApplication.java
└── transferencias/
    ├── config/
    │   ├── BancoNacionalDataSourceConfig.java      ← DataSource + EntityManager + TxManager PostgreSQL
    │   └── BancoInternacionalDataSourceConfig.java ← DataSource + EntityManager + TxManager MySQL
    ├── controller/
    │   └── TransferenciaController.java            ← POST /api/transferencias
    ├── dto/
    │   └── TransferenciaRequest.java               ← Request con validaciones
    ├── model/
    │   ├── Cuenta.java                             ← Entidad JPA compartida
    │   ├── Movimiento.java                         ← Registro de operaciones
    │   └── EstadoTransferencia.java                ← Enum de estados SAGA
    ├── repository/
    │   ├── nacional/
    │   │   ├── CuentaNacionalRepository.java       ← JPA repo con lock pesimista (PostgreSQL)
    │   │   └── MovimientoNacionalRepository.java
    │   └── internacional/
    │       ├── CuentaInternacionalRepository.java  ← JPA repo con lock pesimista (MySQL)
    │       └── MovimientoInternacionalRepository.java
    └── service/
        ├── TransferenciaService.java               ← Orquestador SAGA
        ├── BancoNacionalService.java               ← debitar() + revertirDebito()
        └── BancoInternacionalService.java          ← acreditar()

src/main/resources/
├── application.yml                                 ← Configuración dual DataSource
├── schema-nacional.sql                             ← DDL PostgreSQL
├── schema-internacional.sql                        ← DDL MySQL
├── data-nacional.sql                               ← Datos semilla PostgreSQL
├── data-internacional.sql                          ← Datos semilla MySQL
└── static/
    ├── index.html                                  ← Interfaz web
    ├── app.js                                      ← Lógica frontend (Fetch API)
    └── styles.css
```

---

##  Conceptos Clave Aplicados

**Patrón SAGA Orquestado** — Secuencia de transacciones locales con transacciones compensatorias para deshacer cambios ante fallos. Permite consistencia eventual sin coordinación distribuida síncrona.se aplico 
orquestado debido a que hay una clase central la cual coordina a las demas y no funciona por eventos a diferencia del SAGA coreografico.

**Consistencia Eventual** — El sistema no garantiza que todas las réplicas estén sincronizadas en todo momento, pero garantiza que eventualmente alcanzarán un estado consistente.

**Transacción Compensatoria** — Operación inversa que revierte el efecto de una transacción anterior cuando la saga no puede completarse. En este caso, `revertirDebito()` es la compensación de `debitar()`.

**Bloqueo Pesimista** — Estrategia de concurrencia que bloquea el registro en la base de datos desde el momento en que se lee, impidiendo que otras transacciones lo modifiquen hasta que la transacción actual finalice.

**Multi-DataSource en Spring Boot** — Configuración de múltiples fuentes de datos con EntityManagerFactories y TransactionManagers independientes, permitiendo conectarse a diferentes motores de base de datos en la misma aplicación.

---

*Pontificia Universidad Javeriana — Arquitectura de Software *
