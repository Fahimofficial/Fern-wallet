package com.ivy.data.repository

import com.ivy.base.TestDispatchersProvider
import com.ivy.data.db.dao.fake.FakeAccountDao
import com.ivy.data.db.dao.fake.FakeSettingsDao
import com.ivy.data.media.FakeAccountMediaFiles
import com.ivy.data.model.Account
import com.ivy.data.model.AccountAppearance
import com.ivy.data.model.AccountBackgroundStyle
import com.ivy.data.model.AccountId
import com.ivy.data.model.primitive.AssetCode
import com.ivy.data.model.primitive.ColorInt
import com.ivy.data.model.primitive.NotBlankTrimmedString
import com.ivy.data.repository.fake.fakeRepositoryMemoFactory
import com.ivy.data.repository.mapper.AccountMapper
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.Test
import java.util.UUID

private const val OriginalLogoPath = "/media/logo1.png"
private const val ReplacementLogoPath = "/media/logo2.png"
private const val BackgroundImagePath = "/media/bg1.jpg"

/** An account's logo and background files go when they're replaced or the account is deleted. */
class AccountMediaCleanupTest {
    private val dao = FakeAccountDao()
    private val files = FakeAccountMediaFiles()
    private val settingsDao = FakeSettingsDao()
    private val repository = AccountRepository(
        mapper = AccountMapper(CurrencyRepository(settingsDao, settingsDao, TestDispatchersProvider)),
        accountDao = dao,
        writeAccountDao = dao,
        dispatchersProvider = TestDispatchersProvider,
        memoFactory = fakeRepositoryMemoFactory(),
        mediaFiles = files,
    )
    private val account = Account(
        id = AccountId(UUID.randomUUID()),
        name = NotBlankTrimmedString.unsafe("Indie"),
        asset = AssetCode.unsafe("INR"),
        color = ColorInt(1),
        icon = null,
        includeInBalance = true,
        orderNum = 0.0,
        appearance = AccountAppearance(
            customLogoPath = OriginalLogoPath,
            backgroundStyle = AccountBackgroundStyle.Image,
            backgroundImagePath = BackgroundImagePath,
        ),
    )

    @Test
    fun `replacing the logo deletes the old file only`() = runTest {
        repository.save(account)
        files.deleted.shouldBeEmpty()

        repository.save(account.copy(appearance = account.appearance.copy(customLogoPath = ReplacementLogoPath)))

        files.deleted shouldContainExactly listOf(OriginalLogoPath)
    }

    @Test
    fun `deleting the account deletes its files`() = runTest {
        repository.save(account)
        repository.deleteById(account.id)
        files.deleted shouldContainExactly listOf(OriginalLogoPath, BackgroundImagePath)
    }

    @Test
    fun `wiping all accounts clears the media folder`() = runTest {
        repository.deleteAll()
        files.deletedAll shouldBe true
    }
}
