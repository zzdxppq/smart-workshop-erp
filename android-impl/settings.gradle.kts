// 昆山佰泰胜专属 ERP V1.3.9 - Android Settings
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "erp-android"
include(":app")
// V1.3.9 Sprint 12-14 整合 · 标准 Android Studio 项目结构（app/ 子模块）
// 历史：V1.3.7 顶层 build.gradle.kts 即 app 项目 · 已重构为标准结构

