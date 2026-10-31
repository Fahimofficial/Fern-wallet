package com.ivy.data.backup

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal const val BackupTestJson = """{"accounts":[],"note":"ünïcødé ₹"}"""

internal fun zipOf(vararg entries: Pair<String, ByteArray>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        for ((name, bytes) in entries) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
    return out.toByteArray()
}

internal fun readBackup(bytes: ByteArray, max: Long = BackupArchive.MAX_JSON_BYTES): String =
    BackupArchive.readJson(ByteArrayInputStream(bytes), max)
