---
name: java-spring-best-practices
description: 'Best practices for writing Java, Spring Boot, and Lombok code. Use when writing or reviewing Java/Spring classes, services, controllers, entities, DTOs, configuration, or when deciding how/whether to apply Lombok annotations. Covers constructor injection, immutability, exception handling, transactions, DTO mapping, and Lombok pitfalls with JPA.'
---

# Java, Spring & Lombok Best Practices

## When to Use
- Writing or reviewing Spring Boot components (`@Service`, `@RestController`, `@Repository`, `@Configuration`).
- Designing entities, DTOs, or records.
- Deciding whether/how to use Lombok, and avoiding its common traps.

## Java Core
- Target modern Java (17/21). Prefer `record` for immutable data carriers (DTOs, value objects, events).
- Prefer immutability: `final` fields, unmodifiable collections (`List.copyOf`, `List.of`), no setters unless required.
- Use `Optional` for return values that may be absent; never for fields or method parameters.
- Fail fast at boundaries only: validate inputs at public entry points (controllers, public APIs), not in every internal method.
- Use `switch` expressions and pattern matching over long if/else chains.
- Never swallow exceptions. Don't catch `Exception`/`Throwable` broadly; catch the specific type you can handle.
- Use `try-with-resources` for anything `Closeable` (streams, clients, JDBC).
- Log with SLF4J parameterized messages (`log.info("id={}", id)`), never string concatenation; never log secrets/PII.

## Spring Boot
- **Constructor injection only.** Inject via a single constructor into `private final` fields. Avoid `@Autowired` on fields and setter injection (hurts testability and immutability). With one constructor, `@Autowired` is not needed.
- Keep controllers thin: validation + delegation. Put business logic in services.
- Use `@ConfigurationProperties` (type-safe, grouped) over scattered `@Value` for related config; validate with `@Validated`.
- Externalize config; never hardcode secrets. Read from env vars / secrets manager. Use profiles (`application-<profile>.yml`) for env differences.
- **Transactions**: put `@Transactional` on service methods, not controllers or repositories. Use `readOnly = true` for queries. Remember it only applies to public methods called through the proxy (self-invocation bypasses it).
- Validate request bodies with Jakarta Bean Validation (`@Valid` + `@NotNull`, `@Size`, etc.).
- Handle errors centrally with `@RestControllerAdvice` + `@ExceptionHandler`; return consistent error payloads (e.g. `ProblemDetail`). Don't leak stack traces.
- Never expose JPA entities directly over the API. Map to DTOs/records to avoid over-fetching, lazy-loading serialization issues, and leaking internal fields.
- Prefer constructor-based `RestClient`/`WebClient` beans built once; don't create clients per request.
- Write slice tests (`@WebMvcTest`, `@DataJpaTest`) for fast feedback; reserve `@SpringBootTest` for true integration paths. Use Testcontainers for real DB/infra.

## Lombok
Use Lombok to cut boilerplate — deliberately, not blanket `@Data` everywhere.
- **Prefer explicit annotations** over `@Data`: e.g. `@Getter`, `@RequiredArgsConstructor`, `@Builder`, `@ToString`. `@Data` bundles setters + `equals`/`hashCode` + `toString`, which is often wrong for entities and mutable services.
- **Constructor injection with Lombok**: annotate the class with `@RequiredArgsConstructor` and declare dependencies as `private final` fields. Clean and testable.
- **Slf4j**: use `@Slf4j` to get a `log` field instead of declaring loggers by hand.
- **DTOs**: prefer Java `record`s (built-in immutability, `equals`/`hashCode`/`toString`) over `@Data` classes. If you need a builder for a record, `@Builder` works on records/constructors.

### Lombok + JPA pitfalls (important)
- **Do NOT put `@Data`, `@EqualsAndHashCode`, or `@ToString` on JPA entities.**
  - `@ToString`/`@EqualsAndHashCode` include all fields by default → touching lazy associations triggers `LazyInitializationException` or extra queries, and can cause infinite recursion on bidirectional relations.
  - Generated `equals`/`hashCode` based on mutable fields break the entity's identity across persistence states.
- For entities: use `@Getter`/`@Setter` (or hand-written), and if you need `equals`/`hashCode`, base them only on a stable business key or the `@Id`, and exclude associations (`@ToString.Exclude`, `@EqualsAndHashCode.Exclude`).
- Avoid `@AllArgsConstructor` on entities; JPA needs a no-arg constructor and positional constructors are fragile as fields evolve.
- Keep Lombok config consistent via a project `lombok.config` (e.g. `lombok.addLombokGeneratedAnnotation = true` for coverage tools).

## Quick Checklist
- [ ] Constructor injection, `private final` deps, no field `@Autowired`.
- [ ] DTOs/records at the API boundary — entities never serialized directly.
- [ ] `@Transactional` on service methods; `readOnly` for reads.
- [ ] Central `@RestControllerAdvice` error handling; no leaked internals.
- [ ] No `@Data`/`@ToString`/`@EqualsAndHashCode` on JPA entities.
- [ ] Config externalized; no secrets in code; `@ConfigurationProperties` for grouped config.
- [ ] Specific exception handling; try-with-resources; parameterized logging.
