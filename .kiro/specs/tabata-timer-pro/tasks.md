# Implementation Plan

- [x] 1. Set up project structure and core interfaces
  - **Use context7 to research**: Kotlin Multiplatform project structure best practices, Koin dependency injection patterns
  - **Use sequential-thinking to plan**: Directory structure organization, interface design patterns, and dependency management strategy
  - Create directory structure for domain models, repositories, and use cases in shared module
  - Define core interfaces for TimerEngine, AudioManager, and SubscriptionManager
  - Set up dependency injection with Koin for shared dependencies
  - _Requirements: 1.1, 1.2_

- [x] 2. Implement core data models and validation
  - [x] 2.1 Create timer configuration and state data models
    - **Use context7 to research**: Kotlin data class best practices, validation patterns, and testing frameworks
    - **Use sequential-thinking to design**: Data model structure, validation logic flow, and test case scenarios
    - Write TimerConfiguration, TimerState, and PhaseType data classes
    - Implement validation functions for timer durations and counts
    - Create unit tests for data model validation and edge cases
    - _Requirements: 1.1, 1.2, 1.5_

  - [x] 2.2 Implement subscription and feature access models
    - **Use context7 to research**: Mobile app subscription models, feature gating patterns, and premium tier implementations
    - **Use sequential-thinking to analyze**: Feature access control logic, subscription state management, and validation strategies
    - Write SubscriptionStatus, PremiumFeature enum, and FeatureLimitStatus classes
    - Create FeatureGateManager interface and basic implementation
    - Write unit tests for feature access logic and subscription validation
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [x] 2.3 Create weight tracking data models
    - **Use context7 to research**: Health data modeling best practices, BMI calculation standards, and medical data validation
    - **Use sequential-thinking to plan**: Weight tracking data structure, BMI categorization logic, and validation rules
    - Write WeightEntry, WeightStats, and BMICategory data classes
    - Implement BMI calculation and category classification functions
    - Create unit tests for weight validation and BMI calculations
    - _Requirements: 5.5_

- [ ] 3. Set up database layer with SQLDelight
  - [ ] 3.1 Create database schema with validation and migration strategy
    - **Use context7 to research**: SQLDelight best practices, database migration strategies, and mobile database optimization
    - **Use sequential-thinking to design**: Schema structure, constraint validation, migration flow, and recovery mechanisms
    - Write SQLDelight schema with CHECK constraints for reasonable value ranges (weight 20-400kg, height 100-240cm)
    - Define SQL queries for CRUD operations with input validation
    - Create comprehensive database migration scripts for schema versioning (v1 to v2+ examples)
    - Add database repair and recovery mechanisms for corruption scenarios
    - Test migration scripts with sample data to ensure no data loss
    - _Requirements: 4.1, 4.2, 5.1, 5.2, 5.5_

  - [ ] 3.2 Implement repository interfaces and implementations
    - **Use context7 to research**: Repository pattern implementations, SQLDelight integration patterns, and error handling strategies
    - **Use sequential-thinking to architect**: Repository interface design, implementation patterns, and testing approaches
    - Create PresetRepository, WorkoutHistoryRepository, and WeightTrackingRepository interfaces
    - Implement SQLDelight-based repository classes with error handling
    - Write unit tests for repository operations using in-memory database
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 5.1, 5.2, 5.5_

- [ ] 4. Build core timer engine
  - [ ] 4.1 Implement precise timing system with drift correction
    - **Use context7 to research**: High-precision timer implementations, drift correction algorithms, and mobile timing best practices
    - **Use sequential-thinking to solve**: Timer accuracy challenges, drift correction logic, and cross-platform timing issues
    - Create TimerEngine interface using monotonic clock (System.currentTimeMillis()) instead of delay()
    - Implement drift correction mechanism with <100ms tolerance and automatic adjustment
    - Add background time correction for iOS foreground return scenarios
    - Write comprehensive unit tests for timer accuracy over 30-60 minute periods
    - Test timer precision with <50ms accuracy requirement over extended periods
    - _Requirements: 1.1, 1.2, 1.5_

  - [ ] 4.2 Create phase management system
    - **Use context7 to research**: State machine patterns, phase transition algorithms, and workout timer implementations
    - **Use sequential-thinking to design**: Phase progression logic, state transitions, and color coding systems
    - Implement PhaseManager for calculating next phases and transitions
    - Add phase color coding and display name logic
    - Write unit tests for phase progression through complete workout cycles
    - _Requirements: 1.1, 1.2, 2.2_

  - [ ] 4.3 Add timer control operations
    - **Use context7 to research**: Timer control patterns, state validation strategies, and user interaction handling
    - **Use sequential-thinking to analyze**: Control operation flows, state validation logic, and edge case scenarios
    - Implement start, pause, resume, stop, and skip functionality
    - Add validation for timer operations based on current state
    - Write unit tests for all timer control scenarios and edge cases
    - _Requirements: 1.1, 1.5_

