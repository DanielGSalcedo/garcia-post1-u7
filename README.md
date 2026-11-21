# Post-contenido — Unidad 7: Patrones Arquitectónicos I

API Spring Boot para gestionar multas de biblioteca con arquitectura en capas.

## Ejecución

Desde `multas-biblioteca-api/`, ejecutar `mvn spring-boot:run`. Requiere Java 17 y Maven 3.8+.

## Capas

`controller/` expone REST; `service/` coordina los casos de uso; `model/` contiene la entidad y reglas de dominio; `repository/` abstrae persistencia JPA sobre H2.

## Endpoints

- `GET /api/multas`
- `GET /api/multas/{id}`
- `GET /api/multas/estudiante/{estudianteId}`
- `POST /api/multas`
- `PATCH /api/multas/{id}/pagar`

Las multas calculan COP 500 por día, hasta COP 15.000. Cada estudiante puede tener como máximo tres pendientes.
