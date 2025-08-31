package org.tabata.timber.core.di

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module
import org.tabata.timber.core.audio.AudioManager
import org.tabata.timber.core.audio.AndroidAudioManager
import org.tabata.timber.core.audio.AudioEventHandler
import org.tabata.timber.core.audio.DefaultAudioEventHandler
import org.tabata.timber.core.audio.TimerAudioIntegration
import org.tabata.timber.core.timer.TimerEngine
import org.tabata.timber.core.timer.AndroidTimerEngine
import org.tabata.timber.core.subscription.SubscriptionManager
import org.tabata.timber.core.subscription.AndroidSubscriptionManager

/**
 * Android-specific Koin module
 * Contains Android implementations of platform-specific interfaces
 */
actual val platformModule = module {
    
    // Audio management
    single<AudioManager> { AndroidAudioManager(androidContext()) }
    
    // Audio event handling
    single<AudioEventHandler> { DefaultAudioEventHandler(get()) }
    
    // Timer-Audio integration
    single { TimerAudioIntegration(get(), get()) }
    
    // Timer engine
    singleOf(::AndroidTimerEngine) bind TimerEngine::class
    
    // Subscription management
    singleOf(::AndroidSubscriptionManager) bind SubscriptionManager::class
}