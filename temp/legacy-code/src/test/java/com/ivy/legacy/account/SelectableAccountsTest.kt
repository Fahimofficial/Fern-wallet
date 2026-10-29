package com.ivy.legacy.account

import com.ivy.legacy.datamodel.Account
import com.ivy.wallet.domain.action.account.selectable
import io.kotest.matchers.shouldBe
import org.junit.Test
import java.util.UUID

class SelectableAccountsTest {
    private val bank = Account(name = "Bank", color = 0, id = UUID(0, 1))
    private val openCard = Account(name = "Open card", color = 0, id = UUID(0, 2))
    private val closedCard = Account(name = "Closed card", color = 0, id = UUID(0, 3))
    private val all = listOf(bank, openCard, closedCard)

    @Test
    fun `closed cards are not offered`() {
        all.selectable(setOf(closedCard.id)) shouldBe listOf(bank, openCard)
    }

    @Test
    fun `a closed card the edited item already uses stays selectable`() {
        all.selectable(setOf(closedCard.id), alreadyUsed = setOf(closedCard.id)) shouldBe all
    }

    @Test
    fun `only closed cards means nothing is selectable`() {
        listOf(closedCard).selectable(setOf(closedCard.id)) shouldBe emptyList()
    }
}
