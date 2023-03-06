# Changelog
All **user-facing**, notable changes will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]
### Added
- You can Synchronize your songs with a Web Editor to modify them in any browser, for instance on your desktop computer.
  Then synchronize them back to see the changes on your local device.
  Click 3-dots icon on *My Songs* and pick *Synchronize with Web Editor*.
  This will start a temporary session for 24 hours, which you can access by opening link on any device.
  If changes were made in both places (locally and remotely), there would be a conflict to be resolved.
- *Songbook Web UI* is available at [songbook.igrek.dev](https://songbook.igrek.dev/ui).
  Right now it only handles *Synchronize Sessions*.
- Changelog can be opened from the *About* window
  and is available at [Manual pages](https://igrek51.github.io/android-songbook/CHANGELOG/).
- Application logs can be browsed in the app by typing the secret command `logs`.
- Copyright notices of the third-party libraries are linked in the *About* window.

## [1.33.2] - 2023-01-19
### Added
- Non-fatal errors can be reported by clicking *Send Report* on an error pop-up.

### Changed
- Improved startup performance by loading user data asynchronously.

### Fixed
- Inverted chords (eg. `C/G`) are no longer splitted when wrapping the lines.
- Fixed security error on some devices when opening the external links, like Privacy Policy.

## [1.33.1] - 2023-12-30
### Added
- Debug logs are included in reports in case of a crash.