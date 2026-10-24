# Apex AI Assistant

Native Android Java prototype with proposal review, local policy checks, private-zone target exclusions, a kill switch, and local history.

## Safety status
This is an early prototype. AI is not configured, execution is disabled, and proposal approval only records user consent. It does not control other apps. Exclusion rules only govern this app's own workflow; an ordinary app cannot enforce system-wide private zones or comprehensively protect other apps' data and notifications.

## Get the latest APK
After a successful **Android Debug Build** workflow, the latest debug APK is published at [release/apex-debug.apk](release/apex-debug.apk) in this repository. On GitHub, open that file and choose **Download raw file**. A copy is also retained as the workflow artifact for 7 days.

The build bot commits the APK after each source update. Commits that only update the APK do not start another build.

## Build
GitHub Actions builds a debug APK on pushes to `main` or by manual workflow dispatch. The workflow uses Gradle 8.13 installed on its hosted runner.

## Current features
- Home, Proposal, Policy, and History screens
- Local proposal drafting and review
- Kill switch and excluded-target checks
- Locally retained history (up to 100 records)
- Optional tap-to-talk voice drafting through Android speech recognition (microphone permission requested only after the user taps Speak)
- No AI provider or action executor configured

## Privacy note
Proposal task descriptions may appear in local activity history when approved. Voice recognition is handled by the Android speech recognition provider available on the device and may follow that provider's data practices; voice input is opt-in. Review transcripts before saving. Avoid entering passwords, financial details, or other secrets. Proposal and history records stay in this app's local preferences in the current version.
