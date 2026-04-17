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

- **In-memory storage only.** The specification forbids a database. `dao/MockDatabase.java` holds three in-process collections (two `ArrayList`s and one `ConcurrentHashMap`) and `dao/GenericDAO.java` mediates access with synchronised blocks.
- **JSON everywhere.** Every resource method either produces or consumes `application/json`; every error path is intercepted by a dedicated `ExceptionMapper` that returns a structured JSON body rather than the servlet container's default error page.
- **Referential integrity is enforced at the service layer.** A sensor cannot be registered for a non-existent room, a room cannot be deleted while any sensor still references it, and a sensor under maintenance refuses new readings. Each of those violations maps to a distinct HTTP status code (422, 409, 403).
- **Stack traces never leave the process.** `GenericExceptionMapper` catches `Throwable`, logs the failure server-side through `java.util.logging`, and returns a generic 500 body.

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

**Seed data**

`dao/MockDatabase.java` populates the store on first access with two rooms and three sensors, so the endpoints return non-empty responses immediately:

| Rooms | Sensors |
|---|---|
| `LIB-301` Library Quiet Study (capacity 40) | `TEMP-001` Temperature, ACTIVE, in `LIB-301` |
| `LEC-101` Lecture Hall A (capacity 120) | `CO2-001` CO2, ACTIVE, in `LIB-301` |
| | `OCC-001` Occupancy, **MAINTENANCE**, in `LEC-101` |

The sensor `OCC-001` is deliberately left in `MAINTENANCE` so the 403 path is easy to exercise during demonstration.

## Sample curl commands

The specification requires at least five. The examples below cover discovery, happy-path CRUD, filtering, and each of the three domain-specific error paths.

```bash
BASE="http://localhost:8080/smart-campus-sensor-and-room-management-api/api/v1"
```

**1. Discovery — API metadata and resource map (HATEOAS entry point)**

```bash
curl -s "$BASE"
```

**2. List all rooms**

```bash
curl -s "$BASE/rooms"
```

**3. Fetch a single room by id**

```bash
curl -s "$BASE/rooms/LIB-301"
```

**4. Create a new room (expect 201 Created + `Location` header)**

```bash
curl -s -i -X POST "$BASE/rooms" \
     -H "Content-Type: application/json" \
     -d '{"id":"LAB-220","name":"Physics Lab 2","capacity":30}'
```

**5. Register a sensor inside that new room**

```bash
curl -s -i -X POST "$BASE/sensors" \
     -H "Content-Type: application/json" \
     -d '{"id":"LIGHT-220","type":"Lighting","status":"ACTIVE","currentValue":450.0,"roomId":"LAB-220"}'
```

**6. Filter sensors by type with a query parameter**

```bash
curl -s "$BASE/sensors?type=Temperature"
```

**7. Append a reading to a sensor (side effect: parent `currentValue` updates)**

```bash
curl -s -i -X POST "$BASE/sensors/TEMP-001/readings" \
     -H "Content-Type: application/json" \
     -d '{"value":22.8}'

# Re-fetch TEMP-001 to confirm currentValue was refreshed to 22.8
curl -s "$BASE/sensors/TEMP-001"
```

**8. Error path — delete a room that still has sensors (expect 409 Conflict)**

```bash
curl -s -i -X DELETE "$BASE/rooms/LIB-301"
```

**9. Error path — register a sensor for a non-existent room (expect 422 Unprocessable Entity)**

```bash
curl -s -i -X POST "$BASE/sensors" \
     -H "Content-Type: application/json" \
     -d '{"id":"HEAT-999","type":"Temperature","status":"ACTIVE","currentValue":19.0,"roomId":"NOT-A-ROOM"}'
```

**10. Error path — post a reading to a sensor in MAINTENANCE (expect 403 Forbidden)**

```bash
curl -s -i -X POST "$BASE/sensors/OCC-001/readings" \
     -H "Content-Type: application/json" \
     -d '{"value":5.0}'
```

## Project structure

