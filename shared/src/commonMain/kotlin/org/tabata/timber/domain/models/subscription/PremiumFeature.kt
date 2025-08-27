package org.tabata.timber.domain.models.subscription

/**
 * Enum representing all premium features available in the app
 */
enum class PremiumFeature(
    val displayName: String,
    val description: String,
    val category: FeatureCategory
) {
    // Timer Features
    CUSTOM_TIMERS(
        displayName = "自訂計時器",
        description = "建立無限數量的自訂Tabata計時器配置",
        category = FeatureCategory.TIMER
    ),
    
    ADVANCED_TIMER_SETTINGS(
        displayName = "高級計時器設定",
        description = "存取進階計時器選項，包括自訂音效和振動模式",
        category = FeatureCategory.TIMER
    ),
    
    UNLIMITED_SETS(
        displayName = "無限組數",
        description = "在單次訓練中建立無限數量的組數",
        category = FeatureCategory.TIMER
    ),
    
    EXTENDED_WARMUP_COOLDOWN(
        displayName = "延長熱身/緩和",
        description = "設定更長的熱身和緩和時間（最多30分鐘）",
        category = FeatureCategory.TIMER
    ),
    
    // Audio Features
    PREMIUM_SOUNDS(
        displayName = "高級音效",
        description = "存取更多高品質的計時器音效和語音提示",
        category = FeatureCategory.AUDIO
    ),
    
    CUSTOM_MUSIC_INTEGRATION(
        displayName = "音樂整合",
        description = "與您的音樂應用程式整合，自動調節播放",
        category = FeatureCategory.AUDIO
    ),
    
    VOICE_COACHING(
        displayName = "語音指導",
        description = "專業的語音指導和動機提醒",
        category = FeatureCategory.AUDIO
    ),
    
    // Tracking Features
    UNLIMITED_HISTORY(
        displayName = "無限歷史記錄",
        description = "儲存無限數量的訓練記錄和進度資料",
        category = FeatureCategory.TRACKING
    ),
    
    DETAILED_ANALYTICS(
        displayName = "詳細分析",
        description = "深入的訓練分析和進度圖表",
        category = FeatureCategory.TRACKING
    ),
    
    WEIGHT_TRACKING(
        displayName = "體重追蹤",
        description = "追蹤體重變化和BMI計算",
        category = FeatureCategory.TRACKING
    ),
    
    BODY_MEASUREMENTS(
        displayName = "身體測量",
        description = "記錄和追蹤各種身體測量數據",
        category = FeatureCategory.TRACKING
    ),
    
    PROGRESS_PHOTOS(
        displayName = "進度照片",
        description = "儲存和比較進度照片",
        category = FeatureCategory.TRACKING
    ),
    
    // Export Features
    DATA_EXPORT(
        displayName = "資料匯出",
        description = "匯出訓練資料為CSV或PDF格式",
        category = FeatureCategory.EXPORT
    ),
    
    CLOUD_BACKUP(
        displayName = "雲端備份",
        description = "自動雲端備份所有資料和設定",
        category = FeatureCategory.EXPORT
    ),
    
    // Customization Features
    THEMES_AND_COLORS(
        displayName = "主題和顏色",
        description = "自訂應用程式的外觀主題和色彩配置",
        category = FeatureCategory.CUSTOMIZATION
    ),
    
    CUSTOM_LAYOUTS(
        displayName = "自訂佈局",
        description = "自訂計時器和儀表板的佈局",
        category = FeatureCategory.CUSTOMIZATION
    ),
    
    WIDGETS(
        displayName = "小工具",
        description = "在主畫面上使用Tabata計時器小工具",
        category = FeatureCategory.CUSTOMIZATION
    ),
    
    // Social Features
    SHARING_AND_SOCIAL(
        displayName = "分享和社交",
        description = "分享訓練成果到社交媒體平台",
        category = FeatureCategory.SOCIAL
    ),
    
    WORKOUT_CHALLENGES(
        displayName = "訓練挑戰",
        description = "參與社群挑戰和競賽",
        category = FeatureCategory.SOCIAL
    ),
    
    // Support Features
    PRIORITY_SUPPORT(
        displayName = "優先客服支援",
        description = "享受優先的客戶服務和技術支援",
        category = FeatureCategory.SUPPORT
    ),
    
    NO_ADS(
        displayName = "無廣告",
        description = "移除所有廣告，享受無干擾的訓練體驗",
        category = FeatureCategory.SUPPORT
    );
    
    /**
     * Returns the feature key for internal usage
     */
    fun getFeatureKey(): String = name.lowercase()
    
    /**
     * Returns whether this feature is core functionality
     */
    fun isCoreFunctionality(): Boolean {
        return when (this) {
            CUSTOM_TIMERS, 
            UNLIMITED_SETS, 
            UNLIMITED_HISTORY, 
            NO_ADS -> true
            else -> false
        }
    }
    
    /**
     * Returns the recommended upgrade priority (1 = highest)
     */
    fun getUpgradePriority(): Int {
        return when (this) {
            NO_ADS -> 1
            CUSTOM_TIMERS -> 2
            UNLIMITED_HISTORY -> 3
            DETAILED_ANALYTICS -> 4
            PREMIUM_SOUNDS -> 5
            WEIGHT_TRACKING -> 6
            else -> 7
        }
    }
    
    companion object {
        /**
         * Returns all features in a specific category
         */
        fun getFeaturesByCategory(category: FeatureCategory): List<PremiumFeature> {
            return values().filter { it.category == category }
        }
        
        /**
         * Returns core features that are most commonly used
         */
        fun getCoreFeatures(): List<PremiumFeature> {
            return values().filter { it.isCoreFunctionality() }
        }
        
        /**
         * Returns features sorted by upgrade priority
         */
        fun getFeaturesByPriority(): List<PremiumFeature> {
            return values().sortedBy { it.getUpgradePriority() }
        }
    }
}

/**
 * Categories for organizing premium features
 */
enum class FeatureCategory(
    val displayName: String,
    val description: String
) {
    TIMER(
        displayName = "計時器功能",
        description = "計時器相關的高級功能"
    ),
    
    AUDIO(
        displayName = "音效功能", 
        description = "音效和語音相關功能"
    ),
    
    TRACKING(
        displayName = "追蹤功能",
        description = "資料追蹤和分析功能"
    ),
    
    EXPORT(
        displayName = "匯出功能",
        description = "資料匯出和備份功能"
    ),
    
    CUSTOMIZATION(
        displayName = "自訂功能",
        description = "外觀和介面自訂功能"
    ),
    
    SOCIAL(
        displayName = "社交功能",
        description = "分享和社群互動功能"
    ),
    
    SUPPORT(
        displayName = "支援功能",
        description = "客服和使用體驗優化"
    )
}