package org.tabata.timber.core.di

import org.koin.core.module.Module

/**
 * Platform-specific module containing dependencies that require platform-specific implementations
 * Each platform (Android/iOS) will provide their actual implementation
 */
expect val platformModule: Module