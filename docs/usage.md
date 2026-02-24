# Usage

## Building

Run:

```bash
mvn clean package
```

## Running locally

Run the main class directly from compiled classes:

```bash
java -cp target/classes org.example.Main
```

Or use `Launcher` if that is the intended entry point:

```bash
java -cp target/classes Launcher
```

## Common options
- Add logging flags or system properties with `-D` when invoking `java`.
- To enable verbose GC or debugging, pass JVM flags before `-cp`.

## Packaging
If you want an executable JAR, ensure `pom.xml` sets the `mainClass` in the Maven Shade or Assembly plugin, then:

```bash
mvn package
java -jar target/<artifact-name>.jar
```

Replace `<artifact-name>.jar` with the produced JAR file.
