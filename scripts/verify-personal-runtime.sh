#!/usr/bin/env bash
set -euo pipefail
mkdir -p runtime-evidence
adb shell getprop > runtime-evidence/emulator-properties.txt
adb install -r out/apk/haven-issue21-baseline.apk
adb install -r out/apk/haven-tests.apk
adb logcat -c
adb shell am instrument -w -r -e class io.github.kennethchoinfosec.haven.ui.SettingsActivityTest io.github.kennethchoinfosec.haven.test/androidx.test.runner.AndroidJUnitRunner > runtime-evidence/baseline-tests.txt 2>&1 || true
adb logcat -d > runtime-evidence/baseline-logcat.txt
if ! grep -q 'FragmentContainerView must have an android:id' runtime-evidence/baseline-logcat.txt; then
  cat runtime-evidence/baseline-tests.txt
  echo 'Original layout did not reproduce the issue #21 exception.' >&2
  exit 1
fi
adb install -r out/apk/haven-debug.apk
adb logcat -c
adb shell am instrument -w -r io.github.kennethchoinfosec.haven.test/androidx.test.runner.AndroidJUnitRunner | tr -d '\r' | tee runtime-evidence/fixed-tests.txt
adb logcat -d > runtime-evidence/fixed-logcat.txt
grep -Eq '^OK \([0-9]+ tests?\)' runtime-evidence/fixed-tests.txt
if grep -q 'FAILURES!!!\|Process crashed' runtime-evidence/fixed-tests.txt; then
  exit 1
fi
