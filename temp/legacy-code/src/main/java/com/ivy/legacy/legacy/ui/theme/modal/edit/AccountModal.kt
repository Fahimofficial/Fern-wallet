package com.ivy.wallet.ui.theme.modal.edit

import androidx.compose.foundation.layout.size
import com.ivy.data.model.AccountBackgroundStyle
import com.ivy.legacy.ui.component.account.AccountBackgroundSheet
import com.ivy.legacy.ui.component.account.accountBackground
import com.ivy.legacy.ui.component.account.accountPaint
import android.widget.Toast
import com.ivy.legacy.ui.component.account.rememberImagePicker
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.semantics.Role
import com.ivy.data.model.AccountAppearance
import com.ivy.legacy.ui.component.account.LocalAccountMediaStore
import com.ivy.ui.logo.AccountLogo
import com.ivy.ui.logo.LogoChoice
import com.ivy.ui.logo.LogoPickerSheet
import com.ivy.ui.logo.Logos
import com.ivy.ui.logo.hasAccountLogo
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ivy.design.l0_system.UI
import com.ivy.design.l0_system.style
import com.ivy.domain.legacy.ui.IvyColorPicker
import com.ivy.legacy.IvyWalletPreview
import com.ivy.legacy.datamodel.Account
import com.ivy.legacy.utils.isNotNullOrBlank
import com.ivy.legacy.utils.onScreenStart
import com.ivy.legacy.utils.selectEndTextFieldValue
import com.ivy.legacy.utils.toLowerCaseLocal
import com.ivy.legacy.utils.toUpperCaseLocal
import com.ivy.ui.R
import com.ivy.wallet.domain.data.IvyCurrency
import com.ivy.wallet.domain.deprecated.logic.model.CreateAccountData
import com.ivy.wallet.ui.theme.Gray
import com.ivy.wallet.ui.theme.Ivy
import com.ivy.wallet.ui.theme.components.IvyCheckboxWithText
import com.ivy.wallet.ui.theme.modal.ChooseIconModal
import com.ivy.wallet.ui.theme.modal.CurrencyModal
import com.ivy.wallet.ui.theme.modal.IvyModal
import com.ivy.wallet.ui.theme.modal.ModalAddSave
import com.ivy.wallet.ui.theme.modal.ModalAmountSection
import com.ivy.wallet.ui.theme.modal.ModalTitle
import java.util.UUID

@Deprecated("Old design system. Use `:ivy-design` and Material3")
data class AccountModalData(
    val account: Account?,
    val baseCurrency: String,
    val balance: Double,
    val adjustBalanceMode: Boolean = false,
    val forceNonZeroBalance: Boolean = false,
    val autoFocusKeyboard: Boolean = true,
    val id: UUID = UUID.randomUUID()
)

