## How to contribute to Spokestack Tray
Interested in contributing to Spokestack Tray? Welcome!

### Pull Requests

Before making any changes, we recommend opening an issue (if it doesn’t
already exist) and discussing your proposed changes. This will let us give
you advice on the proposed changes. If the changes are minor, then feel free
to make them without discussion.

## Deploying
https://github.com/researchgate/gradle-release
https://github.com/gradle-nexus/publish-plugin/


1. Add the following to your `~/.gradle/gradle.properties`:

```text
signing.keyId=XXXXXXXX
signing.password=<key password, if one exists>
signing.secretKeyRingFile=/Users/me/.gnupg/secring.gpg
sonatypeUsername=<sonatype username>
sonatypePassword=<sonatype password>
```

1. Start a new branch for your release. Name it according to the release's semantic version number: `v#.#.#`.
1. Run `./gradlew :SpokestackTray:release`. This will prompt for version information, create a git tag, push it to GitHub, and upload build artifacts to Sonatype's OSSRH server.
1. PR your release branch on GitHub.
1. Add release notes for the tag on GitHub.

Note that the project is configured to sign the git tag, so you may have problems if your gitconfig does not include signing information.