```
smart-campus-sensor-and-room-management-api/
├── pom.xml                              Maven build, packaging = war
└── src/main/
    ├── java/com/w2120198/csa/cw/
    │   ├── SmartCampusApplication.java   @ApplicationPath("/api/v1")
    │   ├── dao/
    │   │   ├── GenericDAO.java           Synchronised list-backed store
    │   │   └── MockDatabase.java         Static seed data + readings map
    │   ├── model/
    │   │   ├── BaseModel.java            Shared id contract
    │   │   ├── ErrorMessage.java         JSON error envelope
    │   │   ├── Room.java
    │   │   ├── Sensor.java               STATUS_ACTIVE/MAINTENANCE/OFFLINE
    │   │   └── SensorReading.java
    │   ├── resource/
    │   │   ├── DiscoveryResource.java    GET /api/v1
    │   │   ├── SensorRoom.java           /api/v1/rooms
    │   │   ├── SensorResource.java       /api/v1/sensors
    │   │   └── SensorReadingResource.java  sub-resource, constructed by locator
    │   └── exception/
    │       ├── DataNotFoundException.java + Mapper       404
    │       ├── RoomNotEmptyException.java + Mapper       409
    │       ├── LinkedResourceNotFoundException + Mapper  422
    │       ├── SensorUnavailableException.java + Mapper  403
    │       └── GenericExceptionMapper.java               500 catch-all
    └── webapp/WEB-INF/                   servlet descriptor (empty — Jersey scans)
```

---

## 7. Conceptual Report

The questions below are transcribed verbatim from the coursework specification. Each answer cites the file in which the relevant implementation lives. File paths are relative to the project root (`smart-campus-sensor-and-room-management-api/`).

### 7.1 Part 1.1 — JAX-RS resource lifecycle and in-memory thread safety

**Question.** Explain the default lifecycle of a JAX-RS Resource class. Is a new instance instantiated for every incoming request, or does the runtime treat it as a singleton? Elaborate on how this architectural decision impacts the way you manage and synchronize your in-memory data structures (maps/lists) to prevent data loss or race conditions.

By default, Jersey creates a fresh instance of each resource class for every incoming request. The JAX-RS specification calls this the "per-request" lifecycle, and it is the behaviour used throughout this project because no class is annotated with `@Singleton` and no explicit binding is registered in `SmartCampusApplication.java`. The practical consequence is that instance fields on a resource class are inherently thread-confined: `SensorRoom`, `SensorResource`, and `SensorReadingResource` all hold their collaborators (the `GenericDAO` handles and, in the reading resource, the parent `Sensor`) as instance fields, and those fields are seen only by the thread servicing the request that triggered their construction.

The data that genuinely crosses request boundaries lives in `dao/MockDatabase.java`, whose three static collections are shared by every request thread. Two of them (`ROOMS` and `SENSORS`) are plain `ArrayList`s; iteration and mutation must therefore be coordinated. `dao/GenericDAO.java` takes that responsibility on behalf of the resource classes: every read and write method acquires a monitor on the backing list through `synchronized (items) { ... }`. This prevents the two common failure modes that would otherwise appear — `ConcurrentModificationException` from a reader traversing the list while a writer removes an element, and lost updates from two writers racing on `List.set`. The readings map, `READINGS_BY_SENSOR`, is instead declared as a `ConcurrentHashMap`, which gives `computeIfAbsent` the atomic "fetch-or-create" guarantee needed to register the first reading for a sensor without a torn update. The inner list it stores is then protected with a narrower synchronised block inside `SensorReadingResource.addReading` (`synchronized (history) { history.add(reading); }`).

The spec's compound rule "delete a room only if it has no sensors" cannot be expressed by concurrent collections alone because it is a check-then-act sequence. The lock held by `GenericDAO.delete` covers only the removal; the precondition is checked in the resource, so in a truly contended system a narrower invariant would be required. For the traffic profile this assessment targets, the per-method synchronisation in `GenericDAO` is sufficient and the trade-off is documented here rather than masked.

### 7.2 Part 1.2 — Hypermedia and HATEOAS

**Question.** Why is the provision of "Hypermedia" (links and navigation within responses) considered a hallmark of advanced RESTful design (HATEOAS)? How does this approach benefit client developers compared to static documentation?