@Deprecated("Old design system. Use `:ivy-design` and Material3")
@Suppress("CyclomaticComplexMethod", "LongMethod") // one form; the branches are its fields
@Composable
fun BoxWithConstraintsScope.AccountModal(
    modal: AccountModalData?,
    onCreateAccount: (CreateAccountData) -> Unit,
    onEditAccount: (Account, balance: Double) -> Unit,
    dismiss: () -> Unit,
) {
    val account = modal?.account
    var nameTextFieldValue by remember(modal) {
        mutableStateOf(selectEndTextFieldValue(account?.name))
    }
    var color by remember(modal) {
        mutableStateOf(account?.color?.let { Color(it) } ?: Ivy)
    }
    var amount by remember(modal) {
        mutableStateOf(modal?.balance ?: 0.0)
    }
    var currencyCode by remember(modal) {
        mutableStateOf(account?.currency ?: modal?.baseCurrency ?: "")
    }
    var icon by remember(modal) {
        mutableStateOf(account?.icon)
    }
    var includeInBalance by remember(modal) {
        mutableStateOf(account?.includeInBalance ?: true)
    }
    var appearance by remember(modal) {
        mutableStateOf(account?.appearance ?: AccountAppearance.Default)
    }
    // Images imported while this modal is open; the ones not kept are deleted when it closes.
    val importedFiles = remember(modal) { mutableListOf<String>() }
    var logoPickerVisible by remember(modal) { mutableStateOf(false) }
    var backgroundSheetVisible by remember(modal) { mutableStateOf(false) }

    var amountModalVisible by remember { mutableStateOf(false) }
    var currencyModalVisible by remember { mutableStateOf(false) }
    var chooseIconModalVisible by remember(modal) {
        mutableStateOf(false)
    }

    val forceNonZeroBalance = modal?.forceNonZeroBalance ?: false

    val mediaStore = LocalAccountMediaStore.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    // Unsaved imports are deleted, saved ones replaced later are deleted by the repository.
    val close = { saved: AccountAppearance? ->
        val unused = importedFiles.filterNot { saved?.files?.contains(it) == true }
        importedFiles.clear()
        if (unused.isNotEmpty() && mediaStore != null) scope.launch { mediaStore.delete(unused) }
        dismiss()
    }
    val pickLogo = rememberImagePicker { uri ->
        if (mediaStore != null) {
            scope.launch {
                val path = mediaStore.importLogo(uri)
                if (path == null) {
                    Toast.makeText(context, R.string.logo_upload_failed, Toast.LENGTH_SHORT).show()
                } else {
                    importedFiles += path
                    appearance = appearance.copy(customLogoPath = path)
                }
            }
        }
    }

    val pickBackground = rememberImagePicker { uri ->
        if (mediaStore != null) {
            scope.launch {
                val path = mediaStore.importBackground(uri)
                if (path == null) {
                    Toast.makeText(context, R.string.logo_upload_failed, Toast.LENGTH_SHORT).show()
                } else {
                    importedFiles += path
                    appearance = appearance.copy(
                        backgroundStyle = AccountBackgroundStyle.Image,
                        backgroundImagePath = path,
                    )
                }
            }
        }
    }

    IvyModal(
        id = modal?.id,
        visible = modal != null,
        dismiss = { close(null) },
        shiftIfKeyboardShown = false,
        PrimaryAction = {
            ModalAddSave(
                item = modal?.account,
                enabled = nameTextFieldValue.text.isNotNullOrBlank() && (!forceNonZeroBalance || amount > 0)
            ) {
                save(
                    account = account,
                    nameTextFieldValue = nameTextFieldValue,
                    currency = currencyCode,
                    color = color,
                    icon = icon,
                    amount = amount,
                    includeInBalance = includeInBalance,
                    appearance = appearance,

                    onCreateAccount = onCreateAccount,
                    onEditAccount = onEditAccount,
                    dismiss = { close(appearance) }
                )
            }
        }
    ) {
        onScreenStart {
            if (modal?.adjustBalanceMode == true) {
                amountModalVisible = true
            }
        }

        Spacer(Modifier.height(32.dp))

        ModalTitle(
            text = if (modal?.account != null) {
                stringResource(
                    R.string.edit_account
                )
            } else {
                stringResource(R.string.new_account)
            },
        )

        Spacer(Modifier.height(24.dp))

        IconNameRow(
            hint = stringResource(R.string.account_name),
            defaultIcon = R.drawable.ic_custom_account_m,
            color = color,
            icon = icon,

            autoFocusKeyboard = modal?.autoFocusKeyboard ?: true,

            nameTextFieldValue = nameTextFieldValue,
            setNameTextFieldValue = { nameTextFieldValue = it },
            showChooseIconModal = {
                chooseIconModalVisible = true
            }
        )

        Spacer(Modifier.height(16.dp))

        AccountLogoSelectorRow(
            appearance = appearance,
            name = nameTextFieldValue.text,
            color = color,
        ) { logoPickerVisible = true }

        Spacer(Modifier.height(8.dp))

        AccountBackgroundSelectorRow(appearance = appearance, color = color) { backgroundSheetVisible = true }

        Spacer(Modifier.height(24.dp))

        IvyColorPicker(
            selectedColor = color,
            onColorSelected = { color = it }
        )

        Spacer(modifier = Modifier.height(40.dp))

        ModalAmountSection(
            Header = {
                Spacer(Modifier.height(16.dp))

                AccountCurrency(
                    currencyCode = currencyCode
                ) {
                    currencyModalVisible = true
                }

                Spacer(modifier = Modifier.height(16.dp))

                IvyCheckboxWithText(
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .align(Alignment.Start),
                    text = stringResource(R.string.include_account),
                    checked = includeInBalance
                ) {
                    includeInBalance = it
                }
            },
            label = stringResource(R.string.enter_account_balance).uppercase(),
            currency = currencyCode,
            amount = amount,
            amountPaddingTop = 40.dp,
            amountPaddingBottom = 40.dp,
        ) {
            amountModalVisible = true
        }
    }

    val amountModalId = remember(modal, amount) {
        UUID.randomUUID()
    }
    AmountModal(
        id = amountModalId,
        visible = amountModalVisible,
        currency = currencyCode,
        initialAmount = amount,
        showPlusMinus = true,
        dismiss = { amountModalVisible = false }
    ) { newAmount ->
        amount = newAmount

        if (modal?.adjustBalanceMode == true) {
            save(
                account = account,
                nameTextFieldValue = nameTextFieldValue,
                currency = currencyCode,
                color = color,
                icon = icon,
                amount = newAmount,
                includeInBalance = includeInBalance,
                appearance = appearance,

                onCreateAccount = onCreateAccount,
                onEditAccount = onEditAccount,
                dismiss = { close(appearance) }
            )
        }
    }

    if (logoPickerVisible) {
        LogoPickerSheet(
            accountName = nameTextFieldValue.text,
            accountColor = color,
            selectedKey = appearance.logoKey,
            hasCustomLogo = appearance.customLogoPath != null,
            onChoice = { choice ->
                logoPickerVisible = false
                // A registry logo, initials or none replace an upload; Upload keeps the key for later.
                fun pick(key: String?) {
                    appearance = appearance.copy(logoKey = key, customLogoPath = null)
                }
                when (choice) {
                    LogoChoice.Upload -> pickLogo?.invoke()
                    LogoChoice.Initials -> pick(Logos.InitialsKey)
                    LogoChoice.None -> pick(null)
                    is LogoChoice.Registry -> pick(choice.key)
                }
            },
            onDismiss = { logoPickerVisible = false },
        )
    }

    if (backgroundSheetVisible) {
        AccountBackgroundSheet(
            appearance = appearance,
            accountName = nameTextFieldValue.text,
            accountColor = color,
            surface = UI.colors.medium,
            onChange = { appearance = it },
            onPickImage = pickBackground,
            onDismiss = { backgroundSheetVisible = false },
        )
    }

    CurrencyModal(
        title = stringResource(R.string.choose_currency),
        initialCurrency = IvyCurrency.fromCode(currencyCode),
        visible = currencyModalVisible,
        dismiss = { currencyModalVisible = false }
    ) {
        currencyCode = it

//        if (IvyCurrency.fromCode(it)?.isCrypto == true) {
//            if (getCustomIconId(context = context, iconName = it, size = "m") != null) {
//                icon = it
//            }
//        }
    }

    ChooseIconModal(
        visible = chooseIconModalVisible,
        initialIcon = icon ?: "account",
        color = color,
        dismiss = { chooseIconModalVisible = false }
    ) {
        icon = it
    }
}

