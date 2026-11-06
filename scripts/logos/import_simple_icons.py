"""
Import account logos from an unpacked `simple-icons` npm package.

Usage: python3 scripts/logos/import_simple_icons.py path/to/simple-icons/package

Writes shared/ui/core/res/drawable/logo_*.xml, LogoCatalog.kt and LOGO_LICENSES.md. A glyph is
used only when its Simple Icons entry is unrestricted (see clean()); other brands get initials.
"""
import json
import os
import re
import sys

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
SI = sys.argv[1]
VERSION = json.load(open(SI + "/package.json"))["version"]
data = {i["slug"]: i for i in json.load(open(SI + "/data/simple-icons.json"))}

# id, display name, aliases, category, simple-icons slug or None, fallback brand colour (initials)
BRANDS = [
    # Indian banks and fintech
    ("sbi", "SBI", ["state bank of india", "yono"], "Banks", None, "1A4C9C"),
    ("hdfcbank", "HDFC Bank", ["hdfc"], "Banks", "hdfcbank", None),
    ("icicibank", "ICICI Bank", ["icici"], "Banks", "icicibank", None),
    ("axisbank", "Axis Bank", ["axis"], "Banks", "axisbank", None),
    ("kotak", "Kotak Mahindra Bank", ["kotak", "811", "kotak 811"], "Banks", None, "ED1C24"),
    ("idfcfirst", "IDFC FIRST Bank", ["idfc", "idfc first"], "Banks", None, "9C1D26"),
    ("indusind", "IndusInd Bank", ["indusind", "indie"], "Banks", None, "8B1D41"),
    ("yesbank", "Yes Bank", ["yes"], "Banks", None, "00518F"),
    ("canara", "Canara Bank", ["canara"], "Banks", None, "0091D0"),
    ("pnb", "Punjab National Bank", ["pnb", "punjab national"], "Banks", None, "A20A3A"),
    ("bankofbaroda", "Bank of Baroda", ["bob", "baroda"], "Banks", None, "F26522"),
    ("unionbank", "Union Bank of India", ["union bank", "ubi"], "Banks", None, "E31E24"),
    ("bankofindia", "Bank of India", ["boi"], "Banks", None, "F37021"),
    ("idbi", "IDBI Bank", ["idbi"], "Banks", None, "00836C"),
    ("federal", "Federal Bank", ["federal"], "Banks", None, "1D3E8C"),
    ("aubank", "AU Small Finance Bank", ["au", "au sfb", "au bank"], "Banks", None, "6D2077"),
    ("slice", "slice", ["slice sfb", "slice card"], "Banks", None, "6B3FF2"),
    ("jupiter", "Jupiter", ["jupiter money"], "Banks", None, "F37336"),
    ("fi", "Fi", ["fi money", "epifi"], "Banks", "fi", None),
    # UPI and wallets
    ("upi", "UPI", ["unified payments interface"], "UPI & wallets", None, "097939"),
    ("bhim", "BHIM", ["bhim upi"], "UPI & wallets", None, "00A99D"),
    ("googlepay", "Google Pay", ["gpay", "tez"], "UPI & wallets", "googlepay", None),
    ("phonepe", "PhonePe", ["phone pe"], "UPI & wallets", "phonepe", None),
    ("paytm", "Paytm", ["paytm wallet"], "UPI & wallets", "paytm", None),
    ("amazonpay", "Amazon Pay", ["amazon"], "UPI & wallets", None, "FF9900"),
    ("cred", "CRED", ["cred upi"], "UPI & wallets", None, "1C1C1C"),
    ("mobikwik", "MobiKwik", ["mobikwik wallet"], "UPI & wallets", None, "1D7FE1"),
    ("freecharge", "Freecharge", ["free charge"], "UPI & wallets", None, "EC6B1D"),
    # Investing
    ("zerodha", "Zerodha", ["kite"], "Investing", "zerodha", None),
    ("groww", "Groww", ["grow"], "Investing", None, "00D09C"),
    ("upstox", "Upstox", ["upstox pro"], "Investing", None, "5A2D82"),
    # Cards and networks
    ("visa", "Visa", ["visa card"], "Cards", "visa", None),
    ("mastercard", "Mastercard", ["master card"], "Cards", None, "EB001B"),
    ("rupay", "RuPay", ["rupay card"], "Cards", None, "097A44"),
    ("americanexpress", "American Express", ["amex"], "Cards", "americanexpress", None),
    ("dinersclub", "Diners Club", ["diners"], "Cards", "dinersclub", None),
    # Global
    ("paypal", "PayPal", ["pay pal"], "Global", None, "003087"),
    ("applepay", "Apple Pay", ["apple"], "Global", None, "000000"),
    ("wise", "Wise", ["transferwise"], "Global", None, "9FE870"),
    ("revolut", "Revolut", ["revolut bank"], "Global", None, "191C1F"),
    ("chase", "Chase", ["jpmorgan", "jp morgan"], "Global", "chase", None),
    ("bankofamerica", "Bank of America", ["bofa", "boa"], "Global", "bankofamerica", None),
    ("citi", "Citi", ["citibank"], "Global", None, "003B70"),
    ("hsbc", "HSBC", ["hsbc bank"], "Global", None, "DB0011"),
    ("cashapp", "Cash App", ["square cash"], "Global", "cashapp", None),
    ("venmo", "Venmo", ["venmo app"], "Global", None, "008CFF"),
    ("bitcoin", "Bitcoin", ["btc", "crypto"], "Global", "bitcoin", None),
    ("ethereum", "Ethereum", ["eth", "crypto"], "Global", "ethereum", None),
]


