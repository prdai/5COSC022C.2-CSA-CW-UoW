# Smart Campus Sensor & Room Management API

A JAX-RS RESTful service developed for the University of Westminster "Smart Campus" initiative. The API manages three resources — rooms, sensors, and sensor readings — and enforces the referential-integrity and state-based rules described in the coursework specification.

## Module context

| Field | Value |
|---|---|
| Module | 5COSC022W — Client-Server Architectures |
| Student ID | w2120198 |
| Weighting | 60% of final mark |
| Target mark band | Excellent (70%+) on every rubric row |
| Submission | Public GitHub repository, PDF report (transcribed from this file), 10-minute video demonstration |

This README serves two purposes. The top half is the GitHub-facing overview with build instructions and sample curl commands. Section 7 is the **Conceptual Report** required by the specification, which states: "the report must be organised and written in the README.md file on GitHub. The report must only include the answers to the questions in each part."

## API overview

The service exposes four resource groups under the versioned base path `/api/v1`:

| Resource | URI prefix | Implementation |
|---|---|---|
| Discovery | `/api/v1` | `DiscoveryResource` |
| Rooms | `/api/v1/rooms` | `SensorRoom` |
| Sensors | `/api/v1/sensors` | `SensorResource` |
| Readings (sub-resource) | `/api/v1/sensors/{sensorId}/readings` | `SensorReadingResource` |

Design choices worth flagging up front:

