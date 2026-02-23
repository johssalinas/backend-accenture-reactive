# Prueba Práctica Backend - Gestión de Franquicias (Reactive)

API reactiva construida con Spring Boot (WebFlux), siguiendo como base el scaffold de **Clean Architecture de Bancolombia**. La solución implementa el dominio de franquicias, sucursales y productos con persistencia en PostgreSQL + Redis, empaquetado con Docker, pruebas automatizadas por niveles e infraestructura en AWS aprovisionada con Terraform.

## 1. Contexto y objetivo

La prueba solicita un API para administrar:

- Franquicias
- Sucursales por franquicia
- Productos por sucursal
- Stock por producto/sucursal

Además, se requiere evidencia de buenas prácticas de ingeniería: arquitectura limpia, trabajo con Git, documentación de despliegue local, y puntos extra de contenedorización, enfoque reactivo, IaC y despliegue cloud.

## 2. Base arquitectónica: Scaffold Clean Architecture (Bancolombia)

Referencia conceptual: [Clean Architecture — Aislando los detalles](https://medium.com/bancolombia-tech/clean-architecture-aislando-los-detalles-4f9530f35d7a).

La solución respeta la separación por capas del scaffold:

### Domain (`domain/model`)

Contiene entidades, objetos de valor, contratos (gateways) y reglas de negocio puras. No depende de frameworks.

### Use Cases (`domain/usecase`)

Orquesta la lógica de aplicación y casos de uso del negocio. Depende solo de abstracciones del dominio.

### Infrastructure

- **Entry Points** (`infrastructure/entry-points/reactive-web`): expone endpoints HTTP reactivos.
- **Driven Adapters** (`infrastructure/driven-adapters`): implementaciones concretas de persistencia e integraciones.
  - `r2dbc-postgresql`: persistencia principal transaccional.
  - `redis`: soporte de idempotencia y desempeño de operaciones técnicas.
- **Helpers**: utilidades transversales para adapter y entry points.

### Application (`applications/app-service`)

Ensambla dependencias, configura beans y arranca la aplicación Spring Boot.

## 3. Stack tecnológico

- Java + Spring Boot + WebFlux (programación reactiva)
- Gradle multi-módulo
- PostgreSQL (persistencia principal)
- Redis (persistencia de apoyo)
- R2DBC + Flyway
- Docker / Docker Compose
- JUnit5, Mockito, StepVerifier, WebTestClient, Karate, Testcontainers
- Terraform + GitHub Actions (GitOps para AWS)

## 4. Cumplimiento de criterios de aceptación

| Criterio solicitado | Estado | Evidencia funcional |
|---|---|---|
| API desarrollada en Spring Boot | ✅ OK | Proyecto basado en Spring Boot reactivo (`app-service`) |
| Agregar nueva franquicia | ✅ OK | `POST /api/franquicias` |
| Agregar nueva sucursal a franquicia | ✅ OK | `POST /api/sucursales` |
| Agregar nuevo producto a sucursal | ✅ OK | `POST /api/productos` con lista `sucursales` y `stock` |
| Eliminar producto de una sucursal/franquicia | ✅ OK | `DELETE /api/productos/{id}` |
| Modificar stock de producto | ✅ OK | Flujo soportado en el modelo reactivo de producto/sucursal y persistencia por upsert de stock |
| Consultar producto con mayor stock por sucursal para una franquicia | ✅ OK | `GET /api/franquicias/{franquiciaId}/productos/max-stock-por-sucursal` |
| Persistencia en motor de datos | ✅ OK | PostgreSQL (principal) + Redis (apoyo) |

### Puntos extra

| Plus | Estado | Evidencia |
|---|---|---|
| Empaquetado con Docker | ✅ OK | `deployment/Dockerfile` + `deployment/docker-compose.yml` |
| Programación funcional/reactiva | ✅ OK | Spring WebFlux + Reactor + StepVerifier |
| Actualizar nombre de franquicia | ✅ OK | `PATCH /api/franquicias/{id}` |
| Actualizar nombre de sucursal | ✅ OK | `PATCH /api/sucursales/{id}` |
| Actualizar nombre de producto | ✅ OK | `PATCH /api/productos/{id}` |
| Persistencia aprovisionada como IaC | ✅ OK | Terraform en `terraform/` |
| Despliegue en nube | ✅ OK | Arquitectura AWS + pipeline GitOps |

## 5. Endpoints principales

El set de pruebas y ejemplos de consumo está en `api-casos-uso.http`.

- Franquicias: `POST`, `GET`, `PATCH`, `DELETE` en `/api/franquicias`
- Sucursales: `POST`, `GET`, `PATCH`, `DELETE` en `/api/sucursales`
- Productos: `POST`, `GET`, `PATCH`, `DELETE` en `/api/productos`
- Consulta agregada: `GET /api/franquicias/{franquiciaId}/productos/max-stock-por-sucursal`

## 6. Despliegue local (documentación solicitada)

> Ejecutar comandos desde la raíz del proyecto en PowerShell.

### Prerrequisitos

- Java 17+
- Docker + Docker Compose
- Git

### 1) Variables de entorno

Usar `.env.example` como base para la configuración local de base de datos y variables de soporte.

### 2) Levantar dependencias locales (PostgreSQL + Redis)

```powershell
docker compose -f deployment/docker-compose.yml up -d
```

### 3) Ejecutar la API

```powershell
.\gradlew :app-service:bootRun
```

La API queda disponible en:

- `http://localhost:8080`

### 4) Probar casos de uso

Ejecutar las colecciones del archivo `api-casos-uso.http` desde IntelliJ HTTP Client o importar sus requests en tu herramienta preferida.

### 5) Apagar infraestructura local

```powershell
docker compose -f deployment/docker-compose.yml down
```

## 7. Estrategia de pruebas

Se implementó una estrategia QA automatizada por módulo y por nivel:

- Unitarias (model, usecase, entry-point y adapter) con JUnit5, Mockito y StepVerifier.
- Integración con PostgreSQL usando Testcontainers.
- E2E API reactiva con WebTestClient.
- Aceptación con Karate en `deployment/acceptanceTest`.

### Ejecución de pruebas

#### Ejecutar todo (recomendado)

```powershell
.\scripts\smoke-acceptance.ps1
```

#### Ejecutar pruebas rápidas

```powershell
.\gradlew :model:test :usecase:test :reactive-web:test :r2dbc-postgresql:test
```

#### Integración real con PostgreSQL

```powershell
.\gradlew :app-service:test --tests "*FranquiciaApiIntegrationTest"
```

#### Suite completa

```powershell
.\gradlew test
```

#### Aceptación (Karate)

```powershell
Set-Location .\deployment\acceptanceTest
.\gradlew clean test "-Dkarate.options=--tags @acceptanceTest" "-DbaseUrl=http://localhost:8080"
```

## 8. Docker

### Construir imagen

```powershell
docker build -f deployment/Dockerfile -t backend-accenture-reactive .
```

### Ejecutar contenedores de soporte

```powershell
docker compose -f deployment/docker-compose.yml up -d
```

## 9. Infraestructura cloud (Terraform + GitOps)

La solución incluye aprovisionamiento de infraestructura AWS con enfoque DevSecOps:

- Estado remoto Terraform (`S3 + KMS + DynamoDB Lock`)
- VPC por capas (subred pública para ALB, privadas para app/data)
- ECS Fargate para la API
- RDS PostgreSQL y Redis en red privada
- OIDC desde GitHub Actions para despliegue seguro sin llaves estáticas

### Flujo recomendado

1. `terraform/bootstrap`: crear backend remoto de estado.
2. `terraform/`: aplicar infraestructura principal.
3. Pipeline GitOps: `fmt`, `validate`, `tfsec`, `plan`, `apply` con aprobación en ambiente protegido.

## 10. Flujo de trabajo Git y entrega

La solución está preparada para evaluación en repositorio público, con historial de cambios y documentación de ejecución local. Se recomienda mantener estrategia de ramas por feature y PRs con validación automática.
