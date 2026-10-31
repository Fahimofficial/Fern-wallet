package com.ivy.data.backup

import java.io.BufferedInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Reads and writes the backup file format: a ZIP with exactly one `.json` entry (UTF-16), or,
 * for very old backups, the bare JSON file.
 *
 * Reading happens fully in memory: entries are never extracted to disk, so entry names (e.g.
 * `../../shared_prefs/x.xml`) can't write anywhere. Sizes are capped so a crafted or corrupted
 * file can't exhaust memory (zip bomb).
 */
object BackupArchive {
    /** Name of the JSON entry in backups written by this version. */
    const val JSON_ENTRY_NAME = "data.json"

    /**
     * Upper bound for the uncompressed JSON. UTF-16 uses ~2 bytes per character; a wallet with
     * 100k transactions is roughly 120 MB, so this leaves headroom while still bounding memory.
     */
    const val MAX_JSON_BYTES: Long = 256L * 1024 * 1024
    private const val MAX_ENTRIES = 32

    private val ZIP_MAGIC = byteArrayOf(0x50, 0x4B, 0x03, 0x04)

    /**
     * Returns the backup JSON contained in [input], or throws [BackupImportException].
     * The stream is not closed.
     */
    fun readJson(input: InputStream, maxJsonBytes: Long = MAX_JSON_BYTES): String {
        val buffered = BufferedInputStream(input)
        buffered.mark(ZIP_MAGIC.size)
        val header = ByteArray(ZIP_MAGIC.size)
        val headerSize = buffered.readNBytesCompat(header)
        buffered.reset()

        if (headerSize == 0) throw BackupImportException(ImportFailure.NOT_A_BACKUP)

        val bytes = if (headerSize == ZIP_MAGIC.size && header.contentEquals(ZIP_MAGIC)) {
            readSingleJsonEntry(buffered, maxJsonBytes)
        } else {
            // Legacy: a bare JSON file (no ZIP).
            buffered.readLimited(maxJsonBytes)
        }
        val json = decode(bytes)
        if (!json.trimStart().startsWith("{")) throw BackupImportException(ImportFailure.NOT_A_BACKUP)
        return json
    }

    /** Writes [json] as a backup ZIP (one UTF-16 `data.json` entry) to [output]. */
    fun writeZip(output: OutputStream, json: String) {
        ZipOutputStream(output).use { zip ->
            zip.putNextEntry(ZipEntry(JSON_ENTRY_NAME))
            zip.write(json.toByteArray(Charsets.UTF_16))
            zip.closeEntry()
        }
    }

    private fun readSingleJsonEntry(input: InputStream, maxJsonBytes: Long): ByteArray = try {
        val zip = ZipInputStream(input)
        val jsonEntries = mutableListOf<ByteArray>()
        var entryCount = 0
        var entry = zip.nextEntry
        while (entry != null) {
            entryCount++
            if (!entry.isDirectory && entry.name.endsWith(".json", ignoreCase = true)) {
                jsonEntries += zip.readLimited(maxJsonBytes)
            }
            if (entryCount > MAX_ENTRIES || jsonEntries.size > 1) notABackup()
            entry = zip.nextEntry
        }
        jsonEntries.singleOrNull() ?: notABackup()
    } catch (e: IOException) {
        // Includes ZipException: a damaged or truncated archive.
        throw BackupImportException(ImportFailure.CORRUPTED, e)
    }

    private fun notABackup(): Nothing = throw BackupImportException(ImportFailure.NOT_A_BACKUP)

    /**
     * Backups are written as UTF-16 with a byte-order mark. Files without a BOM that look like
     * plain JSON are read as UTF-8 (e.g. a hand-edited file).
     */
    internal fun decode(bytes: ByteArray): String {
        if (hasUtf16Bom(bytes)) return String(bytes, Charsets.UTF_16)
        val firstChar = bytes.firstOrNull()?.toInt()?.toChar()
        return if (firstChar == '{' || firstChar?.isWhitespace() == true) {
            String(bytes, Charsets.UTF_8)
        } else {
            String(bytes, Charsets.UTF_16)
        }
    }

    private fun hasUtf16Bom(bytes: ByteArray): Boolean {
        if (bytes.size < 2) return false
        val first = bytes[0]
        val second = bytes[1]
        val bigEndian = first == BOM_HIGH && second == BOM_LOW
        val littleEndian = first == BOM_LOW && second == BOM_HIGH
        return bigEndian || littleEndian
    }

    private const val BOM_HIGH: Byte = 0xFE.toByte()
    private const val BOM_LOW: Byte = 0xFF.toByte()

    private fun InputStream.readLimited(maxBytes: Long): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0L
        while (true) {
            val read = read(buffer)
            if (read < 0) break
            total += read
            if (total > maxBytes) throw BackupImportException(ImportFailure.TOO_LARGE)
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun InputStream.readNBytesCompat(target: ByteArray): Int {
        var offset = 0
        while (offset < target.size) {
            val read = read(target, offset, target.size - offset)
            if (read < 0) break
            offset += read
        }
        return offset
    }
}

/** Why a backup couldn't be restored. Shown to the user; nothing is written in any of these cases. */
enum class ImportFailure {
    /** Not a Fern/Ivy Wallet backup (no JSON inside, several JSON files, or not JSON at all). */
    NOT_A_BACKUP,

    /** The ZIP or the JSON inside is damaged or can't be read. */
    CORRUPTED,

    /** The backup is larger than the app can safely load. */
    TOO_LARGE,

    /** Made by a newer app version, using a format this version doesn't understand. */
    NEWER_VERSION,

    /** Reading was fine but saving failed; the database was left unchanged. */
    SAVE_FAILED,
}

class BackupImportException(
    val failure: ImportFailure,
    cause: Throwable? = null,
) : Exception(failure.name, cause)
