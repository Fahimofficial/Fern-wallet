package com.ivy.data.di

import com.ivy.data.media.AccountMediaFiles
import com.ivy.data.media.AccountMediaStore
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class AccountMediaModule {
    @Binds
    abstract fun bindAccountMediaFiles(store: AccountMediaStore): AccountMediaFiles
}
