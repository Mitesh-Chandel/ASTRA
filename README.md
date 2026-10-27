# ASTRA

ASTRA is an Android application with a native llama.cpp-based AI engine. The
native libraries included in the project target **arm64-v8a** devices.

## Requirements

- Android Studio with support for Android Gradle Plugin 9.3.1
- JDK 25 (the Gradle daemon JVM version configured in this project)
- Android SDK Platform 37
- Android NDK 30.0.16138531
- CMake 3.22.1
- A complete llama.cpp checkout whose headers are compatible with the
  prebuilt libraries in `app/src/main/jniLibs/arm64-v8a/`

The Gradle wrapper downloads the configured Gradle distribution automatically.

## Native headers

By default, CMake looks for llama.cpp in a directory named `llama.cpp` alongside
the ASTRA checkout. For example:

```text
projects/
  ASTRA/
  llama.cpp/
```

If llama.cpp is somewhere else, set `LLAMA_CPP_ROOT` before building:

```powershell
$env:LLAMA_CPP_ROOT = 'C:\path\to\llama.cpp'
.\gradlew.bat assembleDebug
```

Use headers from a llama.cpp revision compatible with the prebuilt shared
libraries in `app/src/main/jniLibs/arm64-v8a/`. The matching source revision is
not pinned in this repository; confirm compatibility before changing either
the headers or shared libraries.

## AI model

The model weights are not stored in Git. GitHub rejects files larger than
100 MB in regular Git pushes, and model weights may have separate distribution
terms.

Obtain `qwen2.5-0.5b-instruct-q4_k_m.gguf` from a source whose terms permit your
use, then place it at:

```text
app/src/main/assets/models/qwen2.5-0.5b-instruct-q4_k_m.gguf
```

The app copies the model from that asset location when it first loads the AI.
The root `models/` directory can be used as temporary local storage for the
file; `.gguf` files there and in the app assets directory are ignored by Git.
For public distribution, host the model separately, document its source and
license, and provide a checksum so users can verify their download.

## Build

From the project directory, run:

```powershell
.\gradlew.bat assembleDebug
```

To run the local unit tests:

```powershell
.\gradlew.bat testDebugUnitTest
```

The debug APK is written to `app/build/outputs/apk/debug/`.

## Licensing

No project license is included. Add a license only when you have authority to
grant it. Before publishing, also review the licenses and notice requirements
for the bundled llama.cpp/ggml native libraries and any model you distribute.