HATEOAS — Hypermedia As The Engine Of Application State — sits at the top of Richardson's Maturity Model because it completes the separation between the client's model of the service and the server's URI scheme. When a response carries the links a client needs in order to make its next request, the client no longer has to hard-code those URIs from documentation. The server is free to move endpoints, version them, or rename path segments, so long as the hypermedia in the responses stays internally consistent.

`resource/DiscoveryResource.java` is the practical entry point for this pattern in the present API. `GET /api/v1` returns a JSON object containing an `administrator` block, a semantic `version` string, and a `resources` map that lists every top-level collection together with its absolute URI (`"rooms": "http://.../api/v1/rooms"` and `"sensors": ".../api/v1/sensors"`). A well-behaved client bookmarks only the discovery URL and follows the `resources` map from there. If the university were to re-deploy the service behind a new context root or move to `/api/v2`, the discovery response would change and the client would adapt without a code release.

The benefit over static documentation is threefold in practice. First, the documentation drifts: a PDF reference written at v1.0 is usually out of date by v1.2, while a live discovery response is authoritative by construction. Second, conditional links communicate domain state. A room that still holds sensors could, for example, publish a `"sensors"` link but omit a `"delete"` link, so that the client's UI naturally disables the delete button for rooms the server would refuse to delete. Third, test tooling and API explorers such as Postman's "Link" follower consume hypermedia directly, reducing the setup cost of new clients. The richer the links in a response, the closer the API moves to being self-describing — which is precisely what Fielding's original REST dissertation set out as the goal.

### 7.3 Part 2.1 — Returning IDs versus full objects in list endpoints

**Question.** When returning a list of rooms, what are the implications of returning only IDs versus returning the full room objects? Consider network bandwidth and client side processing.

The trade-off is between single-round-trip responses that may contain unused fields, and thin identifier lists that force a second round trip for every detail view.

Returning ID-only collections keeps the `GET /rooms` payload minimal. For a campus with several thousand rooms, a response of the form `["LIB-301", "LEC-101", ...]` is an order of magnitude smaller than the corresponding full-object list, which is attractive for mobile clients on cellular links and for dashboards that only need to populate a drop-down. The cost is the N+1 network problem: a user interface that then wants to render each room's name or capacity must issue one `GET /rooms/{id}` per identifier. Each extra request carries the TCP/TLS handshake amortisation, the servlet dispatch, and a fresh JSON serialisation, so the aggregate cost can easily exceed the original savings.

Returning full objects, the approach taken by `SensorRoom.getAllRooms()` in `resource/SensorRoom.java`, solves the N+1 problem at the cost of a larger single response. The decision is defensible here because a `Room` is small (an identifier, a display name, an integer capacity, and a short list of sensor ids) and the campus inventory is bounded. The marginal payload size is predictable even in the worst case.

Production APIs that need both shapes typically expose one of three compromises: sparse fieldsets (`GET /rooms?fields=id,name` — the response contains only the requested fields), projection DTOs (a dedicated `RoomSummary` record that omits the `sensorIds` list), or cursor-based pagination (`GET /rooms?limit=50&after=LIB-301`) so that the client consumes the collection incrementally. The present API does not implement any of these because the specification does not require them and adding them for their own sake would only inflate the surface that a viva examiner might probe. The decision is documented here as a deliberate scope boundary rather than an oversight.

### 7.4 Part 2.2 — DELETE idempotency

**Question.** Is the `DELETE` operation idempotent in your implementation? Provide a detailed justification by describing what happens if a client mistakenly sends the exact same `DELETE` request for a room multiple times.

Yes — and the crucial distinction is that idempotency is defined on the **server state** that results from the request, not on the response the client observes. RFC 7231 §4.2.2 puts it precisely: a method is idempotent if the intended effect of N identical requests (for N ≥ 1) is the same as the effect of a single request.

Tracing the three cases against `SensorRoom.deleteRoom` in `resource/SensorRoom.java`:

1. **First call on a room with no sensors.** `roomDAO.getById(roomId)` returns the entity, the `sensorIds` guard passes because the collection is empty, `roomDAO.delete(roomId)` removes it, and the method returns `204 No Content`. The server state after this call contains no such room.
2. **Second identical call.** `roomDAO.getById(roomId)` now returns `null`; the method throws `DataNotFoundException`, which `DataNotFoundExceptionMapper` renders as `404 Not Found`. The server state does not change: the room is still absent.
3. **Third, fourth, ... call.** Exactly the same as call 2. The 404 body is deterministic and the store is untouched.

