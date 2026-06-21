# 昆山佰泰胜 ERP V1.3.9 · Android ProGuard / R8 规则
#
# release 启用 isMinifyEnabled = true（见 app/build.gradle.kts L62）
# R8 默认会执行 shrink + optimize + obfuscate

# ============ V1.3.9 标准 Android Studio 项目结构 ============
# 项目级别的 proguard 规则在 app/proguard-rules.pro
# 各 module 自行维护 buildTypes { release { proguardFiles(...) } }

# ============ Kotlin 反射（KSP / Hilt 生成代码依赖） ============
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature, Exceptions
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# ============ Hilt / Dagger（生成代码） ============
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.* { *; }
-keepclasseswithmembers class * {
    @dagger.hilt.android.* <methods>;
}
-keepclasseswithmembers class * {
    @dagger.* <methods>;
}

# ============ Retrofit / OkHttp / Moshi ============
-dontwarn retrofit2.**
-dontwarn okhttp3.**
-dontwarn okio.**
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response

# Moshi（反射 / 协程）
-keep,allowobfuscation,allowshrinking @com.squareup.moshi.JsonClass class *
-keep class **JsonAdapter { *; }
-keepclassmembers @com.squareup.moshi.JsonClass class * {
    <fields>;
    <init>(...);
}

# ============ Room（生成代码） ============
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# ============ Compose / ViewBinding ============
-keep class androidx.compose.** { *; }
-keep class com.btsheng.erp.databinding.** { *; }

# ============ WorkManager / Hilt-Work ============
-keep class * extends androidx.work.ListenableWorker
-keep class * extends androidx.work.CoroutineWorker
-keep class androidx.hilt.work.** { *; }

# ============ 业务保留 ============
# 5 类码路由 + 7 状态机（QrCodeParser / TokenStore / ConflictResolver）
-keep class com.btsheng.erp.core.scan.** { *; }
-keep class com.btsheng.erp.core.security.** { *; }
-keep class com.btsheng.erp.core.sync.** { *; }
-keep class com.btsheng.erp.core.data.local.** { *; }

# V1.3.8 / V1.3.9 Feature（按版本分包）
-keep class com.btsheng.erp.feature.** { *; }

# ============ 资源压缩 ============
# 启用 res shrink（需 build.gradle.kts 配合 isShrinkResources = true，本期关闭）
# isShrinkResources = true