- [ ] 5. Create audio management system
  - [ ] 5.1 Implement platform-specific audio interfaces with ducking testing
    - **Use context7 to research**: Kotlin Multiplatform audio handling, TTS implementations, and audio ducking techniques
    - **Use sequential-thinking to plan**: Cross-platform audio architecture, ducking strategies, and fallback mechanisms
    - Create AudioManager interface with TTS and sound effect methods
    - Implement expect/actual declarations for platform-specific audio handling (Android TTS vs iOS AVSpeechSynthesizer)
    - Add music ducking functionality with testing across different music apps (Spotify, Apple Music)
    - Create FakeAudioManager for testing and fallback scenarios
    - Test audio conflicts and ensure graceful degradation when audio fails
    - _Requirements: 2.3, 2.4, 2.5, 3.1, 3.2, 3.3_

  - [ ] 5.2 Add audio configuration with language fallback validation
    - **Use context7 to research**: TTS language support, audio configuration patterns, and internationalization best practices
    - **Use sequential-thinking to design**: Language fallback chains, configuration validation, and user notification strategies
    - Create AudioSettings data class with TTS, sound effects, and vibration options
    - Implement language support validation with fallback chain (requested → en-US → en-GB → en)
    - Add TTS language availability checking and automatic fallback notification to users
    - Implement audio feature validation based on subscription status
    - Write unit tests for audio configuration, feature gating, and language fallback scenarios
    - Test unsupported language handling and fallback quality assessment
    - _Requirements: 2.3, 2.4, 2.5, 8.4, 10.1, 10.2_

- [ ] 6. Build subscription management system
  - [ ] 6.1 Implement subscription status tracking with optional server verification
    - **Use context7 to research**: Mobile subscription management, StoreKit 2, Google Play Billing, and receipt validation
    - **Use sequential-thinking to architect**: Subscription verification flow, fallback mechanisms, and cross-platform sync strategies
    - Create SubscriptionManager interface with status checking and purchase methods
    - Implement optional server verification for StoreKit receipts and Google Play purchase tokens
    - Add fallback to local validation with cryptographic signature checking when server unavailable
    - Implement subscription status persistence and validation with grace period logic
    - Add cross-platform subscription sync handling (StoreKit vs Google Play Billing differences)
    - Create restore purchase mechanism with retry logic and user guidance
    - Test subscription expiry scenarios, server unavailability fallback, and graceful downgrade to free tier
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

  - [ ] 6.2 Create feature access control with graceful degradation
    - **Use context7 to research**: Feature gating patterns, freemium models, and user experience best practices
    - **Use sequential-thinking to plan**: Feature access logic, degradation strategies, and upgrade flow design
    - Implement FeatureGateManager with premium feature validation
    - Add free tier limitations (single preset, basic audio effects) with clear user feedback
    - Create upgrade prompts and premium feature discovery
    - Implement graceful downgrade preserving user data when subscription expires
    - Test feature gating edge cases and subscription restoration flows
    - _Requirements: 10.1, 10.2, 10.5_

- [ ] 7. Develop core use cases
  - [ ] 7.1 Create timer management use cases
    - **Use context7 to research**: Clean Architecture use case patterns, reactive programming, and business logic testing
    - **Use sequential-thinking to design**: Use case interactions, state management flow, and error handling strategies
    - Implement StartTimerUseCase, PauseTimerUseCase, and StopTimerUseCase
    - Add GetTimerStateUseCase for reactive state observation
    - Write unit tests for use case business logic and error handling
    - _Requirements: 1.1, 1.2, 1.5_

  - [ ] 7.2 Implement preset management use cases
    - **Use context7 to research**: Data serialization patterns, JSON handling, and file import/export best practices
    - **Use sequential-thinking to plan**: Preset management workflow, validation logic, and serialization strategies
    - Create SavePresetUseCase, LoadPresetUseCase, and DeletePresetUseCase
    - Add ExportPresetUseCase and ImportPresetUseCase with JSON serialization
    - Write unit tests for preset operations and validation
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

  - [ ] 7.3 Create workout history use cases
    - **Use context7 to research**: Workout tracking patterns, statistics calculation algorithms, and data analytics approaches
    - **Use sequential-thinking to design**: History tracking logic, statistics computation, and data aggregation strategies
    - Implement SaveWorkoutSessionUseCase and GetWorkoutHistoryUseCase
    - Add GetWorkoutStatsUseCase for statistics calculation
    - Write unit tests for workout tracking and statistics generation
    - _Requirements: 5.1, 5.2, 5.3, 5.4_

