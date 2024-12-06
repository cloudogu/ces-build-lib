# Developing the ces-build-lib

You need a working Java-11 SDK

Run tests with

```bash
./mvnw test
```

If you want to run tests within IntelliJ you need to use Java 8.

# Running tests in IntelliJ

Open Project Structure and set Java 8 as SDK.

Run

```bash
./mvnw install
```

Then right-click tests in IntelliJ and run.

# Update Maven itself

Use this line to update the mvnw command with your desired version:

```bash
./mvnw -N wrapper:wrapper -Dmaven=3.9.9
```

This will change the mvnw-File and the mvnw.cmd-File.
