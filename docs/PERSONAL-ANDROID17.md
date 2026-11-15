# Personal Android 17 build

This branch fixes upstream issue #21 and reduces work in app-list browsing for
the Samsung SM-A175N. It retains Android-managed work-profile provisioning and
the existing Android 17 compile/target SDK (37). It does not require Knox
enrollment, a Knox license, root, or device-owner setup.

## Changes

- Give the settings fragment container a stable resource ID. XML creates the
  fragment; no second fragment transaction is needed.
- Fetch launchable package names in two category queries per list scan rather
  than looking up each package's launch intent separately. Keep hidden-app
  ordering and user/system application inclusion rules.
- Read each package's policy-hidden state once per scan.
- Use one app-list worker and two icon workers, shutting them down when the
  service is destroyed.
- Coalesce duplicate icon loads, keep adapter state on the main thread, and
  discard callbacks from old data or recycled rows.
- Bound cached bitmap storage to 4 MiB per adapter. Refreshes invalidate icons
  so app updates and label changes are reflected.
- Normalize searchable labels/packages once per data refresh and skip repeated
  normalized queries. Do not bind a row during ViewHolder construction.
- Retain the existing optimized R8 release build and resource shrinking.

Haven is Java/AIDL with no bundled native libraries. CPU-specific NDK flags or
ARM ABI filters would not optimize this app's Java execution. Actual latency,
memory, provisioning, and One UI behavior still need checks on the SM-A175N;
emulator results cannot establish a device-specific performance gain.

## Build and sign future updates

Use JDK 21, the Android 37 platform, Build Tools 37.0.0, and the checked-in Gradle
wrapper. The `Personal Android 17 release` workflow produces an **unsigned**,
optimized release APK. Its `haven-android17-build` artifact also includes build
tools, test APKs, lint/unit reports, and the R8 mapping. Debug and baseline APKs
are for testing only.

The personal signing key and passwords are backed up privately, outside GitHub.
Certificate SHA-256:

```text
c0f1469e2e0466637d0a9c86bf21a8836d45b5695b4ab38d8f9407dd22f4e0c2
```

To sign a future source build, extract the backup and copy its
`haven-personal-release.jks` and `keystore.properties` into the ignored
`release/` directory. Keep the application ID
`io.github.kennethchoinfosec.haven` and increment `versionCode` beyond the
installed version (this version uses 10). Then run:

```sh
./gradlew assembleRelease
apksigner verify --verbose --print-certs app/build/outputs/apk/release/app-release.apk
```

Alternatively, export with `./gradlew assembleRelease -PallowUnsignedRelease=true`
and use `scripts/sign-personal-apk.sh` with the private backup. Use the same key
for every update; do not generate a replacement key. Never commit the backup,
JKS, or passwords. Losing this key prevents in-place updates of this build.

This personal certificate differs from the upstream certificate. Android cannot
install this APK as an in-place update to an official Haven build. Removing a
profile-owner installation can require removing the work profile and its data;
back up any needed work-profile data before planning that migration.

## Verification

CI builds the release, checks unit tests and release lint, and compiles the
Android tests. A separate Android 17 emulator job installs an intentionally
unfixed layout to verify the exact `FragmentContainerView` exception from #21,
then tests settings launch/recreation, icon callback ordering and request
coalescing, search behavior, and app-list compatibility on the fixed build.
Check the workflow result and exported runtime evidence before calling runtime
verification complete. These checks exercise debug builds; release packaging
and signing are checked separately.