The status codes differ between call 1 and calls 2+, but the property required by idempotency is satisfied: after any non-zero number of identical `DELETE /rooms/{id}` requests, the room is absent. A client that retries a lost acknowledgement therefore converges on the same outcome as one that succeeded on the first attempt, which is exactly the network-resilience property the REST community attributes to idempotent verbs.

The 409 Conflict case ("room still has sensors") does not threaten this guarantee. A 409 response leaves the room exactly where it was, so the server state is unchanged on every repetition; the response is also deterministic until some other request removes the dependent sensors, at which point the next `DELETE` will succeed and subsequent ones will revert to the 404 pattern above. The operation therefore stays idempotent in the presence of the business rule.

### 7.5 Part 3.1 — `@Consumes` mismatch consequences

**Question.** We explicitly use the `@Consumes(MediaType.APPLICATION_JSON)` annotation on the POST method. Explain the technical consequences if a client attempts to send data in a different format, such as `text/plain` or `application/xml`. How does JAX-RS handle this mismatch?

`@Consumes` is the server half of content negotiation: it advertises the media types the resource method is prepared to deserialise. When a request arrives, Jersey's runtime inspects the `Content-Type` header before it selects a method to invoke. If no resource method on the matched path declares a `@Consumes` value compatible with the client's `Content-Type`, Jersey never constructs the resource, never calls the method, and returns **HTTP 415 Unsupported Media Type** with an empty body.

Two consequences follow for the present API. First, validation code written inside `SensorResource.createSensor` — the null checks on `sensor.getId()`, the referential-integrity lookup that raises `LinkedResourceNotFoundException`, and the duplicate-id check — is only reached when the request is already known to be JSON. A client that sends `application/xml` or `text/plain` is rejected at the dispatch stage, which keeps the method body free of format-handling branches and means XML payloads can never silently half-parse. Second, the 415 is a clear client contract: retrying the same payload will never succeed, only re-encoding it as JSON will. This is categorically different from a 500, which a client might reasonably retry assuming a transient server fault.

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

The second benefit is parent-context preparation. The locator is the ideal place to resolve and validate the parent entity once, pass it into the sub-resource's constructor, and let the sub-resource methods assume a non-null, valid parent. `SensorResource.readings` does exactly that: it looks the sensor up, throws `DataNotFoundException` if it is missing, and constructs `new SensorReadingResource(parent, sensorDAO)`. Both methods on the sub-resource (`getReadings` and `addReading`) can then focus on reading-specific rules — the maintenance-state guard, the UUID assignment, the `currentValue` side effect — without re-fetching the sensor or repeating the null check.

The third benefit is recursion. Nesting scales. A reading could, in a larger system, acquire its own annotations sub-resource (`/sensors/{id}/readings/{rid}/annotations`) simply by adding another locator inside `SensorReadingResource`. The top-level class neither grows nor becomes entangled in the new hierarchy. The pattern is also friendlier to testing: `SensorReadingResource` is a plain Java class with a straightforward constructor, so it can be unit-tested by instantiation without bootstrapping the JAX-RS runtime.

### 7.8 Part 5.1 — Why 422 beats 404 for a broken reference in a valid payload

**Question.** Why is HTTP 422 often considered more semantically accurate than a standard 404 when the issue is a missing reference inside a valid JSON payload?

The three candidate statuses say very different things about the failure. HTTP 404 Not Found signals that the URI in the request line does not identify a resource. HTTP 400 Bad Request signals that the server cannot parse or syntactically validate the request — malformed JSON, missing required fields, the wrong type in a field. HTTP 422 Unprocessable Entity, introduced by WebDAV in RFC 4918 §11.2 and widely adopted across REST APIs, signals that the request is syntactically well-formed but semantically invalid: the server understands every byte of it but cannot act on it because a domain-level constraint fails.

