# Progress

## Completed
- Cloned and inspected PMCADemo, Recipe Lab, Sony-PMCA-RE, OpenMemories-Framework; revisions in research/sources.lock.json.
- Identified real SDK definitions in OpenMemories-Framework/stubs/src/main/jasmin.
- Selected a small Java-only API-10 application, based on the MIT PMCADemo camera lifecycle.

## Verified from source (not hardware)
- CameraEx.open/getNormalCamera supports the native Android camera bridge.
- PMCADemo uses null capture callbacks; Sony handles writing to the card.
- Scalar display manager exposes finder/panel routing, video rectangle and display events.
- OpenMemories lists A7M2 Scalar API 2.10; Recipe Lab marks ILCE-7M2 compatible without a firmware version.

## Investigating
- Legacy packaging on Apple Silicon, v1 signing, preview coordinate systems and RAW+JPEG negotiation.

## Unverified / outstanding hardware tests
- APK installation, preview, AF, exposure, capture, actual RAW size, return to preview, EVF composition, eye sensor, lifecycle.
- VideoRect coordinate space and physical pixel aspect; RAW/preview lens-correction differences.
- No camera operations or installs have been performed.
