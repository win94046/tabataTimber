# Implementation Plan

- [ ] 1. Set up project structure and core interfaces
  - Create directory structure for domain models, repositories, and use cases in shared module
  - Define core interfaces for TimerEngine, AudioManager, and SubscriptionManager
  - Set up dependency injection with Koin for shared dependencies
  - _Requirements: 1.1, 1.2_

- [ ] 2. Implement core data models and validation
  - [ ] 2.1 Create timer configuration and state data models
    - Write TimerConfiguration, TimerState, and PhaseType data classes
    - Implement validation functions for timer durations and counts
    - Create unit tests for data model validation and edge cases
    - _Requirements: 1.1, 1.2, 1.5_

  - [ ] 2.2 Implement subscription and feature access models
    - Write SubscriptionStatus, PremiumFeature enum, and FeatureLimitStatus classes
    - Create FeatureGateManager interface and basic implementation
    - Write unit tests for feature access logic and subscription validation
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ] 2.3 Create weight tracking data models
    - Write WeightEntry, WeightStats, and BMICategory data classes
    - Implement BMI calculation and category classification functions
    - Create unit tests for weight validation and BMI calculations
    - _Requirements: 5.5_

- [ ] 3. Set up database layer with SQLDelight
  - [ ] 3.1 Create database schema with validation and migration strategy
    - Write SQLDelight schema with CHECK constraints for reasonable value ranges (weight 20-400kg, height 100-240cm)
    - Define SQL queries for CRUD operations with input validation
    - Create comprehensive database migration scripts for schema versioning (v1 to v2+ examples)
    - Add database repair and recovery mechanisms for corruption scenarios
    - Test migration scripts with sample data to ensure no data loss
    - _Requirements: 4.1, 4.2, 5.1, 5.2, 5.5_

  - [ ] 3.2 Implement repository interfaces and implementations
    - Create PresetRepository, WorkoutHistoryRepository, and WeightTrackingRepository interfaces
    - Implement SQLDelight-based repository classes with error handling
    - Write unit tests for repository operations using in-memory database
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.5_

- [ ] 4. Build core timer engine
  - [ ] 4.1 Implement precise timing system with drift correction
    - Create TimerEngine interface using monotonic clock (System.currentTimeMillis()) instead of delay()
    - Implement drift correction mechanism with <100ms tolerance and automatic adjustment
    - Add background time correction for iOS foreground return scenarios
    - Write comprehensive unit tests for timer accuracy over 30-60 minute periods
    - Test timer precision with <50ms accuracy requirement over extended periods
    - _Requirements: 1.1, 1.2, 1.5_

  - [ ] 4.2 Create phase management system
    - Implement PhaseManager for calculating next phases and transitions
    - Add phase color coding and display name logic
    - Write unit tests for phase progression through complete workout cycles
    - _Requirements: 1.1, 1.2, 2.2_

  - [ ] 4.3 Add timer control operations
    - Implement start, pause, resume, stop, and skip functionality
    - Add validation for timer operations based on current state
    - Write unit tests for all timer control scenarios and edge cases
    - _Requirements: 1.1, 1.5_

- [ ] 5. Create audio management system
  - [ ] 5.1 Implement platform-specific audio interfaces with ducking testing
    - Create AudioManager interface with TTS and sound effect methods
    - Implement expect/actual declarations for platform-specific audio handling (Android TTS vs iOS AVSpeechSynthesizer)
    - Add music ducking functionality with testing across different music apps (Spotify, Apple Music)
    - Create FakeAudioManager for testing and fallback scenarios
    - Test audio conflicts and ensure graceful degradation when audio fails
    - _Requirements: 2.3, 2.4, 2.5, 3.1, 3.2, 3.3_

  - [ ] 5.2 Add audio configuration with language fallback validation
    - Create AudioSettings data class with TTS, sound effects, and vibration options
    - Implement language support validation with fallback chain (requested → en-US → en-GB → en)
    - Add TTS language availability checking and automatic fallback notification to users
    - Implement audio feature validation based on subscription status
    - Write unit tests for audio configuration, feature gating, and language fallback scenarios
    - Test unsupported language handling and fallback quality assessment
    - _Requirements: 2.3, 2.4, 2.5, 8.4, 10.1, 10.2_