- [ ] 8. Build Compose UI foundation
  - [ ] 8.1 Create design system components
    - **Use context7 to research**: Jetpack Compose design systems, Material Design 3, and accessibility guidelines
    - **Use sequential-thinking to plan**: Design token structure, component hierarchy, and theming architecture
    - Implement color themes with phase-specific colors and dark/light mode support
    - Create typography system with large, accessible fonts for timer displays
    - Build reusable UI components (buttons, cards, progress indicators)
    - _Requirements: 2.1, 2.2, 8.1, 8.2, 8.3_

  - [ ] 8.2 Implement navigation structure
    - **Use context7 to research**: Compose Navigation patterns, deep linking implementation, and navigation architecture
    - **Use sequential-thinking to design**: Navigation flow, screen transitions, and deep linking strategies
    - Set up Compose Navigation with screen destinations
    - Create navigation graph for Home, Timer, Presets, History, and Settings screens
    - Add deep linking support for preset imports
    - _Requirements: 4.5, 8.4_

- [ ] 9. Develop main timer screen
  - [ ] 9.1 Create timer display UI
    - **Use context7 to research**: Compose custom drawing, circular progress implementations, and timer UI patterns
    - **Use sequential-thinking to design**: Visual hierarchy, progress visualization, and user interface layout
    - Build large timer display with circular progress indicator
    - Implement phase indicator with color coding and remaining time
    - Add round/set counters and workout progress visualization
    - _Requirements: 2.1, 2.2_

  - [ ] 9.2 Add timer control interface
    - **Use context7 to research**: Compose accessibility, button state management, and user interaction patterns
    - **Use sequential-thinking to plan**: Control interface layout, state transitions, and accessibility implementation
    - Create play/pause/stop/skip buttons with proper state management
    - Implement timer configuration selection and quick start options
    - Add accessibility support with proper content descriptions
    - _Requirements: 1.5, 2.1, 8.5_

  - [ ] 9.3 Integrate audio and vibration feedback
    - **Use context7 to research**: Audio-visual synchronization, haptic feedback patterns, and real-time audio updates
    - **Use sequential-thinking to coordinate**: Audio timing, vibration patterns, and settings integration
    - Connect AudioManager to timer state changes for TTS and sound effects
    - Add vibration feedback for phase transitions
    - Implement audio settings integration with real-time updates
    - _Requirements: 2.3, 2.4, 2.5_

- [ ] 10. Build preset management screens
  - [ ] 10.1 Create preset list and selection UI
    - **Use context7 to research**: Compose LazyColumn, search implementations, and filtering patterns
    - **Use sequential-thinking to design**: List layout, search functionality, and user interaction flow
    - Implement preset list screen with search and filtering
    - Add preset preview with duration and phase information
    - Create quick start functionality from preset list
    - _Requirements: 4.1, 4.2_

  - [ ] 10.2 Develop preset creation and editing
    - **Use context7 to research**: Form validation patterns, input handling, and user experience design
    - **Use sequential-thinking to plan**: Form structure, validation logic, and save/edit workflows
    - Build preset configuration form with duration inputs and validation
    - Add audio settings configuration within preset editor
    - Implement preset saving with name validation and duplicate handling
    - _Requirements: 4.1, 4.3, 4.6_

  - [ ] 10.3 Add secure preset export and import with batch limitations
    - **Use context7 to research**: JSON security best practices, file validation techniques, and mobile sharing patterns
    - **Use sequential-thinking to analyze**: Security threats, validation strategies, and import/export workflows
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
    - **Use context7 to research**: Data visualization patterns, date filtering UI, and history management interfaces
    - **Use sequential-thinking to design**: History layout, filtering mechanisms, and detail view structure
    - Build workout history list with date, duration, and completion status
    - Add filtering by date range and preset type
    - Implement workout session details view
    - _Requirements: 5.1, 5.2_

  - [ ] 11.2 Develop statistics dashboard
    - **Use context7 to research**: Data visualization libraries, chart implementations, and statistics calculation methods
    - **Use sequential-thinking to plan**: Dashboard layout, metrics calculation, and visualization strategies
    - Create workout statistics with weekly, monthly, and total summaries
    - Add streak tracking and frequency metrics visualization
    - Implement progress charts for workout consistency
    - _Requirements: 5.3, 5.4_

