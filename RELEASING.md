Releasing
=========

 1. Change the version in `gradle.properties` to a non-SNAPSHOT version.
 2. Update the `CHANGELOG.md` for the impending release.
 3. Update the `README.md` with the new version.
 4. `git commit -am "Prepare for release X.Y.Z."` (where X.Y.Z is the new version)
 5. `git tag -a X.Y.Z -m "Version X.Y.Z"` (where X.Y.Z is the new version)
 6. `./gradlew clean bintrayUpload`
 7. Update the `gradle.properties` to the next SNAPSHOT version.
 8. `git commit -am "Prepare next development version."`
 9. `git push && git push --tags`


Prerequisites
-------------

First, you will need a Bintray account with access to the [linkedin-spyglass](https://bintray.com/linkedin-spyglass) organization.

Then, set the following environment variables:

 * `BINTRAY_USER` - Bintray username
 * `BINTRAY_KEY`  - Bintray API key for the given user account