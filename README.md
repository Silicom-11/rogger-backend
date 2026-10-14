# Rogger Express - API REST (v2)

Back-end del sistema web de gestion de encomiendas de **Rogger Express**
(ruta Lima - Huancayo), con seguridad JWT, control de roles y tablero de indicadores.

Proyecto integrador del curso **Desarrollo Web Integrado** - UTP, seccion 27524.
Docente: Mg. Ing. Jose Fernando Matos Vera.

Integrantes: Aquino Carhuas Marc Andersen · Camposano Sandoval Yefrie Antony ·
Alvarado Arirama Andrey Gianfranco.

---

## Tecnologias

| Componente | Version |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Spring Security + JWT (jjwt) | 6.2 / 0.12.5 |
| Spring Data MongoDB | incluida en el starter |
| MongoDB | 8.0 (local, administrada con MongoDB Compass) |
| SpringDoc OpenAPI (Swagger UI) | 2.5.0 |
| JUnit 5 + Mockito | incluidos en spring-boot-starter-test |

## Como ejecutar

1. Iniciar MongoDB (servicio local en el puerto 27017).
2. Desde esta carpeta:

```bash
mvn spring-boot:run
```

3. API en `http://localhost:8080` · Swagger UI en `http://localhost:8080/swagger-ui.html`

Al primer arranque se cargan las agencias, el tarifario y los usuarios de prueba.

## Usuarios de prueba

| Usuario | Contrasena | Rol | Puede |
|---|---|---|---|
| `operador.lima` | `Rogger2026` | OPERADOR | Cotizar y registrar encomiendas |
| `almacen.huancayo` | `Rogger2026` | ALMACEN | Cambiar estados y registrar entregas |
| `admin` | `Rogger2026` | ADMIN | Todo lo anterior + agencias, usuarios y tarifario |

## Seguridad

La API es *stateless*: `POST /api/v1/auth/login` entrega dos piezas.

- **Token de acceso** (JWT, 60 minutos): viaja en cada peticion como
  `Authorization: Bearer <token>`.
- **Token de refresco** (7 dias): renueva el acceso sin volver a pedir la
  contrasena. Se guarda **hasheado con SHA-256** en la coleccion `sesiones` y se
  **rota** en cada uso, de modo que un token copiado deja de servir apenas el
  usuario legitimo renueva. `POST /api/v1/auth/logout` lo revoca, y cambiar la
  contrasena revoca todas las sesiones abiertas del usuario.

**Rutas publicas** (sin token): `POST /api/v1/auth/login`,
`POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout`,
`GET /api/v1/tracking/{codigo}`, `POST /api/v1/tarifas/cotizar`,
`/swagger-ui.html` y `/actuator/health`.

### Proteccion del acceso

| Medida | Configuracion | Comportamiento |
|---|---|---|
| Bloqueo por fuerza bruta | `app.seguridad.max-intentos` (5), `app.seguridad.bloqueo-minutos` (15) | Tras 5 credenciales erroneas la cuenta se bloquea y la API responde **429** con `Retry-After` |
| Limite de solicitudes | `app.seguridad.limite-por-minuto` (60) | Por origen y sobre las rutas publicas; al superarlo responde **429** |
| HTTPS | perfil `https` | TLS 1.2 / 1.3 en el puerto 8443, con HSTS |

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=https      # https://localhost:8443
```

### Alcance de los datos

Toda consulta se resuelve dentro del alcance del usuario:

- **ADMIN** consulta toda la red.
- **OPERADOR y ALMACEN** solo ven las encomiendas cuyo origen o destino es su
  agencia. Una encomienda ajena responde **404**, igual que si no existiera, para
  no revelar la operacion de otras sedes. El filtro se aplica antes que los
  parametros de busqueda, asi que no puede esquivarse desde el cliente.
- Los **importes acumulados** (`ingresosHoy`, `ingresosMes`, `ticketPromedio`) solo
  se calculan y se devuelven para ADMIN; para los demas roles esos campos **no
  aparecen** en la respuesta.

## Avisos al cliente

`ProveedorNotificaciones` es la interfaz de salida; la implementacion se elige con
`app.notificaciones.proveedor`:

| Valor | Implementacion | Efecto |
|---|---|---|
| `registro` (por defecto) | `ProveedorRegistro` | Deja el mensaje en la coleccion `notificaciones` y en el log. `entregaReal = false`: el cliente **no** recibio nada |
| `http` | `ProveedorHttp` | `POST` a la pasarela contratada con `{to, from, channel, message}` y `Authorization: Bearer`. Toda respuesta distinta de 2xx deja el aviso en `ERROR` |

```properties
app.notificaciones.proveedor=http
app.notificaciones.url=https://api.mi-pasarela.com/v1/messages
app.notificaciones.token=<token de la cuenta>
app.notificaciones.canal=SMS
app.notificaciones.max-intentos=4
app.notificaciones.reintento-ms=180000
```

Cada aviso guarda canal, destino, estado, numero de intentos, referencia del
proveedor y el motivo del ultimo fallo. Los que quedan en `ERROR` se reintentan
solos cada 3 minutos hasta agotar los intentos, y pueden reenviarse a mano.

## Endpoints

| Metodo | Endpoint | Rol requerido |
|---|---|---|
| POST | `/api/v1/auth/login` | publico |
| POST | `/api/v1/auth/refresh` | publico (requiere token de refresco valido) |
| POST | `/api/v1/auth/logout` | publico (revoca el refresco enviado) |
| GET | `/api/v1/auth/me` | autenticado |
| POST | `/api/v1/auth/cambiar-clave` | autenticado |
| POST | `/api/v1/tarifas/cotizar` | publico |
| GET | `/api/v1/tracking/{codigo}?verificacion=` | publico (exige el codigo del comprobante) |
| POST | `/api/v1/envios` | OPERADOR, ADMIN |
| GET | `/api/v1/envios` (paginado + filtros) | autenticado |
| GET | `/api/v1/envios/{codigo}` | autenticado |
| GET | `/api/v1/envios/{codigo}/siguientes-estados` | autenticado |
| PATCH | `/api/v1/envios/{codigo}/estado` | ALMACEN, ADMIN |
| PATCH | `/api/v1/envios/estado-lote` | ALMACEN, ADMIN |
| GET | `/api/v1/envios/para-entrega?dni=` | ALMACEN, ADMIN |
| POST | `/api/v1/envios/{codigo}/entrega` | ALMACEN, ADMIN |
| POST | `/api/v1/envios/{codigo}/clave-recojo` | OPERADOR, ALMACEN, ADMIN |
| POST | `/api/v1/envios/{codigo}/anulacion` | OPERADOR, ADMIN |
| GET | `/api/v1/reportes/resumen` | autenticado (importes solo para ADMIN) |
| GET | `/api/v1/notificaciones` | autenticado (acotado a su agencia) |
| POST | `/api/v1/notificaciones/{id}/reintento` | ALMACEN, ADMIN |
| GET | `/api/v1/agencias` | autenticado |
| POST | `/api/v1/agencias` | ADMIN |
| GET/POST | `/api/v1/usuarios` | ADMIN |
| POST | `/api/v1/usuarios/{id}/reset-clave` | ADMIN |
| GET/POST | `/api/v1/tarifario` | ADMIN |

### Filtros de la busqueda paginada

```
GET /api/v1/envios?estado=EN_TRANSITO&agenciaDestinoId=AG-HYO-CEN
                  &desde=2026-09-01&hasta=2026-09-30
                  &texto=70125489&pagina=0&tamanio=10
