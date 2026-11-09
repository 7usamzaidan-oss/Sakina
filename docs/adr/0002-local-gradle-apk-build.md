# APKs are built locally with Gradle, not EAS Build

There is no Expo account available, so APKs are produced with `expo prebuild` + Gradle inside the dev environment and handed over directly. `eas.json` (with the `preview` = apk and `production` = app-bundle profiles from the brief) is still kept so EAS Build can be adopted later without changes.
