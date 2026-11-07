# Plan: username change, Telegram group, Support section

One commit per phase. `./gradlew :app:assembleDebug` runs after every phase.

## Values

| Item | Value | Source |
|---|---|---|
| Old GitHub username | `Fahimofficial` | README, CONTRIBUTING, `FernLinks.GITHUB_OWNER`, CODEOWNERS |
| New GitHub username | `SimplyFahimOfficial` | given |
| Telegram group | `https://t.me/Fernwallet` | given |
| Instagram | `SimplyFahimofficial` → `https://instagram.com/SimplyFahimofficial` | README Contact section |
| Email | `simplyfahimofficial@gmail.com` (only one exists) | README Contact section |
| Personal Telegram | **not found in the repo**: placeholder in `SupportDestinations` (AppLinks.kt), hidden until set | – |

`git remote -v` already points at `SimplyFahimOfficial/Fern-wallet` (through the sandbox gateway).
Note: the old name is a case-insensitive substring of the Instagram handle and the email
(`Simply`**`Fahimofficial`**). Those are not GitHub links and stay as they are.

## Phases

1. **Username.** Replace `Fahimofficial` (GitHub owner only) in README, CONTRIBUTING, CHANGELOG,
   SECURITY, PRIVACY, docs/, .github/ (CODEOWNERS, issue templates, PR template, fdroid workflow),
   fastlane changelog, build.gradle.kts (wrapper-upgrade repo), ci-actions, and `FernLinks` /
   `SupportDestinations.GITHUB_PROFILE` (every in-app link: hub open-source card, Share Fern,
   Support screen, report issue, releases API). Update unit tests and Paparazzi goldens that show the URL.
   No `FUNDING.yml` and no `ISSUE_TEMPLATE/config.yml` exist; neither is created.
2. **Telegram in README header.** A for-the-badge "Join the Telegram group" badge under the
   existing badges.
3. **In-app Support screen.** Order: UPI card, Ko-fi row, Telegram hero card, "Personal contact"
   tiles (Instagram, email, personal Telegram when set), then the existing "Project and contact"
   rows unchanged. One `openLink()` helper, long-press copy, tick haptic, entrance fade using
   `IvyMotion`. New icons: Telegram and Instagram from Simple Icons (CC0), mail from Material Icons
   (Apache-2.0). Unit tests for the link builders.
4. **README Support redesign.** Self-hosted SVG banner and card graphics in
   `docs/assets/support/` with light/dark variants; UPI → Ko-fi → Telegram → personal contact.

## Current README Support section inventory (must all survive phase 4)

Heading and anchor
- H2 `## 💚 Support the Project` (anchor `#-support-the-project`, used by the top ToC link)

Text
- "Fern Wallet is free and ad-free. If it helps you keep your money in order, a small donation keeps development going."
- H3 "👆 **Tap the QR to open UPI payment options**"
- "**Payee:** Md Fahim"
- "**UPI ID**"
- Code block `SimplyFahim@sbi`
- `<sub>` "You choose the amount. Check that your UPI app shows <b>Md Fahim</b> before you pay."

Links
- `https://simplyfahim.pages.dev/pay/` (QR image link)
- `https://simplyfahim.pages.dev/pay/` (Pay with UPI badge)
- `https://ko-fi.com/simplyfahim` (Ko-fi badge)

Images
- `docs/assets/support/upi-qr.png`, width 220, alt "UPI QR code to pay Md Fahim, UPI ID SimplyFahim@sbi. Tap to open UPI payment options."
- shields.io "Pay with UPI" badge (`009E3B`, logo googlepay), height 44, alt "Pay with UPI"
- shields.io "Support me on Ko-fi" badge (`FF5E5B`, logo kofi), height 44, alt "Support me on Ko-fi"

The separate `## 📬 Contact` section is not part of the Support section and only gets the username fix.