def clean(slug):
    """Use a Simple Icons glyph only when its entry has no guidelines or licence restrictions.

    Its source must not be a brand-guidelines page.
    """
    e = data.get(slug)
    if e is None:
        return False, "not in Simple Icons"
    if e.get("guidelines"):
        return False, "brand guidelines restrict use: " + e["guidelines"]
    if e.get("license"):
        return False, "own licence: " + json.dumps(e["license"])
    src = e.get("source", "")
    if re.search(r"guideline|marketing|brand", src, re.I):
        return False, "source is a brand/marketing page: " + src
    return True, src


drawable_dir = ROOT + "/shared/ui/core/src/main/res/drawable"
licenses = []
initials = []
kotlin_entries = []
for (id_, name, aliases, cat, slug, fallback) in BRANDS:
    ok, why = (False, "not in Simple Icons") if slug is None else clean(slug)
    probe_slug = slug or id_
    if slug is None and probe_slug in data:
        ok, why = clean(probe_slug)
        slug = probe_slug if ok else None
    if ok:
        e = data[slug]
        svg = open(f"{SI}/icons/{slug}.svg").read()
        d = re.search(r' d="([^"]+)"', svg).group(1)
        xml = (
            '<?xml version="1.0" encoding="utf-8"?>\n'
            f'<!-- {e["title"]}: Simple Icons {VERSION} ({slug}.svg), CC0 1.0. Trademark of its owner. -->\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp"\n    android:height="24dp"\n'
            '    android:viewportWidth="24"\n    android:viewportHeight="24">\n'
            f'    <path\n        android:fillColor="#FFFFFFFF"\n        android:pathData="{d}" />\n'
            '</vector>\n'
        )
        open(f"{drawable_dir}/logo_{id_}.xml", "w").write(xml)
        color = e["hex"]
        licenses.append((name, f"logo_{id_}.xml", f"Simple Icons {VERSION}, `{slug}.svg`", e.get("source", ""), "CC0 1.0 (icon data); trademark of its owner"))
        art = f"LogoArt.Glyph(R.drawable.logo_{id_})"
    else:
        color = fallback
        initials.append((name, why))
        art = "LogoArt.Initials"
    alias_list = ", ".join(f'"{a}"' for a in aliases)
    alias_list = ", ".join(f'"{a}"' for a in aliases)
    cat_name = {"Banks": "Banks", "UPI & wallets": "Wallets", "Investing": "Investing", "Cards": "Cards", "Global": "Global"}[cat]
    kotlin_entries.append(
        f'    LogoSpec(\n        id = "{id_}",\n        name = "{name}",\n        aliases = listOf({alias_list}),\n'
        f'        category = LogoCategory.{cat_name},\n        art = {art},\n        brandColor = 0xFF{color.upper()},\n    ),'
    )

