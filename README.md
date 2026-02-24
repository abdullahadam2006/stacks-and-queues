# SecureFileTransfer

A small Java project for securely transferring files (sample project structure).

## Contents
- `src/main/java` — application source (includes `Launcher.java`, `SecureFileTransfer.java`, and `org.example.Main`)
- `src/main/resources` — resources and `META-INF/MANIFEST.MF`
- `target/` — build output

## Prerequisites
- Java 11+ (JDK)
- Maven 3.6+

## Build
Build the project with Maven:

```bash
mvn clean package
```

This compiles the code and produces classes under `target/classes` (and a packaged artifact if configured).

## Run
Run the project from the compiled classes (example runs the `org.example.Main` class):

```bash
java -cp target/classes org.example.Main
```

If your `pom.xml` packages a runnable JAR with the main class and `MANIFEST.MF` configured, you can run:

```bash
java -jar target/<artifact-name>.jar
```

Replace `<artifact-name>.jar` with the actual artifact produced by `mvn package`.

## Development notes
- Use your IDE for debugging and running individual classes (`Launcher.java` may be an alternate entry point).
- Tests (if any) live under `src/test/java` — run them with `mvn test`.

## Documentation
More details in the `docs/` folder: `docs/overview.md`, `docs/usage.md`, and `docs/development.md`.

---

If you'd like, I can update the README with exact Maven coordinates and a runnable JAR name after you confirm the `pom.xml` artifactId/version or let me inspect `Launcher.java` to pick the correct main class to document.
