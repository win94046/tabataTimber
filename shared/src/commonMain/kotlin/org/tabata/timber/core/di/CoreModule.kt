package org.tabata.timber.core.di

// TODO: Uncomment when Koin dependencies are available
// import org.koin.dsl.module
import org.tabata.timber.core.subscription.FeatureGateManager
import org.tabata.timber.core.subscription.FeatureGateManagerImpl
import org.tabata.timber.core.subscription.SubscriptionManager
import org.tabata.timber.core.timer.TimerEngine
import org.tabata.timber.domain.repositories.PresetRepository
import org.tabata.timber.domain.repositories.WorkoutHistoryRepository

/**
 * Core dependency injection module for shared components
 * TODO: Uncomment when Koin dependencies are available
 */
/*
val coreModule = module {
    // Feature gate manager depends on subscription manager
    single<FeatureGateManager> { 
        FeatureGateManagerImpl(get<SubscriptionManager>()) 
    }
}
*/

/**
 * Platform-specific module for platform implementations
 * This will be implemented differently for Android and iOS
 * TODO: Uncomment when Koin dependencies are available
 */
// expect val platformModule: org.koin.core.module.Module

/**
 * Repository module for data layer dependencies
 * Will be implemented when database layer is added
 * TODO: Uncomment when Koin dependencies are available
 */
/*
val repositoryModule = module {
    // Repository implementations will be added in later tasks
}
*/

/**
 * Aggregated application modules
 * TODO: Uncomment when Koin dependencies are available
 */
/*
fun appModules() = listOf(
    coreModule,
    repositoryModule,
    platformModule
)
*/