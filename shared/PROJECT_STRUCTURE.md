# Tabata Timer Pro - Project Structure

## Overview

This document describes the project structure and core interfaces implemented for the Tabata Timer Pro application. The project follows Clean Architecture principles with Kotlin Multiplatform Mobile (KMP) for shared business logic.

## Directory Structure

```
shared/src/
├── commonMain/kotlin/org/tabata/timber/
│   ├── core/
│   │   ├── audio/              # Audio management interfaces
│   │   ├── di/                 # Dependency injection modules
│   │   ├── subscription/       # Subscription and feature gating
│   │   └── timer/              # Timer engine interfaces
│   ├── data/
│   │   ├── database/           # Database layer (future)
│   │   └── repositories/       # Repository implementations (future)
│   ├── domain/
│   │   ├── models/             # Domain data models
│   │   ├── repositories/       # Repository interfaces
│   │   └── usecases/           # Use case interfaces (future)
│   └── platform/               # Platform-specific expect declarations
├── androidMain/kotlin/org/tabata/timber/
│   ├── core/di/                # Android DI modules
│   └── platform/               # Android implementations
└── iosMain/kotlin/org/tabata/timber/
    ├── core/di/                # iOS DI modules
    └── platform/               # iOS implementations
```

## Core Interfaces

### TimerEngine
- **Location**: `core/timer/TimerEngine.kt`
- **Purpose**: Core timer functionality with precise timing and drift correction
- **Key Features**:
  - Reactive state management with StateFlow
  - Background time correction
  - Phase management (warmup, work, rest, set break, cooldown)

### AudioManager
- **Location**: `core/audio/AudioManager.kt`
- **Purpose**: Cross-platform audio management
- **Key Features**:
  - Text-to-speech with language fallback
  - Sound effects and music ducking
  - Platform-specific implementations (Android TTS vs iOS AVSpeechSynthesizer)

### SubscriptionManager
- **Location**: `core/subscription/SubscriptionManager.kt`
- **Purpose**: Premium subscription management
- **Key Features**:
  - Cross-platform subscription handling (Google Play Billing vs StoreKit 2)
  - Grace period management
  - Purchase restoration

### FeatureGateManager
- **Location**: `core/subscription/FeatureGateManager.kt`
- **Purpose**: Controls access to premium features
- **Key Features**:
  - Free tier limitations (1 preset, basic audio)
  - Premium feature unlocking
  - Usage tracking and upgrade prompts

## Repository Interfaces

### PresetRepository
- **Location**: `domain/repositories/PresetRepository.kt`
- **Purpose**: Timer preset management
- **Key Features**:
  - CRUD operations for presets
  - JSON import/export with validation
  - Batch import limitations for security

### WorkoutHistoryRepository
- **Location**: `domain/repositories/WorkoutHistoryRepository.kt`
- **Purpose**: Workout session tracking and statistics
- **Key Features**:
  - Session history management
  - Statistics calculation (streaks, averages)
  - Date range filtering

## Domain Models

### TimerConfiguration
- Workout session configuration (durations, rounds, sets)
- Audio settings integration
- Validation constraints

### TimerState
- Current timer status and progress
- Phase information and remaining time
- Round and set counters

### AudioSettings
- TTS and sound effect preferences
- Language and volume settings
- Music ducking configuration

## Dependency Injection

### Architecture
- **Framework**: Koin (lightweight DI for Kotlin Multiplatform)
- **Structure**: Modular approach with shared and platform-specific modules
- **Initialization**: Single `initKoin()` function callable from both platforms

### Modules
1. **coreModule**: Shared business logic dependencies
2. **repositoryModule**: Data layer dependencies (future)
3. **platformModule**: Platform-specific implementations (expect/actual)

### Platform Implementations
- **Android**: Uses Android TTS, Google Play Billing, MediaPlayer
- **iOS**: Uses AVSpeechSynthesizer, StoreKit 2, AVAudioPlayer

## Build Configuration

### Dependencies Added
- `koin-core`: Shared dependency injection
- `koin-test`: Testing utilities
- `koin-android`: Android-specific Koin features

### Version Catalog Updates
- Added Koin version (4.1.0) to `gradle/libs.versions.toml`
- Configured dependencies for shared and Android modules

## Implementation Status

### ✅ Completed
- [x] Project directory structure
- [x] Core interface definitions
- [x] Domain models and enums
- [x] Repository interfaces
- [x] Platform-specific expect/actual declarations
- [x] Dependency injection module structure
- [x] Build configuration with Koin dependencies

### 🚧 Prepared for Future Tasks
- [ ] Actual platform implementations (Android TTS, iOS AVSpeechSynthesizer)
- [ ] Database layer with SQLDelight
- [ ] Repository implementations
- [ ] Use case implementations
- [ ] Timer engine implementation
- [ ] Koin module activation (commented out pending dependency resolution)

## Next Steps

1. **Task 2**: Implement core data models and validation
2. **Task 3**: Set up database layer with SQLDelight
3. **Task 4**: Build core timer engine with drift correction
4. **Task 5**: Create audio management system
5. **Task 6**: Build subscription management system

## Notes

- Koin dependencies are temporarily commented out due to network connectivity issues during setup
- All interfaces compile successfully and follow the design document specifications
- Platform-specific implementations are stubbed and ready for actual implementation
- The structure supports the full feature set outlined in the requirements and design documents