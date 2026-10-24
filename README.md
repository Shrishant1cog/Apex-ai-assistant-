# Apex AI Assistant

Native Android Java prototype with proposal review, local policy checks, private-zone target exclusions, a kill switch, and local history.

## Safety status
This is an early prototype. AI is not configured, execution is disabled, and proposal approval only records user consent. It does not control other apps. Exclusion rules only govern this app's own workflow; an ordinary app cannot enforce system-wide private zones or comprehensively protect other apps' data and notifications.

## Build
GitHub Actions builds a debug APK on pushes to `main`. Open **Actions → Android Debug Build → a successful run → Artifacts**, then extract the APK.

The workflow uses Gradle 8.13 installed on its hosted runner. A Gradle wrapper JAR is not currently included in this repository; local wrapper builds are therefore not available until the official wrapper JAR is added.

## Current features
- Home, Proposal, Policy, and History screens
- Local proposal drafting and review
- Kill switch and excluded-target checks
- Locally retained history (up to 100 records)
- No special Android permissions or AI provider configured

## Privacy note
Proposal task descriptions may appear in local activity history when approved. Avoid entering passwords, financial details, or other secrets. Data stays in this app's local preferences in the current version.
