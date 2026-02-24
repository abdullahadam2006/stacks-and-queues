# Development Notes

## IDE
- Import the project as a Maven project in your IDE (IntelliJ, Eclipse, VS Code).
- Use the IDE run configuration to set the main class (`Launcher` or `org.example.Main`).

## Testing
- Place tests under `src/test/java` and run `mvn test`.

## Linting / Formatting
- Add a formatter or Checkstyle plugin in `pom.xml` if you want strict style checks.

## Packaging & Distribution
- Use the Maven Shade plugin or Assembly plugin to produce a single fat JAR.
- Ensure `src/main/resources/META-INF/MANIFEST.MF` contains the `Main-Class` if you want `java -jar` to work without extra flags.

## Further work
- Add a `USAGE.md` with example command-line options for the transfer utility.
- Add runtime configuration (properties file) and document environment variables.