- [ ] 12. Build weight tracking system (Premium feature)
  - [ ] 12.1 Create weight entry interface with validation
    - **Use context7 to research**: Health data input patterns, validation techniques, and trend visualization methods
    - **Use sequential-thinking to design**: Input validation flow, BMI calculation logic, and progress tracking interface
    - Build weight input form with strict validation (20-400kg weight, 100-240cm height)
    - Add BMI calculation with reasonable range validation (10-60 BMI)
    - Create weight history list with trend visualization and data conflict resolution
    - Implement weight goal setting and progress tracking
    - Add input sanitization and error handling for invalid data ranges
    - _Requirements: 5.5_

  - [ ] 12.2 Integrate with health platforms
    - **Use context7 to research**: HealthKit integration, Google Fit API, and health data synchronization patterns
    - **Use sequential-thinking to plan**: Health platform integration, data sync strategies, and permission handling
    - Connect weight tracking with HealthDataManager for platform sync
    - Add automatic weight import from health apps when available
    - Implement data export to health platforms
    - _Requirements: 7.1, 7.2, 7.3, 7.4, 7.5_

- [ ] 13. Develop settings and customization
  - [ ] 13.1 Create app settings interface
    - **Use context7 to research**: Settings UI patterns, preference management, and configuration persistence
    - **Use sequential-thinking to organize**: Settings structure, validation logic, and user experience flow
    - Build settings screen with theme, language, and notification preferences
    - Add audio settings configuration with feature gating for premium options
    - Implement settings persistence and validation
    - _Requirements: 8.1, 8.2, 8.4, 8.5_

  - [ ] 13.2 Add reminder and notification settings
    - **Use context7 to research**: Mobile notification systems, scheduling patterns, and reminder best practices
    - **Use sequential-thinking to plan**: Notification scheduling logic, reminder customization, and user engagement strategies
    - Create workout reminder configuration with time and frequency options
    - Implement notification scheduling and management
    - Add reminder customization with motivational messages
    - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [ ] 14. Implement background execution
  - [ ] 14.1 Create platform-specific background task managers with iOS limitations
    - **Use context7 to research**: Android foreground services, iOS background limitations, and cross-platform background execution
    - **Use sequential-thinking to solve**: Background execution challenges, platform differences, and state synchronization issues
    - Implement Android foreground service with partial wake lock and notification channels
    - Create iOS local notification scheduling strategy (cannot maintain continuous background execution)
    - Add iOS foreground return correction mechanism to adjust timer state after background time
    - Implement background execution permission requests and user guidance
    - Test background execution across different Android versions and battery optimization settings
    - _Requirements: 2.6_

  - [ ] 14.2 Add lock screen and notification integration with state correction
    - **Use context7 to research**: Lock screen widgets, notification actions, and state synchronization patterns
    - **Use sequential-thinking to coordinate**: Notification design, state management, and cross-platform consistency
    - Create persistent notification with timer status and controls
    - Implement lock screen widget with current phase and time remaining
    - Add notification action buttons for pause/resume/stop with state synchronization
    - Create iOS background time calculation and timer state correction on foreground return
    - Test notification interactions and state consistency across platforms
    - _Requirements: 2.6_

