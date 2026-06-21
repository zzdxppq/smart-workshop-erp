package com.btsheng.erp.di

import android.content.Context
import com.btsheng.erp.BuildConfig
import com.btsheng.erp.core.data.local.ErpDatabase
import com.btsheng.erp.core.data.local.PendingScanDao
import com.btsheng.erp.core.network.AppMessageApi
import com.btsheng.erp.core.network.AuthApi
import com.btsheng.erp.core.network.E5ScanApi
import com.btsheng.erp.core.network.AuthHeaderInterceptor
import com.btsheng.erp.core.network.GatewayServiceRouteInterceptor
import com.btsheng.erp.core.network.OutsourceReceiveApi
import com.btsheng.erp.core.network.SysParamApi
import com.btsheng.erp.core.network.WarehouseScanApi
import com.btsheng.erp.feature.v138.ApiClient
import com.btsheng.erp.core.network.LocalDateTimeJsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun moshi(): Moshi = Moshi.Builder()
        .add(LocalDateTimeJsonAdapter)
        .add(KotlinJsonAdapterFactory())
        .build()

    @Provides
    @Singleton
    fun okHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .addInterceptor(GatewayServiceRouteInterceptor())
        .addInterceptor(AuthHeaderInterceptor())
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
            }
        }
        .build()

    @Provides
    @Singleton
    fun retrofit(moshi: Moshi, client: OkHttpClient): Retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    @Provides
    @Singleton
    fun e5ScanApi(retrofit: Retrofit): E5ScanApi = retrofit.create(E5ScanApi::class.java)

    @Provides
    @Singleton
    fun appMessageApi(retrofit: Retrofit): AppMessageApi = retrofit.create(AppMessageApi::class.java)

    @Provides
    @Singleton
    fun authApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun sysParamApi(retrofit: Retrofit): SysParamApi = retrofit.create(SysParamApi::class.java)

    @Provides
    @Singleton
    fun v138ApiClient(retrofit: Retrofit): ApiClient = retrofit.create(ApiClient::class.java)

    @Provides
    @Singleton
    fun warehouseScanApi(retrofit: Retrofit): WarehouseScanApi = retrofit.create(WarehouseScanApi::class.java)

    @Provides
    @Singleton
    fun outsourceReceiveApi(retrofit: Retrofit): OutsourceReceiveApi = retrofit.create(OutsourceReceiveApi::class.java)

    @Provides
    @Singleton
    fun productionDashboardApi(retrofit: Retrofit): com.btsheng.erp.core.network.ProductionDashboardApi =
        retrofit.create(com.btsheng.erp.core.network.ProductionDashboardApi::class.java)

    @Provides
    @Singleton
    fun qualityInspectionApi(retrofit: Retrofit): com.btsheng.erp.core.network.QualityInspectionApi =
        retrofit.create(com.btsheng.erp.core.network.QualityInspectionApi::class.java)

    @Provides
    @Singleton
    fun approvalApi(retrofit: Retrofit): com.btsheng.erp.core.network.ApprovalApi =
        retrofit.create(com.btsheng.erp.core.network.ApprovalApi::class.java)

    @Provides
    @Singleton
    fun hrApi(retrofit: Retrofit): com.btsheng.erp.core.network.HrApi = retrofit.create(com.btsheng.erp.core.network.HrApi::class.java)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): ErpDatabase =
        androidx.room.Room.databaseBuilder(context, ErpDatabase::class.java, "erp.db").build()

    @Provides
    fun pendingScanDao(db: ErpDatabase): PendingScanDao = db.pendingScanDao()
}
