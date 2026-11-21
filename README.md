# Post-contenido — Unidad 7: Patrones Arquitectónicos I

Solución de referencia para el sistema de gestión de multas de biblioteca de la UFPS. Un único proyecto Spring Boot expone una API REST en capas y permite pagar multas en línea mediante dos pasarelas seleccionables por configuración.

> Antes de entregar: personaliza cualquier dato que te solicite la plataforma. El repositorio ya está publicado; agrega capturas reales de los endpoints después de ejecutarlos.

## Requisitos y ejecución

- Java 17 o superior y Maven 3.8+
- Desde `multas-biblioteca-api/`: `mvn spring-boot:run`
- Consola H2: `http://localhost:8080/h2-console` (JDBC URL `jdbc:h2:mem:multas_biblioteca_db`, usuario `sa`, contraseña vacía)
- API: `http://localhost:8080/api/multas`

Para compilar: `mvn clean package`.

## Arquitectura

```text
com.example.multas
├── controller/       # REST, validación de entrada y traducción de errores HTTP
├── service/          # Casos de uso y reglas que requieren consultar datos
├── model/            # Entidad, estado y excepciones de dominio
├── repository/       # Persistencia Spring Data JPA
├── domain/           # Puerto de pago y resultado neutral (Java puro)
│   └── port/
└── infrastructure/   # Adaptadores HTTP y configuración Spring
    ├── pago/
    └── config/
```

La Parte 1 mantiene separadas las responsabilidades de presentación, aplicación, dominio e infraestructura. En la Parte 2 se conserva esa solución para las multas y se usa un puerto hexagonal solo en el borde de pagos.

## Endpoints

| Método | Ruta | Resultado |
|---|---|---|
| GET | `/api/multas` | Lista todas las multas |
| GET | `/api/multas/{id}` | Consulta por ID (404 si no existe) |
| GET | `/api/multas/estudiante/{estudianteId}` | Lista por estudiante |
| POST | `/api/multas` | Crea multa; 201, valida campos y aplica límite de pendientes |
| PATCH | `/api/multas/{id}/pagar` | Pago en ventanilla |
| POST | `/api/multas/{id}/pagar-en-linea` | Pago con el proveedor activo |

Ejemplo de creación:

```json
{"estudianteId":"2026001","concepto":"Devolución tardía","diasAtraso":4}
```

El monto es COP 500 por día, con tope de COP 15.000. El cuarto pendiente del mismo estudiante se rechaza con 409. Datos inválidos retornan 400; pagos rechazados 402; intentar pagar de nuevo retorna 409.

## Selección y simulación de pasarelas

`app.pagos.proveedor=pagosudes` activa PagosUDES por defecto; `wompi` activa Wompi. Ambas URLs se configuran en `application.properties`. Los adaptadores esperan servicios HTTP con los contratos indicados por el enunciado (PagosUDES: `idTransaccion/estadoTransaccion`; Wompi: `reference/status` y centavos). No hay servidores simulados incluidos, así que para probar pagos en línea se deben levantar mocks compatibles en los puertos 9001 o 9002.

## Decisiones de diseño

### 1. Cálculo del monto: entidad o servicio

`Multa.calcularMonto` es una regla pura que solo depende de los días de atraso y vive junto al concepto de dominio que describe. Mantenerla en `MultaService` convertiría a la entidad en un modelo anémico y haría más fácil duplicar la fórmula desde otros casos de uso. El servicio sigue siendo responsable de orquestar la creación y guardar el resultado.

### 2. Conteo de pendientes: consulta o filtrado en memoria

`MultaService` decide si se puede crear otra multa, pero delega el conteo a `countByEstudianteIdAndEstado`. La base de datos ejecuta un `COUNT` sin cargar la lista completa. Filtrar todas las multas en memoria transferiría más filas y haría que el coste creciera con el historial del estudiante.

### 3. Elección entre capas extendidas y puerto

Elegimos la opción C, un puerto de dominio con dos adaptadores. PagosUDES y Wompi tienen contratos HTTP distintos y Wompi usa centavos; encapsular las traducciones en adaptadores mantiene esos detalles fuera del servicio. Además, el enunciado anticipa agregar o retirar proveedores, por lo que cambiar adaptadores es menos propenso a modificar el caso de uso.

La alternativa B (Strategy en `service/`) también permitiría intercambiar las implementaciones con menos estructura y sería razonable si solo se buscara el polimorfismo local. La opción A (condicional en el servicio) sería la más corta, pero acoplaría `MultaService` a los formatos y decisiones de configuración de cada proveedor. El costo de C es crear paquetes, puerto, tipo neutral y adaptadores adicionales; requiere aprender límites hexagonales. Si el piloto termina y queda una sola pasarela estable, reconsideraríamos simplificarlo a una estrategia dentro de la capa de aplicación.

### 4. Selección en arranque y contrato neutral

`@ConditionalOnProperty` crea un único `PasarelaPagoPort` según la configuración de la sede. Un mapa seleccionado en tiempo de ejecución permitiría cambiar de proveedor sin reiniciar, pero añade selección y conocimiento de claves al servicio, flexibilidad que el requisito no pide. `ResultadoPago.referenciaExterna` describe ambas referencias; nombrar el campo `idTransaccion` forzaría a Wompi a fingir ese concepto y filtraría el lenguaje de un proveedor al dominio.

### Trade-off y conclusión

El puerto protege al caso de uso de cambios en proveedores y deja la traducción HTTP en infraestructura; se paga por ello con más clases y abstracciones. En la Parte 1 se distinguieron reglas puras de dominio de reglas que requieren datos persistidos. En la Parte 2, la decisión difícil fue equilibrar la variabilidad real de los proveedores con el costo de introducir una frontera adicional. La solución adopta esa frontera únicamente en pagos, donde las diferencias de contrato son concretas.

## Evidencia y publicación

Después de ejecutar la aplicación, captura en Postman o curl los casos principales (creación, validación 400, límite 409, pago en ventanilla, pago en línea exitoso y rechazado) y agrégalos en `docs/` o al README. El repositorio público `garcia-post1-u7` ya tiene tres commits por parte. Entrega su enlace en NPLAD UFPS.

