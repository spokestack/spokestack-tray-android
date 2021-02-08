## How to contribute to Spokestack Tray
Interested in contributing to Spokestack Tray? Welcome!

### Pull Requests

Before making any changes, we recommend opening an issue (if it doesn’t
already exist) and discussing your proposed changes. This will let us give
you advice on the proposed changes. If the changes are minor, then feel free
to make them without discussion.

## Deploying
https://github.com/nebula-plugins/nebula-release-plugin
https://github.com/nebula-plugins/nebula-publishing-plugin


1. Add the following to your `~/.gradle/gradle.properties`:

```text
signing.keyId=XXXXXXXX
signing.password=<key password, if one exists>
signing.secretKeyRingFile=/Users/me/.gnupg/secring.gpg
centralUsername=<sonatype username>
centralPassword=<sonatype password>
```

1. Start a new branch for your release. Name it according to the release's semantic version number: `v#.#.#`.
1. Run `./gradlew final`. This will create a git tag and push it to GitHub.
  * By default, `final` increments the minor version in the project's semantic version.
  * To bump the major or patch version: `./gradlew final -Prelease.scope=(major|patch)]`.
1. Run `./gradlew publish -Prelease.useLastTag=true`. This uploads artifacts to a staging repository to be synced with Maven Central.
1. PR your release branch on GitHub.
1. Add release notes for the tag on GitHub.
