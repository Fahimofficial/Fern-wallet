package com.ivy.data.db.entity

import androidx.annotation.Keep
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ivy.base.kotlinxserilzation.KSerializerUUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.*

@Suppress("DataClassDefaultValues")
@Keep
@Serializable
@Entity(tableName = "accounts")
data class AccountEntity(
    @SerialName("name")
    val name: String,
    @SerialName("currency")
    val currency: String? = null,
    @SerialName("color")
    val color: Int,
    @SerialName("icon")
    val icon: String? = null,
    @SerialName("orderNum")
    val orderNum: Double = 0.0,
    @SerialName("includeInBalance")
    val includeInBalance: Boolean = true,
    /**
     * "ASSET" or "CREDIT_CARD" (see AccountType). Defaults keep older databases and
     * backups (which don't have this field) as ordinary asset accounts.
     */
    @SerialName("type")
    @ColumnInfo(defaultValue = AccountTypeAsset)
    val type: String = AccountTypeAsset,

    // Appearance (logo, card background). All nullable: null means "as before", so existing
    // accounts and older backups (which lack these fields) look exactly as they did.
    @SerialName("logoKey")
    val logoKey: String? = null,
    @SerialName("customLogoPath")
    val customLogoPath: String? = null,
    @SerialName("backgroundStyle")
    val backgroundStyle: String? = null,
    /** Comma-separated ARGB hex, e.g. "FFE91E63,FF3F51B5". */
    @SerialName("backgroundColors")
    val backgroundColors: String? = null,
    @SerialName("backgroundAngle")
    val backgroundAngle: Float? = null,
    @SerialName("backgroundImagePath")
    val backgroundImagePath: String? = null,
    @SerialName("backgroundDim")
    val backgroundDim: Float? = null,

    @Deprecated("Obsolete field used for cloud sync. Can't be deleted because of backwards compatibility")
    @SerialName("isSynced")
    val isSynced: Boolean = false,
    @Deprecated("Obsolete field used for cloud sync. Can't be deleted because of backwards compatibility")
    @SerialName("isDeleted")
    val isDeleted: Boolean = false,

    @PrimaryKey
    @SerialName("id")
    @Serializable(with = KSerializerUUID::class)
    val id: UUID = UUID.randomUUID()
)

const val AccountTypeAsset = "ASSET"
const val AccountTypeCreditCard = "CREDIT_CARD"