- [ ] 15. Build platform-specific integrations
  - [ ] 15.1 Implement health platform connections with granular permission handling
    - **Use context7 to research**: HealthKit permissions, Google Fit integration, and health data privacy best practices
    - **Use sequential-thinking to architect**: Permission flow, data sync strategies, and conflict resolution mechanisms
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
    - **Use context7 to research**: StoreKit 2 implementation, Google Play Billing API, and subscription management patterns
    - **Use sequential-thinking to plan**: Subscription flow, validation logic, and cross-platform synchronization
    - Implement iOS StoreKit 2 integration for subscription management
    - Create Android Google Play Billing integration with purchase validation
    - Add subscription restoration and cross-platform sync
    - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [ ] 16. Develop wearable companion features
  - [ ] 16.1 Create wearable communication layer with offline queue compensation
    - **Use context7 to research**: Wearable communication protocols, offline synchronization patterns, and conflict resolution strategies
    - **Use sequential-thinking to design**: Communication architecture, queue management, and state synchronization logic
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
    - **Use context7 to research**: Apple Watch development, Wear OS design guidelines, and wearable UI best practices
    - **Use sequential-thinking to plan**: Wearable interface design, haptic patterns, and small screen optimization
    - Create Apple Watch complications and app interface
    - Implement Wear OS tiles and app screens
    - Add haptic feedback and display optimization for small screens
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [ ] 17. Add comprehensive error handling and recovery
  - [ ] 17.1 Implement comprehensive error handling with fallback mechanisms
    - **Use context7 to research**: Error handling patterns, fallback strategies, and resilient system design
    - **Use sequential-thinking to analyze**: Failure scenarios, recovery mechanisms, and graceful degradation strategies
    - Create timer failure fallback to foreground-only mode with user notification
    - Add database error recovery using in-memory storage with automatic repair attempts
    - Implement health sync error handling with offline queuing and retry logic
    - Create subscription validation fallback with grace period and offline validation
    - Add audio failure graceful degradation maintaining visual feedback
    - Test all error scenarios and ensure no complete feature failures
    - _Requirements: All requirements - error handling_

  - [ ] 17.2 Add user feedback with actionable recovery steps
    - **Use context7 to research**: User experience design, error messaging best practices, and recovery flow patterns
    - **Use sequential-thinking to design**: Error communication strategies, recovery workflows, and user guidance systems
    - Create user-friendly error messages with specific actionable steps ("Enable background app refresh in Settings")
    - Implement retry mechanisms with exponential backoff for network operations
    - Add diagnostic information and troubleshooting guides for common issues
    - Create error recovery UI with clear next steps and alternative options
    - Test error message clarity and recovery flow effectiveness
    - _Requirements: All requirements - user experience_

- [ ] 18. Implement comprehensive testing suite
  - [ ] 18.1 Create comprehensive unit tests with coverage targets
    - **Use context7 to research**: Testing frameworks, coverage tools, and testing best practices for Kotlin Multiplatform
    - **Use sequential-thinking to plan**: Test strategy, coverage targets, and testing scenarios
    - Write tests for timer engine accuracy (95% coverage) including drift correction and long-duration scenarios
    - Add tests for subscription logic and feature gating (90% coverage) including expiry and restore flows
    - Create tests for data validation and repository operations (85% coverage) including migration scripts
    - Test JSON import validation, weight range validation, and error handling scenarios
    - Achieve minimum 80% overall coverage with core modules exceeding 90%
    - _Requirements: All requirements - testing_

  - [ ] 18.2 Build comprehensive integration tests
    - **Use context7 to research**: Integration testing patterns, end-to-end testing frameworks, and cross-platform testing strategies
    - **Use sequential-thinking to design**: Test workflows, integration scenarios, and validation approaches
    - Create end-to-end timer workflow tests (Preset → Timer → Session → History)
    - Add database integration tests with real SQLDelight database and migration testing
    - Implement platform integration tests for audio, background execution, and health sync
    - Test cross-platform subscription sync and wearable state synchronization
    - Create 30-60 minute continuous timer accuracy tests with <100ms drift tolerance
    - _Requirements: All requirements - integration testing_

  - [ ] 18.3 Add UI, accessibility, and performance tests
    - **Use context7 to research**: Compose testing, accessibility testing tools, and performance testing methodologies
    - **Use sequential-thinking to plan**: UI test scenarios, accessibility requirements, and performance benchmarks
    - Create Compose UI tests for all screens and user interactions including error states
    - Add accessibility tests for screen readers, high contrast mode, and large text (200% scaling)
    - Implement performance tests for timer accuracy, memory usage (60+ minute sessions), and battery consumption
    - Test UI responsiveness (<16ms frame times) during timer updates
    - Create stress tests with large datasets (1000+ presets, 10000+ sessions)
    - _Requirements: All requirements - UI/UX testing_

- [ ] 19. Optimize performance and finalize
  - [ ] 19.1 Performance optimization
    - **Use context7 to research**: Mobile performance optimization, battery efficiency techniques, and memory management best practices
    - **Use sequential-thinking to analyze**: Performance bottlenecks, optimization strategies, and monitoring approaches
    - Optimize timer precision and reduce battery consumption
    - Implement memory leak detection and resolution
    - Add performance monitoring for database operations
    - _Requirements: All requirements - performance_

  - [ ] 19.2 Final integration and polish
    - **Use context7 to research**: App store guidelines, UI polish techniques, and launch preparation best practices
    - **Use sequential-thinking to coordinate**: Final integration steps, polish priorities, and launch readiness checklist
    - Integrate all components and test complete user workflows
    - Add final UI polish and animation improvements
    - Implement app icon, splash screen, and store listing assets
    - _Requirements: All requirements - final integration_
