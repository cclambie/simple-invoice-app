# Simple Invoice (Android)

A basic, offline-first Android app for creating invoices. Built with Kotlin, Jetpack Compose,
and Room. Lets a small business create and keep track of invoices and payments due/paid, with
a simple periodic report for accounting.

## Features

- **Clients**: client name plus an optional contact name, email, phone, address, notes and
  billing currency. Add clients manually, or import from your phone's contacts (requires the
  Contacts permission).
- **Business details**: business name, address, contact info, tax registration (GST, VAT or a
  custom label), default tax rate, default currency and default payment terms (days until due,
  7 by default).
- **Payment details**: bank details per currency, labelled the way local banks do (BSB for
  AUD, sort code for GBP, routing number for USD, BIC/IBAN for EUR), plus an optional
  international option (PayPal, Wise, Revolut…) with a link or account. Printed at the bottom
  of each invoice, using the account for the invoice's currency.
- **Invoices**: line items (description, quantity, unit price), issue/due dates, tax rate,
  currency and notes, with live subtotal/tax/total. Invoice numbers continue from the last
  invoice created (`008` → `009`, `INV-0041` → `INV-0042`).
- **Saved items**: save a line item's description and price, then add it to later invoices.
- **PDF**: a standard A4 invoice PDF. **Send PDF** opens the Android share sheet (email,
  messaging, cloud drives…) with the client's email, subject and message filled in.
  **Save PDF** saves it wherever you choose.
- **Mark as paid**: toggle an invoice between Paid and Unpaid; the paid date is recorded.
- **Multi-currency**: every invoice has its own currency, defaulting to the client's billing
  currency, otherwise the business default.
- **Reports**: pick a date range to see invoice counts and totals per currency, then export the
  invoices in that range as CSV for a spreadsheet or accountant.
- **Backup & restore** (Business → Backup): see below.

All data is stored on-device in a Room (SQLite) database. There is no backend or account.

## Backup & restore

The database lives in the app's private storage
(`/data/data/com.simpleinvoice.app/databases/simple_invoice.db`), which other apps can't read.
Android's built-in cloud backup (Google One) also includes it, but that copy can't be browsed
or restored file by file.

The Backup screen lets the user pick any folder through the system folder picker:

- **Back up now**: writes `SimpleInvoice-backup-YYYY-MM-DD.db` (one file per day, overwritten
  through the day). With no folder chosen, it asks for a single file location instead, which
  also works for Google Drive.
- **Automatic backup**: backs up a few seconds after any change.
- **Keep N daily backups**: 7 by default; older backup files in the folder are deleted.
- **Also save invoice PDFs**: keeps a PDF of every invoice in an `Invoices/` subfolder.
- **Restore from backup…**: checks the file is a Simple Invoice backup that isn't from a newer
  app version, keeps the current data as `files/pre-restore.db`, swaps in the backup and
  restarts the app. Older backups are upgraded by the normal database migrations.

Syncing off the phone is left to existing tools (to be documented on the website): pick a
folder that Nextcloud, Syncthing, FolderSync (Google Drive, SFTP…) or Termux + rsync keeps in
sync. Google Drive's folder picker doesn't allow ongoing writes, so automatic backups to Drive
need a local folder plus a sync app.

## Monetisation

Ad-supported free use with two paid tiers. The tiers, ads (Google test ads) and every rule below
are built; Google Play Billing isn't yet, so the tier is chosen from developer options on the
Plans screen (Business → Plans) in debug builds. Tier names are placeholders.

### Free (ad supported)

Ad load depends on how many invoices were created in the current calendar month:

| Invoices this month | Ads |
|---|---|
| 1–3 | Footer banner only, no pop-ups |
| 4–7 | Footer banner, plus a pop-up (interstitial) when sending or saving an invoice PDF |
| More than 7 | Footer banner, plus pop-ups on app open and on invoice send/save |

- **No report CSV export.**
- **Invoice PDFs**: saving PDFs (Save PDF, and the "Also save invoice PDFs" backup option)
  follows the same invoice-count tiers as Send PDF above.
- **Backup**: pressing Back up now / Restore first shows an explanation ("Free mode: ad
  supported"), then a pop-up ad, then runs the backup.

### Premium: $1/month

- No pop-up ads (footer banner stays).
- Report CSV export, limited to a 1-month range at a time. Longer ranges show "Upgrade to get
  more than 1 month export at a time".
- Manual backup and restore without ads. Turning on automatic backup shows the toast "Upgrade
  to get automated backups".

### Premium Plus: $2/month

- Everything in Premium, plus:
- No ads at all.
- Report export for any date range.
- Automated backup.

Note: the automatic "Also save invoice PDFs" option runs in the background, where a pop-up ad
can't be shown, so for free users it should only run as part of a manual backup (after its ad).

### Implementation

- Rules: `plan/Tier.kt` (what each tier unlocks) and `AdRules` (when pop-ups appear). Invoices
  are counted by `issueDate` within the current calendar month.
- Current tier: `plan/PlanRepository.kt`. Debug builds can also pretend an invoice count
  (Plans → developer options) to try each ad level.
- Ads: `ads/AdManager.kt` keeps one pop-up preloaded and never blocks the user if none is
  ready; after a pop-up closes, a toast suggests Premium. `ads/BannerAd.kt` is the footer.
- Gates: footer in `ui/nav/SimpleInvoiceNavHost.kt`; app-open pop-up in `MainActivity`;
  Send/Save PDF in `InvoiceDetailScreen`; CSV export and its 1-month limit in `ReportsScreen`;
  backup/restore ads and the automatic-backup lock in `BackupScreen`, with `BackupManager`
  also refusing automatic backups for lower tiers so a downgrade takes effect.

### Before publishing

- **AdMob IDs**: `app/build.gradle.kts` uses Google's public test IDs. Create the app and ad
  units in AdMob and put the real IDs in a release-only config.
- **Billing**: add Google Play Billing with two subscription products, replace the developer
  switch with the purchased tier (cached locally so the app works offline), and enable the
  Subscribe buttons on the Plans screen. Billing can only be tested once the app is on a Play
  Console test track.
- **Consent**: add Google's UMP consent form for EU/UK users before requesting ads.
- **Play Console**: declare ads and the advertising ID in the Data safety form.

## Requirements

- Android Studio (Koala or newer recommended)
- JDK 17
- Android SDK with API 35 installed (compileSdk/targetSdk 35, minSdk 26 / Android 8.0+)
- Kotlin 2.2 (via the Gradle plugin; nothing to install)

## Building

Open the repository root directly in Android Studio and let it sync, or from the command line:

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`. Install it on a
connected device with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.

## Project structure

```
app/src/main/java/com/simpleinvoice/app/
  data/model/     Room entities (BusinessProfile, Client, Invoice, InvoiceLineItem,
                  SavedItem, PaymentAccount)
  data/db/        DAOs, AppDatabase (with migrations), type converters
  repository/     Repository layer wrapping the DAOs (business logic, transactions)
  backup/         BackupManager: folder backups, pruning, PDF export, restore
  ui/screens/     One package per feature: invoices, clients, reports, business, backup
  ui/nav/         Navigation graph + bottom navigation bar
  ui/components/  Shared composables (DateField, CurrencyPicker)
  ui/theme/       Material 3 theme
  util/           Formatting, invoice PDF, CSV export, contact import, ViewModel helper
```

There's no backend and no DI framework; dependency wiring is done manually in
`SimpleInvoiceApp` (the `Application` subclass) to keep the project simple and easy to read.
