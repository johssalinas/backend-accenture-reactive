# Proyecto Base Implementando Clean Architecture

## Antes de Iniciar

Empezaremos por explicar los diferentes componentes del proyectos y partiremos de los componentes externos, continuando con los componentes core de negocio (dominio) y por último el inicio y configuración de la aplicación.

Lee el artículo [Clean Architecture — Aislando los detalles](https://medium.com/bancolombia-tech/clean-architecture-aislando-los-detalles-4f9530f35d7a)

# Arquitectura

![Clean Architecture](https://miro.medium.com/max/1400/1*ZdlHz8B0-qu9Y-QO3AXR_w.png)

## Domain

Es el módulo más interno de la arquitectura, pertenece a la capa del dominio y encapsula la lógica y reglas del negocio mediante modelos y entidades del dominio.

## Usecases

Este módulo gradle perteneciente a la capa del dominio, implementa los casos de uso del sistema, define lógica de aplicación y reacciona a las invocaciones desde el módulo de entry points, orquestando los flujos hacia el módulo de entities.

## Infrastructure

### Helpers

En el apartado de helpers tendremos utilidades generales para los Driven Adapters y Entry Points.

Estas utilidades no están arraigadas a objetos concretos, se realiza el uso de generics para modelar comportamientos
genéricos de los diferentes objetos de persistencia que puedan existir, este tipo de implementaciones se realizan
basadas en el patrón de diseño [Unit of Work y Repository](https://medium.com/@krzychukosobudzki/repository-design-pattern-bc490b256006)

Estas clases no puede existir solas y debe heredarse su compartimiento en los **Driven Adapters**

### Driven Adapters

Los driven adapter representan implementaciones externas a nuestro sistema, como lo son conexiones a servicios rest,
soap, bases de datos, lectura de archivos planos, y en concreto cualquier origen y fuente de datos con la que debamos
interactuar.

### Entry Points

Los entry points representan los puntos de entrada de la aplicación o el inicio de los flujos de negocio.

## Application

Este módulo es el más externo de la arquitectura, es el encargado de ensamblar los distintos módulos, resolver las dependencias y crear los beans de los casos de use (UseCases) de forma automática, inyectando en éstos instancias concretas de las dependencias declaradas. Además inicia la aplicación (es el único módulo del proyecto donde encontraremos la función “public static void main(String[] args)”).

**Los beans de los casos de uso se disponibilizan automaticamente gracias a un '@ComponentScan' ubicado en esta capa.**

## Estrategia de pruebas

Se incluyó una estrategia QA automatizada con pruebas por módulo y por nivel:

- Unitarias (model, usecase, entry-point y adapter) con JUnit5, Mockito y StepVerifier.
- Integración real con base de datos PostgreSQL usando Testcontainers.
- E2E del API reactivo con WebTestClient.
- Aceptación con Karate en deployment/acceptanceTest (generado con gat).

## Ejecutar pruebas

> Ejecuta los comandos desde la raíz del proyecto (`backend-accenture-reactive`) en PowerShell.

### Ejecutar todo (recomendado)

```powershell
.\scripts\smoke-acceptance.ps1
```

Este comando:
- levanta la base de datos de pruebas,
- inicia la API,
- ejecuta acceptance tests,
- y apaga el entorno al finalizar.

### Ejecutar pruebas rápidas (mock)

```powershell
.\gradlew :model:test :usecase:test :reactive-web:test :r2dbc-postgresql:test
```

### Ejecutar integración real con PostgreSQL (Testcontainers)

```powershell
.\gradlew :app-service:test --tests "*FranquiciaApiIntegrationTest"
```

### Ejecutar suite principal completa

```powershell
.\gradlew test
```

### Ejecutar aceptación (Karate)

1. Levantar API + DB localmente o por Docker.
2. Ejecutar el subproyecto:

```powershell
Set-Location .\deployment\acceptanceTest
.\gradlew clean test "-Dkarate.options=--tags @acceptanceTest" "-DbaseUrl=http://localhost:8080"
```

### Entorno de pruebas real por Docker

```powershell
docker compose --env-file .env.test -f deployment/docker-compose.test.yml up -d
```

Para apagar:

```powershell
docker compose --env-file .env.test -f deployment/docker-compose.test.yml down
```

Variables disponibles en .env.test para estandarizar ejecución local/CI.
