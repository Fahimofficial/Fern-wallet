package com.ivy.data.backup.auto

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.OutputStream
import javax.inject.Inject

/** A folder the user picked (Storage Access Framework tree), as the auto-backup sees it. */
interface BackupFolder {
    /** Creates a new file and returns a stream to write it, or null if that isn't possible. */
    fun create(name: String, mimeType: String): OutputStream?

    /** Names of the files directly in the folder, with a handle to delete each. */
    fun files(): List<BackupFolderFile>

    fun delete(file: BackupFolderFile): Boolean
}

class BackupFolderFile(val name: String, val id: String)

interface BackupFolderAccess {
    /** The folder for [treeUri], or null if the app has no (more) access to it. */
    fun open(treeUri: String): BackupFolder?

    /** A name to show for the folder, or null if it's unavailable. */
    fun displayName(treeUri: String): String?

    /** Keeps read/write access to a folder the user just picked, across restarts. */
    fun keepAccess(treeUri: String)

    fun releaseAccess(treeUri: String)
}

private const val ReadWrite =
    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION

class SafBackupFolderAccess @Inject constructor(
    @ApplicationContext private val context: Context,
) : BackupFolderAccess {
    override fun open(treeUri: String): BackupFolder? {
        val tree = Uri.parse(treeUri)
        val hasPermission = context.contentResolver.persistedUriPermissions.any {
            it.uri == tree && it.isReadPermission && it.isWritePermission
        }
        if (!hasPermission) return null
        return try {
            val folderDocument = DocumentsContract.buildDocumentUriUsingTree(
                tree,
                DocumentsContract.getTreeDocumentId(tree)
            )
            SafBackupFolder(context, tree, folderDocument).takeIf { displayName(treeUri) != null }
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    override fun keepAccess(treeUri: String) {
        context.contentResolver.takePersistableUriPermission(Uri.parse(treeUri), ReadWrite)
    }

    override fun releaseAccess(treeUri: String) {
        try {
            context.contentResolver.releasePersistableUriPermission(Uri.parse(treeUri), ReadWrite)
        } catch (e: SecurityException) {
            // Already gone.
        }
    }

    override fun displayName(treeUri: String): String? = try {
        val tree = Uri.parse(treeUri)
        val document = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        context.contentResolver.query(
            document,
            arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
    } catch (e: SecurityException) {
        null
    } catch (e: IllegalArgumentException) {
        null
    }
}

private class SafBackupFolder(
    private val context: Context,
    private val tree: Uri,
    private val folderDocument: Uri,
) : BackupFolder {
    override fun create(name: String, mimeType: String): OutputStream? {
        val file = DocumentsContract.createDocument(context.contentResolver, folderDocument, mimeType, name)
            ?: return null
        return context.contentResolver.openOutputStream(file, "w")
    }

    override fun files(): List<BackupFolderFile> {
        val children = DocumentsContract.buildChildDocumentsUriUsingTree(
            tree,
            DocumentsContract.getDocumentId(folderDocument)
        )
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
        return context.contentResolver.query(children, projection, null, null, null)?.use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(BackupFolderFile(name = cursor.getString(1) ?: "", id = cursor.getString(0)))
                }
            }
        }.orEmpty()
    }

    override fun delete(file: BackupFolderFile): Boolean = try {
        DocumentsContract.deleteDocument(
            context.contentResolver,
            DocumentsContract.buildDocumentUriUsingTree(tree, file.id)
        )
    } catch (e: SecurityException) {
        false
    } catch (e: java.io.FileNotFoundException) {
        false
    }
}
