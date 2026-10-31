package com.ivy.data.di

import com.ivy.data.backup.BackupDataUseCase
import com.ivy.data.backup.auto.AndroidKeystoreBackupKeyWrapper
import com.ivy.data.backup.auto.BackupFolderAccess
import com.ivy.data.backup.auto.BackupKeyWrapper
import com.ivy.data.backup.auto.BackupWriter
import com.ivy.data.backup.auto.SafBackupFolderAccess
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface BackupModule {
    @Binds
    fun backupWriter(impl: BackupDataUseCase): BackupWriter

    @Binds
    fun backupFolderAccess(impl: SafBackupFolderAccess): BackupFolderAccess

    @Binds
    fun backupKeyWrapper(impl: AndroidKeystoreBackupKeyWrapper): BackupKeyWrapper
}