kt = f'''package com.ivy.ui.logo

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Toll
import androidx.compose.ui.graphics.vector.ImageVector
import com.ivy.ui.R

// Generated from Simple Icons {VERSION} by the logo import (see LOGO_LICENSES.md). Brands whose
// glyph couldn't be used cleanly are initials on the brand colour.
internal val BrandLogos: List<LogoSpec> = listOf(
{chr(10).join(kotlin_entries)}
)

// Material Icons (Apache 2.0), tinted to contrast with the account colour.
internal val GenericLogos: List<LogoSpec> = listOf(
    generic("generic_cash", "Cash", listOf("money", "notes"), Icons.Filled.Payments),
    generic("generic_wallet", "Wallet", listOf("purse"), Icons.Filled.AccountBalanceWallet),
    generic("generic_bank", "Bank", listOf("account"), Icons.Filled.AccountBalance),
    generic("generic_card", "Card", listOf("debit card"), Icons.Filled.Payment),
    generic("generic_credit_card", "Credit card", listOf("credit"), Icons.Filled.CreditCard),
    generic("generic_piggy_bank", "Piggy bank", listOf("piggy"), Icons.Filled.Savings),
    generic("generic_savings", "Savings", listOf("coins", "fund"), Icons.Filled.Toll),
    generic("generic_investment", "Investment", listOf("stocks", "shares", "mutual fund"), Icons.Filled.ShowChart),
)

private fun generic(id: String, name: String, aliases: List<String>, icon: ImageVector): LogoSpec =
    LogoSpec(id, name, aliases, LogoCategory.Generic, LogoArt.Vector(icon), brandColor = null)
'''
os.makedirs(ROOT + "/shared/ui/core/src/main/java/com/ivy/ui/logo", exist_ok=True)
open(ROOT + "/shared/ui/core/src/main/java/com/ivy/ui/logo/LogoCatalog.kt", "w").write(kt)

md = ["# Logo licences", "",
      "Account logos come from two sources. Nothing was drawn or recreated by hand.", "",
      f"- **Brand glyphs:** [Simple Icons](https://simpleicons.org) {VERSION}. The icon data is released under",
      "  CC0 1.0. Each mark is a trademark of its owner and is shown only to identify the user's own",
      "  account (nominative use), with no endorsement implied.",
      "- **Generic icons:** [Material Icons](https://fonts.google.com/icons), Apache License 2.0, through the",
      "  `material-icons-extended` dependency the app already has.", "",
      "A Simple Icons glyph is used only if its entry in `simple-icons.json` has no `guidelines` and",
      "no `license` field, and its `source` isn't a brand-guidelines or marketing page.",
      "Every other brand is shown as its initials on a brand colour, and no artwork is bundled for it.", "",
      "## Bundled brand glyphs", "",
      "| Brand | File | From | Upstream source | Licence |", "|---|---|---|---|---|"]
def markdown_urls(value):
    return re.sub(r"(https?://[^\s|]+)", r"<\1>", value)


for (n, f, frm, src, lic) in licenses:
    md.append(f"| {n} | `shared/ui/core/src/main/res/drawable/{f}` | {frm} | {markdown_urls(src)} | {lic} |")
md += ["", "## Shown as initials (no artwork bundled)", "", "| Brand | Why |", "|---|---|"]
for (n, why) in initials:
    md.append(f"| {n} | {markdown_urls(why)} |")
md += ["", "## Generic icons", "",
       "Cash (`Payments`), Wallet (`AccountBalanceWallet`), Bank (`AccountBalance`), Card (`Payment`),",
       "Credit card (`CreditCard`), Piggy bank (`Savings`), Savings (`Toll`) and Investment (`ShowChart`)",
       "come from Material Icons, Apache License 2.0.", "",
       "Before a store release, the app's owner should review this list and each brand's current",
       "rules. The import script re-checks the Simple Icons data each time it runs.", ""]
open(ROOT + "/LOGO_LICENSES.md", "w").write("\n".join(md))
print(len(licenses), "glyphs;", len(initials), "initials")
for n, w in initials:
    print(" -", n, ":", w)
