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
            customLogoPath = "/media/logo1.png",
            backgroundStyle = AccountBackgroundStyle.Image,
            backgroundImagePath = "/media/bg1.jpg",
        ),
    )

    @Test
    fun `replacing the logo deletes the old file only`() = runTest {
        repository.save(account)
        files.deleted.shouldBeEmpty()

        repository.save(account.copy(appearance = account.appearance.copy(customLogoPath = "/media/logo2.png")))

        files.deleted shouldContainExactly listOf("/media/logo1.png")
    }

    @Test
    fun `deleting the account deletes its files`() = runTest {
        repository.save(account)
        repository.deleteById(account.id)
        files.deleted shouldContainExactly listOf("/media/logo1.png", "/media/bg1.jpg")
    }

    @Test
    fun `wiping all accounts clears the media folder`() = runTest {
        repository.deleteAll()
        files.deletedAll shouldBe true
    }
}
