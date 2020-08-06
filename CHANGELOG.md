<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# Drupal Test Runner Changelog

## [Unreleased]
### Changed
- Move to Gradle build

## [1.0-rc3]
### Added
- Only pass `--types` and other Drupal 8 flags if the project is for Drupal 8.
### Fixed
- Fix java.lang.NoSuchMethodError on createProcessHandler

## [1.0-rc2]
### Changed
- Update deprecated items.

## [1.0-rc1]
### Added
- Add screenshots for wiki, update README screenshot.
### Changed
- Check if `vendor` created for check configuration.
### Fixed
- Fix deprecation notice on createProcessHandler
- Fix NotNull error when Drupal integration is not yet configured
- Unselected test types in the UI output as 'null' in command parameter
- Invalid Drupal Directory configured for this project

## [1.0-beta2]
### Added
- Support running specific types.
- Link to failed test class files.
### Changed
- Adjust naming and positioning on configure form.

## [1.0-beta1]

### Added
- Allow debugging like phpunit tests.
- Add utility to help build test runner command.
- If Drupal path isn't configured, error for run configuration should link to settings form.
- Add support for "repeat"
- Support "die-on-fail"

### Changed
- Clean up and configuration class and command building.
