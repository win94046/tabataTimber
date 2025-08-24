package org.tabata.timber.platform

import org.tabata.timber.core.subscription.SubscriptionManager

/**
 * Platform-specific subscription manager implementation
 * Actual implementations will handle StoreKit (iOS) and Google Play Billing (Android)
 */
expect class PlatformSubscriptionManager() : SubscriptionManager