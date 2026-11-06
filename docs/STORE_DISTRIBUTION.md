# Store distribution

## Google Play

The `.github/workflows/play-publish.yml` workflow uploads the signed AAB attached to an existing stable GitHub release. It is deliberately manual and never builds or signs from a pull request.

One-time setup:

1. Create the app in Play Console with package name `com.fernwallet.app` and complete the initial Play Console setup. The first upload may need to be made manually.
2. Enable the Google Play Android Developer API in the Google Cloud project.
3. Configure Workload Identity Federation for GitHub Actions. Restrict the provider's attribute condition to this repository and the `play-store` GitHub environment. Do not create a downloadable service-account JSON key.
4. Create a service account with only the Play Console app permissions needed to upload releases for this app. Add it in Play Console API access.
5. Create a GitHub environment named `play-store`, restrict deployments to the default branch, and add required reviewers. Add these environment variables:
   - `PLAY_WIF_PROVIDER`: the full Workload Identity Provider resource name.
   - `PLAY_SERVICE_ACCOUNT`: the service-account email address.
6. Publish the GitHub draft release after reviewing it. Run **Actions → Publish to Google Play**, enter the same stable tag, and choose the track. Production uploads are created as drafts and still require a human to finish publishing in Play Console.

The Play upload uses the AAB already signed and verified by `release.yml`; it does not have access to the APK release-signing secrets.

## F-Droid status

This source tree is not currently ready for the official F-Droid repository. It includes Google Play-only dependencies (Play In-App Updates, Play review, Play Services Tasks, and the Play Services coroutine bridge). F-Droid's main repository requires free/open-source software and does not accept proprietary Google Play Services dependencies.

The correct next step is a separate FOSS build variant that removes or replaces those integrations, followed by a public-source build recipe and F-Droid metadata submission for review. The official F-Droid repository is not a direct GitHub Actions APK-upload target. Do not treat a successful GitHub APK build as F-Droid eligibility.
