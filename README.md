# ASTRA

**ASTRA** is an Android application featuring a locally running AI assistant powered by a native **llama.cpp-based inference engine**. It is designed to run AI models directly on supported Android devices without relying on cloud-based AI APIs.

The project integrates Android's native capabilities with a locally hosted GGUF language model to provide on-device AI inference.

## Features

- **Local AI Inference:** Runs language models directly on the Android device.
- **Native AI Engine:** Uses llama.cpp and prebuilt native libraries.
- **Offline Architecture:** AI inference does not require a cloud-based AI API.
- **GGUF Model Support:** Uses quantized GGUF models to reduce memory requirements.
- **Android Integration:** Connects the native inference engine with the Android application.
- **Speech Recognition:** Integrates Android SpeechRecognizer for voice input.
- **Text-to-Speech:** Designed to support spoken AI responses.

## Tech Stack

| Technology | Purpose |
|---|---|
| Java | Android application development |
| XML | Android UI layouts |
| C++ / JNI | Native AI integration |
| llama.cpp | Local language model inference |
| GGUF | Model format |
| Gradle | Build automation |
| CMake | Native build configuration |
| Android SDK | Android platform development |

## Requirements

Make sure the following tools and dependencies are installed:

- Android Studio with support for Android Gradle Plugin 9.3.1
- JDK 25 (configured Gradle daemon JVM)
- Android SDK Platform 37
- Android NDK 30.0.16138531
- CMake 3.22.1
- A complete llama.cpp checkout with headers compatible with the prebuilt native libraries.

**Supported native architecture:** `arm64-v8a`

## Project Setup

### 1. Clone the repository

```bash
git clone https://github.com/Mitesh-Chandel/ASTRA.git
cd ASTRA
```

### 2. Configure llama.cpp

By default, CMake expects the llama.cpp source directory alongside the ASTRA project.

Expected directory structure:

```text
projects/
├── ASTRA/
│   ├── app/
│   ├── gradle/
│   ├── build.gradle
│   └── settings.gradle
│
└── llama.cpp/
    ├── include/
    └── ggml/
```

If llama.cpp is installed elsewhere, configure its location using PowerShell:

```powershell
$env:LLAMA_CPP_ROOT = 'C:\path\to\llama.cpp'
```

**Important:** The llama.cpp headers must be compatible with the prebuilt shared libraries located in:

```text
app/src/main/jniLibs/arm64-v8a/
```

The exact matching llama.cpp revision is not pinned in this repository. Verify compatibility before replacing headers or native libraries.

### 3. Configure the AI model

ASTRA does not include model weights in the Git repository.

Download the following model from a source whose distribution terms permit your intended use:

```text
qwen2.5-0.5b-instruct-q4_k_m.gguf
```

Place it inside:

```text
app/src/main/assets/models/
```

The expected model location is:

```text
app/src/main/assets/models/qwen2.5-0.5b-instruct-q4_k_m.gguf
```

The application copies the model from the assets directory when AI initialization first occurs.

You can also temporarily store the downloaded model inside the root `models/` directory. GGUF files in both locations are excluded from Git.

## Build Instructions

From the ASTRA project directory, run:

### Build Debug APK

```powershell
.\gradlew.bat assembleDebug
```

### Run Unit Tests

```powershell
.\gradlew.bat testDebugUnitTest
```

### APK Location

After a successful build, the debug APK will be available at:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Install the APK on a compatible Android device to test the application.

## Native Libraries

ASTRA includes prebuilt native shared libraries targeting:

```text
arm64-v8a
```

These libraries are located in:

```text
app/src/main/jniLibs/arm64-v8a/
```

The native integration depends on compatible llama.cpp and ggml headers. Using incompatible source headers may result in compilation errors or runtime issues.

## Model Distribution

Model weights are intentionally excluded from Git because:

- GitHub rejects regular Git pushes containing files larger than 100 MB.
- Large model files increase repository size.
- Model distribution may be subject to separate licensing restrictions.

For public distribution, provide a separate download location, document the model's source and license, and publish a checksum (SHA-256) for integrity verification.

## Limitations

- Native libraries currently target `arm64-v8a` devices.
- AI inference performance depends on device hardware and available memory.
- The model must be downloaded separately.
- llama.cpp headers must match the prebuilt native libraries.
- Larger language models may exceed the memory capacity of lower-end Android devices.

## Licensing

No project license is currently included.

Before distributing ASTRA, review the licensing and attribution requirements for:

- llama.cpp
- ggml native libraries
- The selected GGUF language model
- Other third-party dependencies

Only add a project license when you have the authority to grant those rights.

## Author

**Mitesh Chandel**

GitHub: [Mitesh-Chandel](https://github.com/Mitesh-Chandel)

---

*ASTRA — Bringing local AI intelligence directly to Android devices.*
