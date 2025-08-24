package org.tabata.timber.core.di

// TODO: Uncomment when Koin dependencies are available
// import org.koin.dsl.module
import org.tabata.timber.core.audio.AudioManager
import org.tabata.timber.core.subscription.SubscriptionManager
import org.tabata.timber.platform.PlatformAudioManager
import org.tabata.timber.platform.PlatformSubscriptionManager

/**
 * iOS-specific dependency injection module
 * TODO: Uncomment when Koin dependencies are available
 */
/*
actual val platformModule = module {
    single<AudioManager> { PlatformAudioManager() }
    single<SubscriptionManager> { PlatformSubscriptionManager() }
}
*/