- [ ] 6. Build subscription management system
  - [ ] 6.1 Implement subscription status tracking with optional server verification
    - Create SubscriptionManager interface with status checking and purchase methods
    - Implement optional server verification for StoreKit receipts and Google Play purchase tokens
    - Add fallback to local validation with cryptographic signature checking when server unavailable
    - Implement subscription status persistence and validation with grace period logic
    - Add cross-platform subscription sync handling (StoreKit vs Google Play Billing differences)
    - Create restore purchase mechanism with retry logic and user guidance
    - Test subscription expiry scenarios, server unavailability fallback, and graceful downgrade to free tier
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ] 6.2 Create feature access control with graceful degradation
    - Implement FeatureGateManager with premium feature validation
    - Add free tier limitations (single preset, basic audio effects) with clear user feedback
    - Create upgrade prompts and premium feature discovery
    - Implement graceful downgrade preserving user data when subscription expires
    - Test feature gating edge cases and subscription restoration flows
    - _Requirements: 10.1, 10.2, 10.5_

- [ ] 7. Develop core use cases
  - [ ] 7.1 Create timer management use cases
    - Implement StartTimerUseCase, PauseTimerUseCase, and StopTimerUseCase
    - Add GetTimerStateUseCase for reactive state observation
    - Write unit tests for use case business logic and error handling
    - _Requirements: 1.1, 1.2, 1.5_

  - [ ] 7.2 Implement preset management use cases
    - Create SavePresetUseCase, LoadPresetUseCase, and DeletePresetUseCase
    - Add ExportPresetUseCase and ImportPresetUseCase with JSON serialization
    - Write unit tests for preset operations and validation
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [ ] 7.3 Create workout history use cases
    - Implement SaveWorkoutSessionUseCase and GetWorkoutHistoryUseCase
    - Add GetWorkoutStatsUseCase for statistics calculation
    - Write unit tests for workout tracking and statistics generation
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 8. Build Compose UI foundation
  - [ ] 8.1 Create design system components
    - Implement color themes with phase-specific colors and dark/light mode support
    - Create typography system with large, accessible fonts for timer displays
    - Build reusable UI components (buttons, cards, progress indicators)
    - _Requirements: 2.1, 2.2, 8.1, 8.2, 8.3_

  - [ ] 8.2 Implement navigation structure
    - Set up Compose Navigation with screen destinations
    - Create navigation graph for Home, Timer, Presets, History, and Settings screens
    - Add deep linking support for preset imports
    - _Requirements: 4.5, 8.4_

- [ ] 9. Develop main timer screen
  - [ ] 9.1 Create timer display UI
    - Build large timer display with circular progress indicator
    - Implement phase indicator with color coding and remaining time
    - Add round/set counters and workout progress visualization
    - _Requirements: 2.1, 2.2_

  - [ ] 9.2 Add timer control interface
    - Create play/pause/stop/skip buttons with proper state management
    - Implement timer configuration selection and quick start options
    - Add accessibility support with proper content descriptions
    - _Requirements: 1.5, 2.1, 8.5_

  - [ ] 9.3 Integrate audio and vibration feedback
    - Connect AudioManager to timer state changes for TTS and sound effects
    - Add vibration feedback for phase transitions
    - Implement audio settings integration with real-time updates
    - _Requirements: 2.3, 2.4, 2.5_

