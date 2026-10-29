@file:Suppress("InvalidPackageDeclaration") // same package as AccountsAct

package com.ivy.wallet.domain.action.account

import com.ivy.legacy.datamodel.Account
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import java.util.UUID

/**
 * Accounts that can be picked for a new money movement: everything except closed credit cards
 * (`CreditCardRepository.findClosedCardIds`), apart from [alreadyUsed] — accounts the edited
 * item already references, so editing it never drops its account.
 */
fun List<Account>.selectable(closedCardIds: Set<UUID>, alreadyUsed: Set<UUID> = emptySet()): ImmutableList<Account> =
    filter { it.id !in closedCardIds || it.id in alreadyUsed }.toImmutableList()