- **In-memory storage only.** The specification forbids a database. Each DAO (`dao/RoomDAO.java`, `dao/SensorDAO.java`) holds a `static final List<T>` wrapped in `Collections.synchronizedList`, guarded by `synchronized` blocks on every read and write. `dao/SensorReadingDAO.java` keys a plain `HashMap` by `sensorId` and guards it with `synchronized (HISTORY)` blocks, so all three DAOs share the same locking idiom.
- **Three-tier separation.** Resources (`resource/*.java`) do nothing but JAX-RS mapping and `Response` construction. Business rules and multi-DAO orchestration live in the service layer (`service/RoomService.java`, `service/SensorService.java`, `service/SensorReadingService.java`). The service layer is where the three domain rules the specification lists are enforced: a sensor cannot be registered for a non-existent room, a room cannot be deleted while any sensor still references it, and a sensor under maintenance refuses new readings. Each violation maps to a distinct HTTP status code — 422, 409, 403 respectively.
- **JSON everywhere.** Every resource method either produces or consumes `application/json`; each of the three domain exception mappers returns a structured `ErrorMessage` body (`errorMessage` + `errorCode`) rather than the servlet container's default error page.
- **Stack traces never leave the process.** `GenericExceptionMapper` is the global safety net. `WebApplicationException` subtypes (400 / 405 / 406 / 415 raised by Jersey's content-negotiation and dispatch machinery) pass through with their intended status, Jackson `JsonProcessingException` collapses to 400 Bad Request, and every other `Throwable` is logged server-side through `java.util.logging` and returned as a generic 500 body. Every error response — mapped or inline 404 — uses the uniform `{errorMessage, errorCode}` envelope.

## Build and run

**Prerequisites**

- JDK 8 or later
- Maven 3.6+
- A Servlet 3.1+ container. The project was developed against GlassFish 5 (the container NetBeans ships by default); Tomcat 9 and Payara 5 also work because Jersey 2.32 targets the `javax.ws.rs` namespace.

**Build**

```bash
cd smart-campus-sensor-and-room-management-api
mvn clean package
# produces: target/smart-campus-sensor-and-room-management-api-1.0-SNAPSHOT.war
```

**Deploy**

Either open the project in NetBeans and choose **Run Project** (which deploys to the bundled GlassFish), or copy the `.war` into the container's `webapps/` directory. The default context root matches the artefact name, so the full base URL is:

```
http://localhost:8080/smart-campus-sensor-and-room-management-api/api/v1
```

A verification smoke-test after deployment:

```bash
curl -s http://localhost:8080/smart-campus-sensor-and-room-management-api/api/v1
```

A 200 response containing the `resources` map confirms that the JAX-RS application has bootstrapped and classpath scanning has picked up the resource classes.

**Fresh-start behaviour**

The three in-memory stores begin empty on every deployment. The curl sequence below is ordered as a self-contained demonstration: steps 2–5 create the rooms and sensors that the later steps read, filter, and operate on.

## Sample curl commands

The specification requires at least five. The twelve calls below cover discovery, happy-path CRUD, filtering, the sub-resource locator, and each of the three domain-specific error paths. They are designed to run end-to-end on a freshly-deployed WAR.

```bash
BASE="http://localhost:8080/smart-campus-sensor-and-room-management-api/api/v1"
```

**1. Discovery — API metadata and resource map (HATEOAS entry point)**

```bash
curl -s "$BASE"
```

**2. Create a room (expect 201 Created + `Location` header)**

```bash
curl -s -i -X POST "$BASE/rooms" \
     -H "Content-Type: application/json" \
     -d '{"id":"LIB-301","name":"Library Quiet Study","capacity":40}'
```

**3. Create a second room**

```bash
curl -s -i -X POST "$BASE/rooms" \
     -H "Content-Type: application/json" \
     -d '{"id":"LEC-101","name":"Lecture Hall A","capacity":120}'
```

**4. Register a sensor inside the first room**

```bash
curl -s -i -X POST "$BASE/sensors" \
     -H "Content-Type: application/json" \
     -d '{"id":"TEMP-001","type":"Temperature","status":"ACTIVE","currentValue":21.3,"roomId":"LIB-301"}'
```

**5. Register a sensor that is already under `MAINTENANCE` (for the 403 path below)**

```bash
curl -s -i -X POST "$BASE/sensors" \
     -H "Content-Type: application/json" \
     -d '{"id":"OCC-001","type":"Occupancy","status":"MAINTENANCE","currentValue":0.0,"roomId":"LEC-101"}'
```

**6. List all rooms**

```bash
curl -s "$BASE/rooms"
```

**7. Fetch a single room by id**

```bash
curl -s "$BASE/rooms/LIB-301"
```

**8. Filter sensors by type with a query parameter**

```bash
curl -s "$BASE/sensors?type=Temperature"
```

**9. Append a reading to a sensor (side effect: parent `currentValue` updates)**

```bash
curl -s -i -X POST "$BASE/sensors/TEMP-001/readings" \
     -H "Content-Type: application/json" \
     -d '{"value":22.8,"timestamp":1713352800000}'

# Re-read the readings history to confirm the reading was recorded
curl -s "$BASE/sensors/TEMP-001/readings"
```

**10. Error path — delete a room that still has sensors (expect 409 Conflict)**

```bash
curl -s -i -X DELETE "$BASE/rooms/LIB-301"
```

**11. Error path — register a sensor for a non-existent room (expect 422 Unprocessable Entity)**

```bash
curl -s -i -X POST "$BASE/sensors" \
     -H "Content-Type: application/json" \
     -d '{"id":"HEAT-999","type":"Temperature","status":"ACTIVE","currentValue":19.0,"roomId":"NOT-A-ROOM"}'
```

**12. Error path — post a reading to a sensor in MAINTENANCE (expect 403 Forbidden)**

```bash
curl -s -i -X POST "$BASE/sensors/OCC-001/readings" \
     -H "Content-Type: application/json" \
     -d '{"value":5.0}'
```

## Project structure

```
smart-campus-sensor-and-room-management-api/
├── pom.xml                                 Maven build, packaging = war
└── src/main/
    ├── java/com/w2120198/csa/cw/
    │   ├── SmartCampusApplication.java     @ApplicationPath("/api/v1")
    │   ├── resource/
    │   │   ├── DiscoveryResource.java      GET /api/v1
    │   │   ├── SensorRoom.java             /api/v1/rooms
    │   │   ├── SensorResource.java         /api/v1/sensors
    │   │   └── SensorReadingResource.java  sub-resource, constructed by locator
    │   ├── service/
    │   │   ├── RoomService.java            orphan-room check (409)
    │   │   ├── SensorService.java          roomId referential integrity (422)
    │   │   └── SensorReadingService.java   MAINTENANCE guard (403) + currentValue side-effect
    │   ├── dao/
    │   │   ├── RoomDAO.java                static synchronized list
    │   │   ├── SensorDAO.java              static synchronized list
    │   │   └── SensorReadingDAO.java       HashMap<sensorId, List> under synchronized(HISTORY)
    │   ├── model/
    │   │   ├── ErrorMessage.java           JSON error envelope (errorMessage + errorCode)
    │   │   ├── Room.java
    │   │   ├── Sensor.java                 STATUS_MAINTENANCE constant
    │   │   └── SensorReading.java
    │   └── exception/
    │       ├── RoomNotEmptyException.java + Mapper       409
    │       ├── LinkedResourceNotFoundException + Mapper  422
    │       ├── SensorUnavailableException.java + Mapper  403
    │       └── GenericExceptionMapper.java               500 catch-all
    └── webapp/WEB-INF/                     servlet descriptor (empty — Jersey scans)
```

404 responses for missing rooms or sensors are returned inline from the resource methods rather than through a dedicated exception mapper. The four mapper-backed codes (409, 422, 403, 500) are the exact set the specification Part 5 enumerates.

---

## 7. Conceptual Report

The questions below are transcribed verbatim from the coursework specification. Each answer cites the file in which the relevant implementation lives. File paths are relative to the project root (`smart-campus-sensor-and-room-management-api/`).

### 7.1 Part 1.1 — JAX-RS resource lifecycle and in-memory thread safety

**Question.** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.

By default, Jersey creates a fresh instance of each resource class for every incoming request. The JAX-RS specification calls this the "per-request" lifecycle, and it is the behaviour used throughout this project because no resource class is annotated with `@Singleton` and no explicit binding is registered in `SmartCampusApplication.java`. The practical consequence is that instance fields on a resource class are thread-confined: `SensorRoom`, `SensorResource`, and `SensorReadingResource` each hold a service-layer collaborator as an instance field, and those fields are only visible to the thread handling the request that triggered the resource's construction.

The data that genuinely crosses request boundaries lives in the DAO classes. `dao/RoomDAO.java` and `dao/SensorDAO.java` each declare a `private static final List<T>` initialised through `Collections.synchronizedList(new ArrayList<>())`. Every read and write method on these DAOs acquires a monitor on the backing list via `synchronized (ROOMS) { ... }` or `synchronized (SENSORS) { ... }`. Two common failure modes are ruled out by that pattern: `ConcurrentModificationException` from a reader traversing the list while a writer removes an element, and lost updates from two writers racing on `List.set`. The readings store follows the same idiom: `dao/SensorReadingDAO.java` holds a `static final HashMap<String, List<SensorReading>>` keyed by `sensorId` and guards every read and write with a `synchronized (HISTORY) { ... }` block. The lock covers the lazy create-on-first-reading path so two threads posting the first reading for a new sensor converge on the same list, and all three DAOs now share the same locking model, which makes the concurrency story easier to describe in a viva.

The specification's compound rule "delete a room only if it has no sensors" is a check-then-act sequence that spans both stores, and primitive list-level locking is not enough to keep it safe under concurrent writes. Two race windows would otherwise violate the room-sensor link invariant (a room's `sensorIds` list is empty if and only if no `Sensor` references the room): `RoomService.delete` could observe `sensorIds.isEmpty()` and remove the room between that check and a concurrent `SensorService.create` that was about to append a sensor id; `SensorService.create` could confirm the parent exists and be interrupted by a concurrent `RoomService.delete` that saw the same empty `sensorIds` and finished before the create appended the new id. Either race orphans a sensor.