- [ ] 10. Build preset management screens
  - [ ] 10.1 Create preset list and selection UI
    - Implement preset list screen with search and filtering
    - Add preset preview with duration and phase information
    - Create quick start functionality from preset list
    - _Requirements: 4.1, 4.2_

  - [ ] 10.2 Develop preset creation and editing
    - Build preset configuration form with duration inputs and validation
    - Add audio settings configuration within preset editor
    - Implement preset saving with name validation and duplicate handling
    - _Requirements: 4.1, 4.3, 4.6_

  - [ ] 10.3 Add secure preset export and import with batch limitations
    - Create export functionality with JSON generation and sharing
    - Implement import validation with 10KB size limit, 50 preset batch limit, and field whitelist security
    - Add malicious payload detection (excessive nesting, long field names, suspicious patterns)
    - Implement batch import restrictions to prevent system overload from mass imports
    - Create partial import capability with error reporting and warnings
    - Add field name length validation and nested structure depth limits
    - Add preset sharing UI with platform-specific sharing options
    - Test with malformed JSON, oversized files, batch overflow, and malicious payload scenarios
    - _Requirements: 4.4, 4.5_

- [ ] 11. Implement workout history and statistics
  - [ ] 11.1 Create workout history display
    - Build workout history list with date, duration, and completion status
    - Add filtering by date range and preset type
    - Implement workout session details view
    - _Requirements: 5.1, 5.2_

  - [ ] 11.2 Develop statistics dashboard
    - Create workout statistics with weekly, monthly, and total summaries
    - Add streak tracking and frequency metrics visualization
    - Implement progress charts for workout consistency
    - _Requirements: 5.3, 5.4_

- [ ] 12. Build weight tracking system (Premium feature)
  - [ ] 12.1 Create weight entry interface with validation
    - Build weight input form with strict validation (20-400kg weight, 100-240cm height)
    - Add BMI calculation with reasonable range validation (10-60 BMI)
    - Create weight history list with trend visualization and data conflict resolution
    - Implement weight goal setting and progress tracking
    - Add input sanitization and error handling for invalid data ranges
    - _Requirements: 5.5_

  - [ ] 12.2 Integrate with health platforms
    - Connect weight tracking with HealthDataManager for platform sync
    - Add automatic weight import from health apps when available
    - Implement data export to health platforms
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 13. Develop settings and customization
  - [ ] 13.1 Create app settings interface
    - Build settings screen with theme, language, and notification preferences
    - Add audio settings configuration with feature gating for premium options
    - Implement settings persistence and validation
    - _Requirements: 8.1, 8.2, 8.4, 8.5_

  - [ ] 13.2 Add reminder and notification settings
    - Create workout reminder configuration with time and frequency options
    - Implement notification scheduling and management
    - Add reminder customization with motivational messages
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 14. Implement background execution
  - [ ] 14.1 Create platform-specific background task managers with iOS limitations
    - Implement Android foreground service with partial wake lock and notification channels
    - Create iOS local notification scheduling strategy (cannot maintain continuous background execution)
    - Add iOS foreground return correction mechanism to adjust timer state after background time
    - Implement background execution permission requests and user guidance
    - Test background execution across different Android versions and battery optimization settings
    - _Requirements: 2.6_

  - [ ] 14.2 Add lock screen and notification integration with state correction
    - Create persistent notification with timer status and controls
    - Implement lock screen widget with current phase and time remaining
    - Add notification action buttons for pause/resume/stop with state synchronization
    - Create iOS background time calculation and timer state correction on foreground return
    - Test notification interactions and state consistency across platforms
    - _Requirements: 2.6_

