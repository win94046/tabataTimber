package org.tabata.timber.platform

import org.tabata.timber.core.audio.AudioManager

/**
 * Platform-specific audio manager implementation
 * Actual implementations will be provided for Android and iOS
 */
expect class PlatformAudioManager() : AudioManager