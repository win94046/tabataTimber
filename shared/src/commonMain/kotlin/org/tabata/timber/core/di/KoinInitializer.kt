package org.tabata.timber.core.di

import org.koin.core.context.startKoin
import org.koin.dsl.KoinAppDeclaration

/**
 * Initializes Koin dependency injection for the shared module
 * This function should be called from each platform's application entry point
 */
fun initKoin(appDeclaration: KoinAppDeclaration = {}): org.koin.core.KoinApplication {
    return startKoin {
        appDeclaration()
        modules(
            commonModule,
            platformModule
        )
    }
}

/**
 * Helper function for shared configuration
 * Platforms can extend this with their own specific setup
 */
fun getSharedModules() = listOf(
    commonModule,
    platformModule
)