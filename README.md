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

## Monetisation plan (not built yet)

Everything above is currently unlocked. The plan is ad-supported free use with two paid tiers.
Tier names below are placeholders.

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

### Implementation notes

- Subscriptions: Google Play Billing (two subscription products). Cache the active tier
  locally so the app works offline, refreshing on launch.
- Ads: AdMob banner + interstitial. Respect consent (UMP SDK) for EU/UK users.
- Where the gates go:
  - Footer banner: the `Scaffold` bottom bar in `ui/nav/SimpleInvoiceNavHost.kt`.
  - App-open pop-up: `MainActivity`.
  - Send / Save PDF pop-up: `ui/screens/invoices/InvoiceDetailScreen.kt`.
  - CSV export and its range limit: `ui/screens/reports/ReportsScreen.kt`.
  - Manual backup/restore, automatic backup toggle and the PDF option:
    `ui/screens/backup/BackupScreen.kt`. `backup/BackupManager.kt` should also refuse automatic
    backups when the tier doesn't allow them, so a downgrade takes effect.
  - Monthly invoice count: count invoices by `issueDate`, or add a created-at column if the
    count should ignore back-dated invoices.

## Requirements

- Android Studio (Koala or newer recommended)
- JDK 17
- Android SDK with API 34 installed (compileSdk/targetSdk 34, minSdk 26 / Android 8.0+)

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
