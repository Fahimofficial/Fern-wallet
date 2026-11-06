package com.ivy.data.media

/** Records deletions instead of touching files. */
class FakeAccountMediaFiles : AccountMediaFiles {
    val deleted = mutableListOf<String>()
    var deletedAll = false

    override suspend fun delete(paths: Collection<String>) {
        deleted += paths
    }

    override suspend fun deleteAll() {
        deletedAll = true
    }
}