`dao/RoomDAO.java` declares a `public static final Object LINK_LOCK` — a dedicated shared monitor — and both `service/RoomService.delete` and `service/SensorService.create` acquire it at the top of their critical section. `delete` holds the lock across the empty-check and the removal; `create` holds it across the parent lookup, the sensor insert, and the `sensorIds` append. Serialising only those two operations keeps the invariant intact. Ordinary reads stay concurrent, and sensor-reading appends take independent locks on the readings store so the POST-reading path is unaffected.

### 7.2 Part 1.2 — Hypermedia and HATEOAS

**Question.** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

HATEOAS — Hypermedia As The Engine Of Application State — sits at the top of Richardson's Maturity Model because it completes the separation between the client's model of the service and the server's URI scheme. When a response carries the links a client needs in order to make its next request, the client no longer has to hard-code those URIs from documentation. The server is free to move endpoints, version them, or rename path segments, so long as the hypermedia in the responses stays internally consistent.

`resource/DiscoveryResource.java` is the practical entry point for this pattern in the present API. `GET /api/v1` returns a JSON object containing an `administrator` block, a semantic `version` string, and a `resources` map that lists every top-level collection together with its absolute URI (`"rooms": "http://.../api/v1/rooms"` and `"sensors": ".../api/v1/sensors"`). A well-behaved client bookmarks only the discovery URL and follows the `resources` map from there. If the university were to re-deploy the service behind a new context root or move to `/api/v2`, the discovery response would change and the client would adapt without a code release.