- [ ] 15. Build platform-specific integrations
  - [ ] 15.1 Implement health platform connections with granular permission handling
    - Create iOS HealthKit integration for workout and weight data with individual permission requests
    - Implement Android Google Fit integration with activity tracking and fine-grained permission control
    - Add granular health data permission handling (workout, heart rate, weight, BMI separately)
    - Create partial permission scenarios handling (e.g., workout allowed, weight denied)
    - Implement UI for explaining missing features when permissions are partially granted
    - Add health data permission retry prompts and settings redirect for denied permissions
    - Create exponential backoff retry mechanism for failed sync operations
    - Implement data conflict resolution using timestamp-based "latest wins" strategy
    - Add permission denial recovery UI with actionable user guidance
    - Test sync failure scenarios, partial permission flows, and offline data queuing
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

  - [ ] 15.2 Add subscription platform integration
    - Implement iOS StoreKit 2 integration for subscription management
    - Create Android Google Play Billing integration with purchase validation
    - Add subscription restoration and cross-platform sync
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 16. Develop wearable companion features
  - [ ] 16.1 Create wearable communication layer with offline queue compensation
    - Implement WearableManager interface with platform-specific implementations
    - Add timer state synchronization with "last write wins" conflict resolution strategy
    - Create wearable command handling with priority system (Stop > Pause > Resume > Skip)
    - Implement offline operation queue for when wearable is disconnected
    - Add automatic sync of queued operations when wearable reconnects
    - Implement timestamp-based state synchronization to handle simultaneous operations
    - Add connection status monitoring and automatic reconnection attempts
    - Create offline operation conflict resolution for queued commands vs current state
    - Test simultaneous command scenarios, disconnection/reconnection flows, and offline queue processing
    - Test wearable offline scenarios with command queuing and compensation sync
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

  - [ ] 16.2 Build wearable UI components
    - Create Apple Watch complications and app interface
    - Implement Wear OS tiles and app screens
    - Add haptic feedback and display optimization for small screens
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 17. Add comprehensive error handling and recovery
  - [ ] 17.1 Implement comprehensive error handling with fallback mechanisms
    - Create timer failure fallback to foreground-only mode with user notification
    - Add database error recovery using in-memory storage with automatic repair attempts
    - Implement health sync error handling with offline queuing and retry logic
    - Create subscription validation fallback with grace period and offline validation
    - Add audio failure graceful degradation maintaining visual feedback
    - Test all error scenarios and ensure no complete feature failures
    - _Requirements: All requirements - error handling_

  - [ ] 17.2 Add user feedback with actionable recovery steps
    - Create user-friendly error messages with specific actionable steps ("Enable background app refresh in Settings")
    - Implement retry mechanisms with exponential backoff for network operations
    - Add diagnostic information and troubleshooting guides for common issues
    - Create error recovery UI with clear next steps and alternative options
    - Test error message clarity and recovery flow effectiveness
    - _Requirements: All requirements - user experience_

- [ ] 18. Implement comprehensive testing suite
  - [ ] 18.1 Create comprehensive unit tests with coverage targets
    - Write tests for timer engine accuracy (95% coverage) including drift correction and long-duration scenarios
    - Add tests for subscription logic and feature gating (90% coverage) including expiry and restore flows
    - Create tests for data validation and repository operations (85% coverage) including migration scripts
    - Test JSON import validation, weight range validation, and error handling scenarios
    - Achieve minimum 80% overall coverage with core modules exceeding 90%
    - _Requirements: All requirements - testing_

  - [ ] 18.2 Build comprehensive integration tests
    - Create end-to-end timer workflow tests (Preset → Timer → Session → History)
    - Add database integration tests with real SQLDelight database and migration testing
    - Implement platform integration tests for audio, background execution, and health sync
    - Test cross-platform subscription sync and wearable state synchronization
    - Create 30-60 minute continuous timer accuracy tests with <100ms drift tolerance
    - _Requirements: All requirements - integration testing_

  - [ ] 18.3 Add UI, accessibility, and performance tests
    - Create Compose UI tests for all screens and user interactions including error states
    - Add accessibility tests for screen readers, high contrast mode, and large text (200% scaling)
    - Implement performance tests for timer accuracy, memory usage (60+ minute sessions), and battery consumption
    - Test UI responsiveness (<16ms frame times) during timer updates
    - Create stress tests with large datasets (1000+ presets, 10000+ sessions)
    - _Requirements: All requirements - UI/UX testing_

- [ ] 19. Optimize performance and finalize
  - [ ] 19.1 Performance optimization
    - Optimize timer precision and reduce battery consumption
    - Implement memory leak detection and resolution
    - Add performance monitoring for database operations
    - _Requirements: All requirements - performance_

  - [ ] 19.2 Final integration and polish
    - Integrate all components and test complete user workflows
    - Add final UI polish and animation improvements
    - Implement app icon, splash screen, and store listing assets
    - _Requirements: All requirements - final integration_