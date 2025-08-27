package org.tabata.timber.core.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.tabata.timber.data.repositories.TimerRepositoryImpl
import org.tabata.timber.domain.repositories.TimerRepository
import org.tabata.timber.domain.usecases.StartTabataUseCase

/**
 * Common Koin module containing shared dependencies
 * These dependencies are available across all platforms
 */
val commonModule = module {
    
    // Repositories
    singleOf(::TimerRepositoryImpl) bind TimerRepository::class
    
    // Use Cases
    singleOf(::StartTabataUseCase)
}