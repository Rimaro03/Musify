package com.rimaro.musify.di

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.rimaro.musify.data.extractor.DownloaderImpl
import com.rimaro.musify.data.remote.DeezerApi
import com.rimaro.musify.data.remote.MusicBrainzApi
import com.rimaro.musify.data.remote.interceptors.UserAgentInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import org.schabi.newpipe.extractor.downloader.Downloader
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        isLenient = true
    }

//    @Provides
//    @Singleton
//    fun provideOkHttpClient(): OkHttpClient {
//        return OkHttpClient.Builder()
//            .addInterceptor { chain ->
//                val request = chain.request().newBuilder()
//                    .header("User-Agent", "MyMusicApp/1.0.0 (contact@example.com)")
//                    .build()
//                chain.proceed(request)
//            }
//            .addInterceptor(UserAgentInterceptor())
//            .connectTimeout(15, TimeUnit.SECONDS)
//            .build()
//    }

    @Provides
    @Singleton
    fun provideMusicBrainzApi(
        okHttpClient: OkHttpClient,
        json: Json
    ): MusicBrainzApi {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl("https://musicbrainz.org/ws/2/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(MusicBrainzApi::class.java)
    }

    @Provides
    @Singleton
    fun provideDeezerApi(
        okHttpClient: OkHttpClient,
        json: Json
    ): DeezerApi {
        val contentType = "application/json".toMediaType()

        return Retrofit.Builder()
            .baseUrl(" https://api.deezer.com/")
            .client(okHttpClient)
            .addConverterFactory(json.asConverterFactory(contentType))
            .build()
            .create(DeezerApi::class.java)
    }

    @Module
    @InstallIn(SingletonComponent::class)
    object NetworkModule {

        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        @Provides
        @Singleton
        fun provideDownloader(impl: DownloaderImpl): Downloader {
            return impl
        }
    }
}