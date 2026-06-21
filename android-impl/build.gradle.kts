// 昆山佰泰胜专属 ERP V1.3.9 - Android Root Build
// 客户合同 XP-ZPF202606082405 · 厂商 河南晓评信息科技
//
// 标准 Android Studio 多模块项目：仅声明插件版本（不应用）
// 各 subproject（app/）通过 plugins { id("...") } 自动继承
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.google.devtools.ksp") version "1.9.22-1.0.17" apply false
    id("com.google.dagger.hilt.android") version "2.51" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22" apply false
}

// V1.3.9 标准 Android Studio 项目结构（app/ 子模块）
// 历史：V1.3.7 顶层 build.gradle.kts 即 app 项目 · 已重构为标准结构
// 所有业务代码与 module 配置在 app/build.gradle.kts
