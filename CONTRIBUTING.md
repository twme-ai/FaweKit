# Contributing

Use Java 21 and keep changes compatible with the supported Paper and FAWE APIs.

Before opening a pull request, run:

```sh
./gradlew clean test build
```

Changes to player-facing workflows should also update `integration/run.js` and
be exercised against the isolated server described in `integration/README.md`.
Do not run that integration test against a production world.