`POST /api/v1/sensors` with a payload such as `{"id":"HEAT-999","roomId":"NOT-A-ROOM",...}` falls squarely into the third category. The target URI `/sensors` **does** exist, so 404 would misrepresent the problem (the resource the client is addressing is the sensors collection, and the collection is present and willing to accept registrations). The payload **is** valid JSON with every required field of the correct type, so 400 would also misrepresent it. What fails is referential integrity between the payload and existing server state: the room the client is pointing at was never created.

The current implementation expresses this by throwing `LinkedResourceNotFoundException` from `SensorResource.createSensor` and letting `LinkedResourceNotFoundExceptionMapper` render it as 422 with a JSON `ErrorMessage` body containing the offending `roomId`. That response tells the client three things simultaneously: the request was understood, re-sending the same bytes will not change the outcome, and the fix is to create the referenced room first (or correct the id). A 404 response would ambiguously suggest that the sensors endpoint itself has disappeared, and a 400 would suggest a typo or missing field the client cannot actually find. 422 collapses the ambiguity.

### 7.9 Part 5.2 — Cybersecurity risks of leaked stack traces

**Question.** From a cybersecurity standpoint, explain the risks associated with exposing internal Java stack traces to external API consumers. What specific information could an attacker gather from such a trace?

An unfiltered stack trace is a reconnaissance gift. Four distinct classes of information leak out of it, and each one enables a different stage of an attack.

**Internal paths and package structure.** Every frame in the trace carries a fully-qualified class name. A response that leaks `at com.w2120198.csa.cw.dao.GenericDAO.delete(GenericDAO.java:63)` simultaneously discloses the student identifier used as the root package, the two-letter module code embedded in it, and the service's internal layer split between `dao` and `resource`. An attacker now knows where business logic lives without decompiling anything.

**Library versions for CVE targeting.** Frames in stack traces frequently reference third-party classes — `org.glassfish.jersey.server.ServerRuntime$1.run`, `com.fasterxml.jackson.databind.ObjectMapper.readValue`. A manifest read or a casual cross-reference with `pom.xml` pins those libraries to specific versions. The National Vulnerability Database can then be queried for CVEs affecting Jersey 2.32 or the corresponding Jackson line; the attacker walks away with a list of known weaknesses already present in the deployment.

**Logic flaws.** The line number and exception class together hint at the internal failure. A `NullPointerException at SensorRoom.deleteRoom:78` publicly admits that a particular request path hits a null dereference under some condition, and the exception type narrows the guess about which variable. An attacker can then craft payloads that deliberately reach that branch, either to exhaust the service with 500s or to probe for a crash that escapes the `ExceptionMapper` net.

**Server and runtime configuration.** Real-world traces have been known to include filesystem paths, operating user names, container flavours and versions, JVM flags, and, in genuinely bad cases, values of environment variables or JDBC URLs that were being logged at the moment of failure. None of this is information a legitimate API consumer needs.

The remedy in this project is `exception/GenericExceptionMapper.java`. It declares `ExceptionMapper<Throwable>`, so it acts as the final safety net for anything no more specific mapper has claimed. It logs the full `Throwable` through `java.util.logging` for operators, then returns a hand-written JSON body containing a generic 500 message, the numeric status, and a documentation link — nothing about the internal path, the library, the line, or the JVM. The failure stays inside the process; the client sees only what is safe to share.

---

## References

- Coursework specification and rubric: `context/cw/CW Complete Reference.md`, `context/cw/5COSC022W_Coursework Specification(1).pdf`, `context/cw/CSA_2026_Coursework_Rubric_Final.xlsx` (workspace-private, not tracked in this repository).
- Fielding, R. T. (2000). *Architectural Styles and the Design of Network-based Software Architectures* (PhD thesis, UC Irvine), chapter 5 — origin of the REST architectural constraints cited in §7.2.
- IETF RFC 7231 §4.2.2 — definition of idempotent methods cited in §7.4.
- IETF RFC 4918 §11.2 — origin of the 422 Unprocessable Entity status code cited in §7.8.
- Jersey 2.32 User Guide, chapter 3 (JAX-RS Application, Resources and Sub-Resources) and chapter 9 (Filters, Interceptors and ExceptionMappers) — the JAX-RS reference used throughout development.
- Richardson, L. & Ruby, S. (2007). *RESTful Web Services*, O'Reilly — maturity-model terminology used in §7.2.
