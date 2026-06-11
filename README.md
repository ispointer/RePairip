<p align="center">
  <img src="preview/re.png" width="100" height="100" alt="RePairip logo"/>
</p>

<h1 align="center">RePairip</h1>

<p align="center">
  A Java reverse-engineering utility for analyzing and rebuilding Android APK/APKS packages that use Google Play PairIP protection.
</p>

<p align="center">
  <b>Current version:</b> 1.4.14 &nbsp;|&nbsp;
  <b>Language:</b> Java 17 &nbsp;|&nbsp;
  <b>Build:</b> Gradle + Shadow Jar
</p>

---

## Overview

RePairip is a command-line tool that works with PairIP-protected Android packages. It can merge split `.apks` packages, patch PairIP-related dex code, clean split metadata from the manifest, rebuild the APK, and optionally apply a static translation JSON to restore dumped PairIP values.

The project focuses on two workflows:

- **Dynamic dump/logging workflow**: merge an `.apks`, inject logging support, patch PairIP launcher/signature logic, rebuild the APK, and preserve selected dex CRC values.
- **Static translation workflow**: take a merged APK plus a `pairip.json` translation file, rewrite PairIP classes, restore strings/method references, remove unused PairIP runtime pieces, and rebuild a translated APK.

The dex rewriting logic is built on top of the open-source [smali/dexlib2](https://github.com/JesusFreke/smali) ecosystem.

---

## Screenshots

| App UI | Dump Style |
| --- | --- |
| ![App UI](preview/a1.jpg) | ![Dump Style](preview/a2.jpg) |

---

## Features

- Loads normal `.apk` files and split `.apks` bundles.
- Merges split APK modules into a single APK with `ARSCLib`.
- Patches `AndroidManifest.xml` split metadata and PairIP license components.
- Injects helper dex code from embedded resources:
  - `log.dex`
  - `restoreMethod.dex`
- Patches PairIP startup and license classes.
- Adds a `PairipLog` helper with an app-specific dictionary path.
- Detects simple PairIP junk holder classes.
- Inserts a `StartupLauncher.pairip()` call after `VMRunner.invoke()`.
- Rewrites signature/integrity checks to return safe values.
- Applies translation data from a JSON file.
- Extracts embedded dex payloads from top-level PairIP assets when needed.
- Removes `libpairipcore.so` during translation patching.
- Rebuilds final APK output beside the input file.

---

## Requirements

- JDK 17 or newer
- Gradle wrapper included in this repository
- A PairIP-protected `.apks` or already merged `.apk`

---

## Build

### Windows

```bat
gradlew.bat assemble
```

### Linux / macOS

```bash
./gradlew assemble
```

The final jar is generated here:

```text
antik/build/libs/RePairip.jar
```

The jar entry point is:

```text
com.antik.Main
```

---

## Usage

### 1. Merge and patch an APKS bundle

```bash
java -jar antik/build/libs/RePairip.jar -i input.apks
```

This flow:

- extracts the `.apks` bundle into a temporary directory
- merges split APK modules
- patches manifest split/license metadata
- patches dex files for PairIP logging and launcher behavior
- writes a merged output APK
- writes a PairIP-patched output APK

Expected output names:

```text
input_merged.apk
input_merged_pairip.apk
```

### 2. Apply a translation JSON to an APK

```bash
java -jar antik/build/libs/RePairip.jar -i input.apk -t pairip.json
```

This flow:

- loads the APK
- reads `pairip.json`
- rewrites selected PairIP classes
- restores static string/method values
- adds required embedded dex payloads when needed
- removes native PairIP runtime library entries
- writes a translated APK

Expected output name:

```text
input_translated.apk
```

---

## Translation JSON Format

The translation file is a JSON object keyed by class name. Each class entry contains a value `type` and a `fields` object.

Example:

```json
{
  "com.example.SomePairipHolder": {
    "type": "java.lang.String",
    "fields": {
      "a": "restored value",
      "b": "another restored value"
    }
  },
  "Lcom/example/MethodHolder;": {
    "type": "java.lang.reflect.Method",
    "fields": {
      "m0": "com.example.Target->methodName(java.lang.String):void"
    }
  }
}
```

Supported type forms include:

```text
java.lang.String
Ljava/lang/String;
java.lang.reflect.Method
Ljava/lang/reflect/Method;
```

Class names can be written as Java-style names or dex descriptors:

```text
com.example.ClassName
Lcom/example/ClassName;
```

---

## How It Works

### Full Pipeline

```text
// RePairip high-level flow
//
//                 input
//                   |
//        +----------+----------+
//        |                     |
//       .apk                  .apks
//        |                     |
//        |              +------v------+
//        |              |  Extract    |
//        |              |  Splits     |
//        |              +------+------+
//        |                     |
//        |              +------v------+
//        +------------->|  Merge APK  |
//                       +------+------+
//                              |
//                    +---------v----------+
//                    | Patch Manifest     |
//                    | - split metadata   |
//                    | - license entries  |
//                    +---------+----------+
//                              |
//                  +-----------v------------+
//                  | Translation provided?  |
//                  +-----------+------------+
//                       yes    |     no
//                              |
//        +---------------------+--------------------+
//        |                                          |
// +------v------+                           +-------v-------+
// | Rewrite     |                           | Logging Patch |
// | PairIP dex  |                           | + Dex Patch   |
// +------+------+                           +-------+-------+
//        |                                          |
// +------v------+                           +-------v-------+
// | Add restore |                           | Patch CRC32   |
// | dex/assets  |                           | if available  |
// +------+------+                           +-------+-------+
//        |                                          |
//        +---------------------+--------------------+
//                              |
//                       +------v------+
//                       | Build APK   |
//                       +------+------+
//                              |
//                           output
```

### Dynamic Dump / Logging Patch

```text
// Before
//
//             app start
//                |
//        +-------v--------+
//        | StartupLauncher |
//        | launch()        |
//        +-------+--------+
//                |
//        +-------v--------+
//        | VMRunner.invoke |
//        +-------+--------+
//                |
//              return
//
//
// After
//
//             app start
//                |
//        +-------v--------+
//        | StartupLauncher |
//        | launch()        |
//        +-------+--------+
//                |
//        +-------v--------+
//        | VMRunner.invoke |
//        +-------+--------+
//                |
//        +-------v--------+
//        | pairip()        |
//        | dump/log values |
//        +-------+--------+
//                |
//              return
```

### Static Translation Patch

```text
// Before
//
//          PairIP classes
//               |
//     +---------v----------+
//     | Obfuscated fields  |
//     | VM/runtime restore |
//     +---------+----------+
//               |
//        runtime lookup
//               |
//             return
//
//
// After
//
//          pairip.json
//               |
//     +---------v----------+
//     | TranslationData    |
//     | class -> fields    |
//     +---------+----------+
//               |
//     +---------v----------+
//     | TranslationRewriter|
//     | restore statically |
//     +---------+----------+
//               |
//     +---------v----------+
//     | Clean PairIP flow  |
//     | remove runtime lib |
//     +---------+----------+
//               |
//          translated APK
```

### Manifest Patch

```text
// AndroidManifest.xml cleanup
//
//     manifest/application
//              |
//   +----------v-----------+
//   | Remove split flags   |
//   | requiredSplitTypes   |
//   | splitTypes           |
//   | isSplitRequired      |
//   | isFeatureSplit       |
//   +----------+-----------+
//              |
//   +----------v-----------+
//   | Remove Play metadata |
//   | vending splits       |
//   | stamp source/type    |
//   +----------+-----------+
//              |
//   +----------v-----------+
//   | Remove PairIP license|
//   | activity/provider    |
//   | CHECK_LICENSE perm   |
//   +----------+-----------+
//              |
//       patched manifest
```

### Signature / Integrity Patch

```text
// Target methods
//
//   verifySignatureMatches()
//   verifyIntegrity()
//
//
// Before:
//
//       entry
//         |
//   +-----v------+
//   | original   |
//   | validation |
//   +-----+------+
//         |
//      result
//
//
// After:
//
//       entry
//         |
//   +-----v------+
//   | fixed safe |
//   | return     |
//   +-----+------+
//         |
//   true / void / null
```

### Class Rewrite Map

```text
// Translation mode class handling
//
//   Lcom/pairip/Application;          -> clean constructor + clinit
//   Lcom/pairip/StartupLauncher;      -> launch + restoreString + restoreMethod
//   Lcom/pairip/VMRunner;             -> private constructor + invoke returns null
//   Lcom/pairip/licensecheck3/...;    -> clean constructor + no-op activity hook
//   Other com/pairip/* classes        -> removed unless explicitly kept
```

---

## Project Structure

```text
RePairip/
├── antik/
│   ├── build.gradle.kts
│   └── src/main/
│       ├── java/com/antik/
│       │   ├── Main.java
│       │   ├── AntikUtils.java
│       │   ├── DexPatcher/
│       │   │   ├── DexPatcher.java
│       │   │   ├── MethodT/
│       │   │   ├── PairipMethodMake/
│       │   │   └── Translation/
│       │   ├── crc32/
│       │   ├── manifest/
│       │   ├── ui/
│       │   └── utils/
│       └── resources/
│           ├── log.dex
│           ├── restoreMethod.dex
│           └── info.txt
├── gradle/
├── preview/
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

---

## Important Source Files

| File | Purpose |
| --- | --- |
| `Main.java` | CLI argument parsing and main APK/APKS workflow |
| `manifestP.java` | Manifest split/license metadata cleanup |
| `DexPatcher.java` | Dynamic logging dex patch flow |
| `patchLauncher.java` | Rebuilds `StartupLauncher` and creates `pairip()` |
| `patchLaunchMethod.java` | Inserts `StartupLauncher.pairip()` after `VMRunner.invoke()` |
| `patchM.java` | Rewrites signature/integrity methods |
| `TranslationPatcher.java` | Static translation patch workflow |
| `TranslationData.java` | Parses and normalizes `pairip.json` |
| `TranslationRewriter.java` | Rewrites known PairIP classes |
| `crc32.java` | Copies dex CRC values from merged source APK to patched APK |

---

## Dependencies

| Dependency | Use |
| --- | --- |
| `org.smali:dexlib2` | Read and write dex classes/methods/instructions |
| `com.github.REAndroid:ARSCLib` | Load, merge, patch, and write APK/APKS modules |
| `com.google.guava:guava` | Utility dependency |
| `com.gradleup.shadow` | Build the runnable fat jar |

---

## Notes

- RePairip writes generated APK files beside the input file.
- Temporary APKS extraction folders are deleted after processing.
- APK signing is not handled by this tool. Sign the rebuilt APK with your preferred Android signing workflow.
- Results can vary by PairIP version and app build layout.
- If you find a bug, open an issue with the input type, command, stack trace, and expected behavior.

---

## Contributing

Contributions are welcome. Good areas for improvement include:

- better PairIP version detection
- stronger translation JSON validation
- more detailed error messages
- test APK fixtures
- signing workflow documentation
- additional dex rewrite strategies

Please keep patches focused and include enough detail to reproduce any bug you fix.

---

## License

This project is distributed under the Apache License 2.0.

```text
Apache License
Version 2.0, January 2004
https://www.apache.org/licenses/
```

Copyright (C) 2026 HighCapable