```

## Regla de negocio principal

```
peso volumetrico = (largo x ancho x alto) / 6000
peso facturable  = max(peso real, peso volumetrico)
total            = monto base + (peso facturable x precio por kg) + recargo
```

Ejemplo: 12.5 kg, 40 x 30 x 25 cm -> volumetrico 5.00 kg -> facturable 12.50 kg
-> total **S/ 45.50**.

## Estados de la encomienda

`REGISTRADO -> EN_ALMACEN_ORIGEN -> EN_TRANSITO -> EN_ALMACEN_DESTINO ->
LISTO_PARA_RECOJO -> ENTREGADO`, con las ramas `INCIDENCIA`, `DEVUELTO` y `ANULADO`.
Una transicion no permitida devuelve **409 Conflict**.

## Reglas de proteccion de la operacion

| Regla | Comportamiento |
|---|---|
| Doble registro | Una admision identica (mismo remitente, destinatario, peso y ruta) dentro de 2 minutos devuelve **409** con el codigo ya emitido |
| Limites de la carga | Peso maximo 1000 kg; cada dimension maximo 300 cm |
| Tamano de pagina | Maximo 100 registros por peticion |
| Anulacion | Solo desde `REGISTRADO` o `EN_ALMACEN_ORIGEN`, solo OPERADOR/ADMIN y con motivo (minimo 5 caracteres). El registro **no se elimina**: queda como `ANULADO` con el motivo en el historial |
| Cambio en lote | Cada codigo se procesa por separado; el resultado detalla actualizados y fallidos |
| Contrasenas | Los usuarios creados por un administrador y los que tuvieron su clave restablecida deben definir una propia antes de operar |
| Usuario desactivado | Su token deja de servir en la siguiente peticion (**401**), sin esperar a que expire |
| Contacto del destinatario | Obligatorio: sin telefono no se admite la encomienda (**400**), porque el sistema no podria avisar la llegada |
| Consulta del cliente | Exige el codigo de rastreo y el codigo de verificacion del comprobante. Si alguno no coincide se responde **404**, igual que si la encomienda no existiera |
| Clave de recojo | Se define en la admision (la dicta el cliente o la genera el sistema), se guarda con BCrypt y se pide en la entrega. Una clave equivocada devuelve **409** |
| Personas autorizadas | Hasta dos por encomienda. Un documento que no es del destinatario ni de un autorizado devuelve **409** |
| Entrega sin clave | Permitida indicando un motivo de al menos 5 caracteres, que queda en el historial y en la constancia de entrega |
| Agencia ajena | Consultar o mover una encomienda de otra sede devuelve **404** para todo rol distinto de ADMIN |
| Importes acumulados | Solo se calculan para ADMIN; para los demas roles no viajan en la respuesta |

## Pruebas

```bash
mvn test
```

Las pruebas no requieren MongoDB: los repositorios se simulan con Mockito.
