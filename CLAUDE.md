# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is an Android application named "Squat" built with Kotlin and Jetpack Compose. The project follows standard Android project structure and uses modern Android development practices.

- **Package**: `com.chaeyk.squat`
- **Build System**: Gradle with Kotlin DSL
- **UI Framework**: Jetpack Compose with Material 3
- **Target SDK**: 36 (compileSdk and targetSdk)
- **Min SDK**: 21

�� ���α׷����� ������� ���� ����Ʈ ��� �����ִ� �����̴�.
���α׷��� ���۵Ǹ� ī�޶� �Ѽ� ��ϴ� ����� �Կ��ϱ� �����Ѵ�.
����� �� �� �ɾҴٰ� �Ͼ�� �������� 1ȸ �� ���̸�, ����Ʈ Ƚ���� ȭ�鿡 ǥ�����ش�.

## Development Commands

### Build Commands
```bash
# Build the project
./gradlew build

# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Clean build
./gradlew clean
```

### Testing Commands
```bash
# Run unit tests
./gradlew test

# Run instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "com.chaeyk.squat.ExampleUnitTest"

# Run all tests
./gradlew check
```

### Development Commands
```bash
# Install debug APK to device/emulator
./gradlew installDebug

# Lint the code
./gradlew lint

# Check dependencies
./gradlew dependencies
```

## Project Architecture

### Main Components
- **MainActivity.kt**: Single activity using Jetpack Compose
- **UI Theme**: Custom Material 3 theme with dark/light mode support and dynamic colors
- **Package Structure**: Standard Android package structure under `com.chaeyk.squat`

### Key Files
- `app/build.gradle.kts`: Module-level build configuration
- `build.gradle.kts`: Project-level build configuration  
- `gradle/libs.versions.toml`: Version catalog for dependency management
- `app/src/main/AndroidManifest.xml`: App manifest and configuration

### Dependencies
The project uses modern Android libraries managed through version catalog:
- Jetpack Compose BOM for UI components
- Material 3 for design system
- Activity Compose for integration
- Core KTX for Kotlin extensions
- JUnit and Espresso for testing

### Testing Setup
- Unit tests in `app/src/test/` using JUnit
- Instrumented tests in `app/src/androidTest/` using AndroidJUnit4 and Espresso
- Test runner: AndroidJUnitRunner
