package com.rimaro.musify.di

import com.rimaro.musify.data.extractor.Extractor
import com.rimaro.musify.data.extractor.TrackExtractor
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ExtractorModule {
    @Binds
    @Singleton
    abstract fun bindTrackExtractor(impl: Extractor): TrackExtractor

    companion object {

        @Provides
        @Singleton
        fun provideNewPipeInit(downloader: Downloader): Boolean {
            NewPipe.init(downloader)
            return true
        }
    }
}