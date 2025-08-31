package org.tabata.timber.core.di

import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.tabata.timber.core.audio.AudioManager
import org.tabata.timber.core.audio.IOSAudioManager
import org.tabata.timber.core.audio.AudioEventHandler
import org.tabata.timber.core.audio.DefaultAudioEventHandler
import org.tabata.timber.core.audio.TimerAudioIntegration
import org.tabata.timber.core.timer.TimerEngine
import org.tabata.timber.core.timer.IOSTimerEngine
import org.tabata.timber.core.subscription.SubscriptionManager
import org.tabata.timber.core.subscription.IOSSubscriptionManager

/**
 * iOS-specific Koin module
 * Contains iOS implementations of platform-specific interfaces
 */
actual val platformModule = module {
    
    // Audio management
    singleOf(::IOSAudioManager) bind AudioManager::class
    
    // Audio event handling
    single<AudioEventHandler> { DefaultAudioEventHandler(get()) }
    
    // Timer-Audio integration
    single { TimerAudioIntegration(get(), get()) }
    
    // Timer engine
    singleOf(::IOSTimerEngine) bind TimerEngine::class
    
    // Subscription management
    singleOf(::IOSSubscriptionManager) bind SubscriptionManager::class
}