# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 專案概述

TabataTimber 是一個 Kotlin Multiplatform 專案，使用 Compose Multiplatform 針對 Android 和 iOS 平台開發。採用清潔架構模式，將共享業務邏輯與平台特定實作分離。

## 架構結構

專案分為三個主要模組：

- **`/shared`** - 包含共享業務邏輯和領域模型
  - `commonMain/` - 所有目標平台共享的跨平台程式碼
  - `androidMain/` 和 `iosMain/` - 平台特定實作
  - 核心模組：`audio/`、`timer/`、`subscription/`（業務邏輯）
  - 資料層：`database/`、`repositories/`（資料管理）
  - 領域層：`models/`、`repositories/`、`usecases/`（業務規則）

- **`/composeApp`** - 使用 Compose Multiplatform 的 UI 應用層
  - 包含主要 App composable 和 UI 元件
  - 依賴 shared 模組取得業務邏輯
  - 需要時進行平台特定的 UI 適配

- **`/iosApp`** - iOS 特定應用程式進入點和原生整合
  - SwiftUI 程式碼和 iOS 特定配置
  - 橋接至共享 Kotlin 程式碼

## 開發指令

### 建構專案
```bash
# 建構所有目標
./gradlew build

# 專門建構 Android 應用程式
./gradlew :composeApp:assembleDebug

# 建構 iOS framework
./gradlew :shared:linkDebugFrameworkIosArm64
```

### 執行測試
```bash
# 執行所有測試
./gradlew test

# 僅執行通用測試
./gradlew :shared:commonTest

# 執行 Android 單元測試
./gradlew :composeApp:testDebugUnitTest
```

### 開發環境設定
- 使用 Gradle 8.12.1 和 Kotlin 2.2.0
- Compose Multiplatform 1.8.2
- 目標 SDK：Android 35，最低 SDK：Android 24
- iOS 目標：x64、arm64 和 simulator arm64

### 主要依賴項
- Compose Multiplatform 用於 UI
- AndroidX Lifecycle 用於狀態管理
- Kotlin Test 用於跨平台測試

## 程式碼庫使用方式

新增功能時：
1. 將共享業務邏輯放在 `/shared/src/commonMain/kotlin/org/tabata/timber/`
2. 在各自的 `androidMain/` 或 `iosMain/` 資料夾中新增平台特定實作
3. UI 元件放在 `/composeApp/src/commonMain/kotlin/`
4. 遵循現有套件結構：`org.tabata.timber.*`

專案使用分層架構模式，將領域邏輯與資料存取和 UI 呈現層分離。

# 所有交互回答都使用中文

#完成後使用git 推送到遠端