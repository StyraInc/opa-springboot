# OPA-SpringBoot Changelog

## v0.0.3

* Add `OPAAuthorizationManager` constructor that accepts a path but not an `OPAClient`.

## v0.0.2

* Rather than hard-coding `en`, the preferred key to search for decision reasons for can now be changed with `OPAAuthorizationManager.setReasonKey()`. The default remains `en`.
* Update `build.gradle` to accurately reflect Apache 2 license.

## v0.0.1

* Initial release of OPA-SpringBoot
