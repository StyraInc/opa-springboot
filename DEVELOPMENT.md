# OPA-SpringBoot Development

## Changelog Conventions

* Each version should have an h2 heading (`##`) consisting of its semver version, using the same format as for tags (see *Release Process* below).
* If a version has not been released yet, `(unreleased)` should be added to the end of the heading.
* The newest version should be the first subheading, and the oldest should be the least.

## Release Process

> [!IMPORTANT]
> GitHub Actions workflows used for release processes will only run when `CHANGELOG.md` has been modified, and will not run if the first subheading contains `(unreleased)`.

1. Create branch `release-$VERSION`.
2. Update the version number in `build.gradle`.
3. Update `CHANGELOG.md` to reflect changes made since the last version.
4. Create a PR for the release. Merge it once tests pass.
5. GitHub Actions should automatically update the docs site to reflect the release, and publish a staging repository to Maven Central.
6. Use the OSSRH portal to publish the release to Maven Central, see [this doc for instructions](https://styrainc.github.io/opa-java/maintenance/releases/).
7. Tag the release. Tags should follow semver conventions and be formatted as `v$MAJOR.$MINOR.$PATCH`.
8. Create a GitHub release, copy-paste the relevant section of the changelog into it.
9. Delete the release branch.

## Toolchain Setup for macOS

If you do not already have a working Java 17 toolchain on macOS, you can use these steps to set one up. You must also have a working Docker installation to run the tests.

1. Install [Homebrew](https://brew.sh/), if you have not already.
2. Install the JDK with `brew install openjdk@17`.
3. Install `jenv` with `brew install jenv`.
4. You need to make `jenv` aware of the your Homebrew Java installations. This shell command will remove any existing Java installs from `jenv`, and add all OpenJDK installs from Homebrew: `jenv versions | tr -d '*' | awk '$1!="system"{print($1)}' | while read -r vers ; do jenv remove "$vers" ; done ; for p in /opt/homebrew/Cellar/openjdk* ; do for q in "$p"/* ; do jenv add "$q" ; done ; done`
5. Use `jenv` to select version 17 with `jenv global 17`.

Further reading:

* [Step-by-Step Guide: Installing and Switching Java Versions on Mac OSX](https://medium.com/@haroldfinch01/step-by-step-guide-installing-and-switching-java-versions-on-mac-osx-f3896b9872f4)