private fun save(
    account: Account?,
    nameTextFieldValue: TextFieldValue,
    currency: String,
    color: Color,
    icon: String?,
    amount: Double,
    includeInBalance: Boolean,
    appearance: AccountAppearance,

    onCreateAccount: (CreateAccountData) -> Unit,
    onEditAccount: (Account, balance: Double) -> Unit,
    dismiss: () -> Unit
) {
    if (account != null) {
        onEditAccount(
            account.copy(
                name = nameTextFieldValue.text.trim(),
                currency = currency,
                includeInBalance = includeInBalance,
                icon = icon,
                color = color.toArgb(),
                appearance = appearance,
            ),
            amount
        )
    } else {
        onCreateAccount(
            CreateAccountData(
                name = nameTextFieldValue.text.trim(),
                currency = currency,
                color = color,
                icon = icon,
                balance = amount,
                includeBalance = includeInBalance,
                appearance = appearance,
            )
        )
    }

    dismiss()
}

/** Shows the selected account logo and opens its picker. */
@Composable
private fun AccountLogoSelectorRow(
    appearance: AccountAppearance,
    name: String,
    color: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(UI.shapes.r4)
            .background(UI.colors.medium, UI.shapes.r4)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("account_modal_logo"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.logo),
            style = UI.typo.b2.style(fontWeight = FontWeight.Bold),
        )
        if (hasAccountLogo(appearance.logoKey, appearance.customLogoPath)) {
            AccountLogo(
                logoKey = appearance.logoKey,
                customLogoPath = appearance.customLogoPath,
                name = name,
                color = color,
                size = 32.dp,
                contentDescription = stringResource(R.string.account_logo_description, name),
            )
        } else {
            Text(
                text = stringResource(R.string.logo_none),
                style = UI.typo.b2.style(color = Gray, fontWeight = FontWeight.SemiBold),
            )
        }
    }
}

/** Shows the selected account background and opens its editor. */
@Composable
private fun AccountBackgroundSelectorRow(appearance: AccountAppearance, color: Color, onClick: () -> Unit) {
    val surface = UI.colors.medium
    val paint = remember(appearance, color, surface) { accountPaint(appearance, color, surface) }
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(UI.shapes.r4)
            .background(surface, UI.shapes.r4)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .testTag("account_modal_background"),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(R.string.account_background),
            style = UI.typo.b2.style(fontWeight = FontWeight.Bold),
        )
        Box(
            Modifier
                .size(width = 56.dp, height = 32.dp)
                .clip(UI.shapes.rFull)
                .accountBackground(paint)
        )
    }
}

@Composable
private fun AccountCurrency(
    currencyCode: String,

    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .background(UI.colors.medium, UI.shapes.r4)
            .clip(UI.shapes.r4)
            .clickable {
                onClick()
            }
            .padding(vertical = 24.dp)
            .testTag("account_modal_currency"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(Modifier.width(32.dp))

        Text(
            text = currencyCode.toUpperCaseLocal(),
            style = UI.typo.b1.style(
                fontWeight = FontWeight.ExtraBold
            )
        )

        Spacer(Modifier.weight(1f))

        val currencyName = IvyCurrency.fromCode(currencyCode)?.name ?: ""
        Text(
            text = "-$currencyName".toLowerCaseLocal(),
            style = UI.typo.b2.style(
                fontWeight = FontWeight.SemiBold,
                color = Gray
            )
        )

        Spacer(Modifier.width(24.dp))
    }
}

@Preview
@Composable
private fun Preview() {
    IvyWalletPreview {
        AccountModal(
            modal = AccountModalData(
                account = null,
                baseCurrency = "BGN",
                balance = 0.0
            ),
            onCreateAccount = { },
            onEditAccount = { _, _ -> }
        ) {
        }
    }
}
