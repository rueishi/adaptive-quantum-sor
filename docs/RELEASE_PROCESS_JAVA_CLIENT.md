# Java Client Release Process

1. Confirm `com.nitroj.sor` ownership in Maven Central / Sonatype Central.
2. Configure release signing with GPG and publish credentials outside source control.
3. Run `./gradlew :sor-client-java:javadoc :sor-client-java:test`.
4. Run `./gradlew :sor-client-java:publishToMavenLocal`.
5. Run `cd sor-client-java/examples/quickstart && ./gradlew run`.
6. Publish `com.nitroj.sor:sor-client-java:1.0.0` through the Central staging workflow.
7. Verify the staged POM contains description, license, SCM, and developer metadata.
8. Release the staging repository and smoke-test a clean project against Maven Central.