The benefit over static documentation is threefold. First, the documentation drifts: a PDF reference written at v1.0 is usually out of date by v1.2, while a live discovery response is authoritative by construction. Second, conditional links communicate domain state. A sensor in `ACTIVE` state could ship with a `post-reading` link; the same sensor in `MAINTENANCE` could ship without it, so that a hypermedia-literate client disables the "record a reading" button in its UI without round-tripping to discover the 403. A room that still holds sensors could similarly publish a `"sensors"` link but omit a `"delete"` link. Third, test tooling and API explorers such as Postman's "Link" follower consume hypermedia directly, reducing the setup cost of new clients.

Standardised hypermedia media types lift hand-rolled JSON into a vocabulary a generic client library can traverse. HAL (`application/hal+json`, https://stateless.group/hal_specification.html) represents links under `_links` and embedded children under `_embedded`. JSON:API (`application/vnd.api+json`, https://jsonapi.org) specifies a richer `relationships` structure with included-resource inlining. Richardson's Maturity Model (https://martinfowler.com/articles/richardsonMaturityModel.html) frames the design levels from the client's perspective and is the canonical reference for why Level 3 matters. The richer the links in a response, the closer the API moves to being self-describing.

### 7.3 Part 2.1 — Returning IDs versus full objects in list endpoints

**Question.** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.

The trade-off is between single-round-trip responses that may contain unused fields, and thin identifier lists that force a second round trip for every detail view.

Returning ID-only collections keeps the `GET /rooms` payload minimal. For a campus with several thousand rooms, a response of the form `["LIB-301", "LEC-101", ...]` is an order of magnitude smaller than the corresponding full-object list, which is attractive for mobile clients on cellular links and for dashboards that only need to populate a drop-down. The cost is the N+1 network problem: a user interface that then wants to render each room's name or capacity must issue one `GET /rooms/{id}` per identifier. Each extra request carries the TCP/TLS handshake amortisation, the servlet dispatch, and a fresh JSON serialisation, so the aggregate cost can easily exceed the original savings.

Returning full objects, the approach taken by `SensorRoom.getAllRooms()` in `resource/SensorRoom.java`, solves the N+1 problem at the cost of a larger single response. The decision is defensible here because a `Room` is small (an identifier, a display name, an integer capacity, and a short list of sensor ids) and the campus inventory is bounded. The marginal payload size is predictable even in the worst case.

Production APIs that need both shapes typically expose one of three compromises, all formalised by the major hypermedia media types. JSON:API (https://jsonapi.org) and HAL (https://stateless.group/hal_specification.html) both support **sparse fieldsets** (`GET /rooms?fields[room]=id,name` — the response contains only the requested fields) and **linked references** (a list entry carries the child's id alongside a follow-up link, letting the client choose when to hydrate). Cursor-based or page-based **pagination** (`GET /rooms?limit=50&after=LIB-301`) bounds payload size at the collection layer. **Projection DTOs** — a dedicated `RoomSummary` record for the list endpoint and a `RoomDetail` record for the single-fetch endpoint — give the server a stable contract without depending on a client-driven query parameter. GraphQL solves the same problem at a different layer, letting the client declare the projection in the request body rather than the URL.

The Smart Campus API's choice is a *hybrid*. `GET /rooms` returns full `Room` objects, but the embedded `sensorIds` field is ID-only — not the sensor records themselves. That avoids duplicating the `/sensors` projection inside every room response, keeps cache invalidation per-resource, and leaves sensors autonomous as a sibling collection. At campus scale (hundreds of rooms, not millions) the payload stays small and pagination is unnecessary; the first evolution step if inventory grew would be page-based pagination, and the second would be HAL `_embedded` blocks so a `?expand=sensors` parameter could inline sensor objects when a client asked for them. The present API does not implement sparse fieldsets or projection DTOs because the specification does not require them, but the hybrid shape already chosen for `Room.sensorIds` is a deliberate bandwidth-for-round-trip compromise rather than a default.

### 7.4 Part 2.2 — DELETE idempotency

**Question.** Is the `DELETE` operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same `DELETE` request for a room multiple times.

Yes — and the crucial distinction is that idempotency is defined on the **server state** that results from the request, not on the response the client observes. RFC 7231 §4.2.2 puts it precisely: a method is idempotent if the intended effect of N identical requests (for N ≥ 1) is the same as the effect of a single request.

Tracing the three cases against `SensorRoom.deleteRoom` in `resource/SensorRoom.java`, which delegates to `service/RoomService.java`:

1. **First call on a room with no sensors.** `roomService.delete(roomId)` looks up the entity, finds the `sensorIds` collection empty, removes the room from the in-memory list, and returns `true`. The resource converts that into `204 No Content`. The server state after this call contains no such room.
2. **Second identical call.** `roomService.delete(roomId)` fails its lookup — `roomDAO.getById(roomId)` now returns `null` — and the service returns `false`. The resource maps the `false` return value to an inline `404 Not Found` response with an `ErrorMessage` body. The server state does not change: the room is still absent.
3. **Third, fourth, ... call.** Exactly the same as call 2. The 404 body is deterministic and the store is untouched.

The status codes differ between call 1 and calls 2+, but the property required by idempotency is satisfied: after any non-zero number of identical `DELETE /rooms/{id}` requests, the room is absent. A client that retries a lost acknowledgement therefore converges on the same outcome as one that succeeded on the first attempt, which is exactly the network-resilience property the REST community attributes to idempotent verbs.

The 409 Conflict case ("room still has sensors") does not threaten this guarantee. A 409 response leaves the room exactly where it was, so the server state is unchanged on every repetition; the response is also deterministic until some other request removes the dependent sensors, at which point the next `DELETE` will succeed and subsequent ones will revert to the 404 pattern above. The operation therefore stays idempotent in the presence of the business rule.

### 7.5 Part 3.1 — `@Consumes` mismatch consequences

**Question.** We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as `text/plain` or `application/xml`. How does JAX-RS handle this mismatch?

`@Consumes` is the server half of content negotiation: it advertises the media types the resource method is prepared to deserialise. When a request arrives, Jersey's runtime inspects the `Content-Type` header before it selects a method to invoke. If no resource method on the matched path declares a `@Consumes` value compatible with the client's `Content-Type`, Jersey never constructs the resource, never calls the method, and throws `javax.ws.rs.NotSupportedException` — a `WebApplicationException` subtype — which the runtime translates into **HTTP 415 Unsupported Media Type**.

In this project the 415 is routed through `exception/GenericExceptionMapper`, which preserves the WebApplicationException's intended status and wraps the error in the standard JSON envelope, so a client sees:

```json
{
  "errorMessage": "HTTP 415 Unsupported Media Type",
  "errorCode": 415
}
```

Two consequences follow for the present API. First, the referential-integrity check inside `service/SensorService.create` — the `roomDAO.getById` lookup that raises `LinkedResourceNotFoundException` when the declared `roomId` does not exist — is only reached when the request is already known to be JSON. A client that sends `application/xml` or `text/plain` is rejected at the dispatch stage, which keeps the service method body free of format-handling branches and means a non-JSON payload can never silently half-parse. Second, the 415 is a clear client contract: retrying the same payload will never succeed, only re-encoding it as JSON will. That is categorically different from a 500, which a client might reasonably retry assuming a transient server fault.

`@Consumes` and its sibling `@Produces` both participate in this negotiation but in opposite directions. `@Produces` is matched against the request's `Accept` header and a mismatch yields **HTTP 406 Not Acceptable** instead. The two together define the server's contract for a resource method: `@Consumes(APPLICATION_JSON) @Produces(APPLICATION_JSON)` reads as "I accept JSON and I respond with JSON". A missing `@Consumes` annotation would make the method accept any `Content-Type`, which is usually a bug waiting to happen because Jackson would then be asked to parse payloads it was never designed to handle. Declaring the annotation explicitly, as every POST in `SensorRoom`, `SensorResource`, and `SensorReadingResource` does, converts a class of malformed requests into a well-defined 415 response before any application code runs.

### 7.6 Part 3.2 — `@QueryParam` versus `@PathParam` for filtering

**Question.** You implemented this filtering using `@QueryParam`. Contrast this with an alternative design where the type is part of the URL path (e.g., `/api/v1/sensors/type/CO2`). Why is the query parameter approach generally considered superior for filtering and searching collections?

REST draws a sharp line between identifying a resource and selecting a projection of one. The path identifies, the query refines. `/api/v1/sensors` identifies the whole sensor collection; `/api/v1/sensors?type=CO2` refines the representation the server returns to a filtered subset of that same collection. The URI is unchanged, so cache keys, link relations, and the collection's identity all stay stable regardless of how many filters the client applies.

Encoding the filter in the path, as in the hypothetical `/api/v1/sensors/type/CO2`, fabricates a resource hierarchy that does not exist in the domain. It implies that `type` is a container with `CO2` as a member, which invites a proliferation of virtual paths: `/sensors/status/ACTIVE`, `/sensors/room/LIB-301`, and then the combinatorial explosion of `/sensors/type/CO2/status/ACTIVE/room/LIB-301`. Each of those paths needs its own route, its own test, and its own place in the discovery document. Query parameters compose naturally in a single method signature. `SensorResource.getSensors` in `resource/SensorResource.java` already demonstrates the pattern with `@QueryParam("type") String type`, and adding a status filter is a one-line addition of a second `@QueryParam`, not a new endpoint.

Query parameters also keep the "no filter" case free. Omit the `type` parameter and the endpoint returns the entire collection, which is the natural default behaviour. A path-segment filter cannot degrade to "no filter" without a special-case path such as `/sensors/type/all`, which is both ugly and collides with the normal case. Finally, query strings are exactly what HTTP clients, caches, and logging pipelines expect for search and filter operations: tools such as Apache access logs, CloudFront caches, and Postman parameter panels all treat query parameters as first-class, whereas filters smuggled into the path are opaque to them.

The principle generalises: use path segments for anything that has a genuine "resource" character (a particular sensor identified by its id) and query parameters for anything that narrows, sorts, or paginates a collection. Mixing the two makes the URI schema harder to reason about without any expressive gain.

### 7.7 Part 4.1 — Benefits of the sub-resource locator pattern

**Question.** Discuss the architectural benefits of the Sub-Resource Locator pattern. How does delegating logic to separate classes help manage complexity in large APIs compared to defining every nested path (e.g., `sensors/{id}/readings/{rid}`) in one massive controller class?

A sub-resource locator is a method that is annotated with `@Path` but **not** with an HTTP method annotation. It returns an object whose own `@Path`-annotated methods handle the remainder of the URI. In the present API, `SensorResource.readings(@PathParam("sensorId") String sensorId)` in `resource/SensorResource.java` fulfils that role: JAX-RS picks it up when a request arrives for `/sensors/{sensorId}/readings`, Jersey invokes the locator, and the returned `SensorReadingResource` instance takes over dispatch for the remaining path segments.

The first benefit is separation of concerns. `SensorResource` is responsible for sensors; `SensorReadingResource` is responsible for the readings that belong to one sensor. Neither class contains logic from the other's domain, and the split matches the natural join in the data model. A single mega-controller with `@Path("/sensors/{id}/readings")` sprinkled over half its methods quickly becomes the kind of 800-line file the coding-style rules in this project explicitly flag against.

The second benefit is scoped parent context. The locator receives the `sensorId` path parameter and passes only that identifier to the sub-resource's constructor: `new SensorReadingResource(sensorId)`. Each method on the sub-resource resolves the parent through `SensorReadingService` at the moment it needs it, and the service converts a missing parent into an `Optional.empty()` that the resource method renders as an inline `404 Response`. Holding the identifier rather than a captured `Sensor` object keeps the sub-resource immune to stale snapshots: if another thread mutates the parent between the locator firing and the sub-resource method running, the method sees the fresh state. The MAINTENANCE guard, the UUID assignment, and the `currentValue` side-effect all live inside `SensorReadingService.record`, so the resource body is a thin dispatcher rather than a business-rule container.

The third benefit is recursion. Nesting scales. A reading could, in a larger system, acquire its own annotations sub-resource (`/sensors/{id}/readings/{rid}/annotations`) simply by adding another locator inside `SensorReadingResource`. The top-level class neither grows nor becomes entangled in the new hierarchy. The pattern is also friendlier to testing: `SensorReadingResource` is a plain Java class with a straightforward constructor, so it can be unit-tested by instantiation without bootstrapping the JAX-RS runtime.

### 7.8 Part 5.1 — Why 422 beats 404 for a broken reference in a valid payload

**Question.** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

The three candidate statuses say very different things about the failure. HTTP 404 Not Found signals that the URI in the request line does not identify a resource. HTTP 400 Bad Request signals that the server cannot parse or syntactically validate the request — malformed JSON, missing required fields, the wrong type in a field. HTTP 422 Unprocessable Entity, introduced by WebDAV in RFC 4918 §11.2 and widely adopted across REST APIs, signals that the request is syntactically well-formed but semantically invalid: the server understands every byte of it but cannot act on it because a domain-level constraint fails.

`POST /api/v1/sensors` with a payload such as `{"id":"HEAT-999","roomId":"NOT-A-ROOM",...}` falls squarely into the third category. The target URI `/sensors` **does** exist, so 404 would misrepresent the problem (the resource the client is addressing is the sensors collection, and the collection is present and willing to accept registrations). The payload **is** valid JSON with every required field of the correct type, so 400 would also misrepresent it. What fails is referential integrity between the payload and existing server state: the room the client is pointing at was never created.

The current implementation expresses this by throwing `LinkedResourceNotFoundException` from `SensorResource.createSensor` and letting `LinkedResourceNotFoundExceptionMapper` render it as 422 with a JSON `ErrorMessage` body containing the offending `roomId`. That response tells the client three things simultaneously: the request was understood, re-sending the same bytes will not change the outcome, and the fix is to create the referenced room first (or correct the id). A 404 response would ambiguously suggest that the sensors endpoint itself has disappeared, and a 400 would suggest a typo or missing field the client cannot actually find. 422 collapses the ambiguity.

### 7.9 Part 5.2 — Cybersecurity risks of leaked stack traces

**Question.** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

An unfiltered stack trace is a reconnaissance gift. Four distinct classes of information leak out of it, and each one enables a different stage of an attack.

**Internal paths and package structure.** Every frame in the trace carries a fully-qualified class name. A response that leaks `at com.w2120198.csa.cw.service.RoomService.delete(RoomService.java:30)` simultaneously discloses the student identifier used as the root package, the two-letter module code embedded in it, and the project's three-tier layer split between `resource`, `service`, and `dao`. An attacker now knows where business logic lives without decompiling anything.

**Library versions for CVE targeting.** Frames in stack traces frequently reference third-party classes — `org.glassfish.jersey.server.ServerRuntime$1.run`, `com.fasterxml.jackson.databind.ObjectMapper.readValue`. A manifest read or a casual cross-reference with `pom.xml` pins those libraries to specific versions. The National Vulnerability Database can then be queried for CVEs affecting Jersey 2.32 or the corresponding Jackson line; the attacker walks away with a list of known weaknesses already present in the deployment.

**Logic flaws.** The line number and exception class together hint at the internal failure. A `NullPointerException at SensorRoom.deleteRoom:30` publicly admits that a particular request path hits a null dereference under some condition, and the exception type narrows the guess about which variable. An attacker can then craft payloads that deliberately reach that branch, either to exhaust the service with 500s or to probe for a crash that escapes the `ExceptionMapper` net.

**Server and runtime configuration.** Real-world traces have been known to include filesystem paths, operating user names, container flavours and versions, JVM flags, and, in genuinely bad cases, values of environment variables or JDBC URLs that were being logged at the moment of failure. None of this is information a legitimate API consumer needs.

**Personal data embedded in the trace itself (GDPR / privacy angle).** Exception messages regularly carry field values that triggered the failure — a user's email on a reservation flow, the bytes of a malformed payload in a Jackson `JsonParseException`, a room identifier that a staff-card scan fed in, a timestamp that localises an individual at a campus location. Surfacing those values in the HTTP response body, or indexing them in an external log aggregator whose data-processing contract was never written to cover personal data, constitutes an unintended processing event under GDPR Article 5(1)(f) ("integrity and confidentiality"). If the leaked fields can be linked to an identifiable person — a card number, a booking email, an occupancy record tagged with a session identifier — the organisation may also have a 72-hour notification duty under Article 33. "Minor" debug leaks through error responses are a well-documented root cause of formally reported data incidents, and a Smart Campus API that correlates room access with individual staff or students is exactly the sort of deployment in which that risk is real.

The remedy in this project is `exception/GenericExceptionMapper.java`. It declares `ExceptionMapper<Throwable>` as the final safety net for anything no more specific mapper has claimed. `WebApplicationException` subtypes (400 / 405 / 406 / 415) pass through with their intended HTTP status; Jackson `JsonProcessingException` becomes a 400 Bad Request; every other `Throwable` is logged server-side through `java.util.logging` and returned as a generic 500 body. In all three branches the response body is the fixed `ErrorMessage` envelope with a canonical message (the HTTP reason phrase for preserved WAE statuses, a short "request body was not valid JSON" for parse failures, or "the server encountered an unexpected error" for everything else) — nothing about the internal path, the library, the line, the JVM, or the payload that triggered the failure. The failure stays inside the process; the client sees only what is safe to share.

---

## References

- Coursework specification and rubric: `context/cw/CW Complete Reference.md`, `context/cw/5COSC022W_Coursework Specification(1).pdf`, `context/cw/CSA_2026_Coursework_Rubric_Final.xlsx` (workspace-private, not tracked in this repository).
- Jersey 2.32 user guide — https://eclipse-ee4j.github.io/jersey.github.io/documentation/2.32/user-guide.html (chapter 3 JAX-RS Application & Sub-Resources, chapter 9 Filters, Interceptors and ExceptionMappers).
- JAX-RS 2.1 specification (JSR 370) — https://jakarta.ee/specifications/restful-ws/2.1/
- Richardson's Maturity Model (Martin Fowler) — https://martinfowler.com/articles/richardsonMaturityModel.html (cited in §7.2).
- HAL (`application/hal+json`) specification — https://stateless.group/hal_specification.html (cited in §7.2 and §7.3).
- JSON:API specification — https://jsonapi.org (cited in §7.2 and §7.3).
- Jackson databind — https://github.com/FasterXML/jackson-databind (project README).
- IETF RFC 7231 §4.2.2 Idempotent Methods — https://datatracker.ietf.org/doc/html/rfc7231#section-4.2.2 (cited in §7.4).
- IETF RFC 7231 §6.5.6 (406 Not Acceptable) — https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.6
- IETF RFC 7231 §6.5.13 (415 Unsupported Media Type) — https://datatracker.ietf.org/doc/html/rfc7231#section-6.5.13
- IETF RFC 4918 §11.2 (422 Unprocessable Entity) — https://datatracker.ietf.org/doc/html/rfc4918#section-11.2 (cited in §7.8).
- GDPR Articles 5(1)(f) and 33 — https://gdpr-info.eu/art-5-gdpr/ and https://gdpr-info.eu/art-33-gdpr/ (cited in §7.9).
