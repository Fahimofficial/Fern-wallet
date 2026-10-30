# Contributing to Fern Wallet

Thanks for helping! Fern Wallet is a GPL-3.0 fork of [Ivy Wallet](https://github.com/Ivy-Apps/ivy-wallet),
maintained at https://github.com/Fahimofficial/Fern-wallet.

## 1. Find or open an issue

Browse [the issues](https://github.com/Fahimofficial/Fern-wallet/issues) or
[open a new one](https://github.com/Fahimofficial/Fern-wallet/issues/new/choose). For bigger changes, discuss
the idea in an issue first.

## 2. Fork, branch, build

```sh
git clone https://github.com/<you>/Fern-wallet.git
cd Fern-wallet
git checkout -b fix-issue-<number>
./gradlew :app:assembleDebug
```

Requirements: JDK 21 and the Android SDK (see the [README](README.md#build-from-source)).
Read the [Developer Guidelines](docs/Guidelines.md) before you start.

## 3. Before opening a pull request

Run the same checks as CI:

```sh
./gradlew testDebugUnitTest verifyPaparazziDebug detekt lintRelease
```

- Changes to financial calculations, date ranges or data import/export **must** include regression tests.
- Never add a destructive database migration; add a Room migration and a migration test instead.
- UI changes: record new Paparazzi screenshots (`./gradlew recordPaparazziDebug`) and include before/after images.
- Don't add analytics, ads, trackers or new network calls without discussing it first.

## 4. Open the PR against `main`

Fill in the [PR template](.github/PULL_REQUEST_TEMPLATE.md). The CI check requires a line like
`Closes #123` (or `Closes N/A`).
