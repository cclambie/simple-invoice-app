# Simple Invoice (Android)

A basic, offline-first Android app for creating invoices. Built with Kotlin, Jetpack Compose,
and Room. Lets a small business create and keep track of invoices and payments due/paid, with
a simple periodic report for accounting.

## Features

- **Clients** — add clients manually, or import them from your phone's contacts (name, phone,
  email are pulled in automatically; requires the Contacts permission).
- **Business details** — set your business name, address, contact info, and a tax registration
  number with a selectable type (GST, VAT, or a custom label), plus a default tax rate applied
  to new invoices.
- **Invoices** — create an invoice for a client with one or more line items (description,
  quantity, unit price), an auto-generated invoice number, issue/due dates, tax rate, and notes.
  Subtotal, tax, and total are calculated live.
- **Mark as paid** — toggle any invoice between Paid and Unpaid from its detail screen; the
  paid date is recorded automatically.
- **Reports** — pick a date range and see invoice count, total invoiced, total paid, and total
  outstanding, plus the list of invoices issued in that period.

All data is stored locally on-device in a Room (SQLite) database — there is no backend or sync.

## Requirements

- Android Studio (Koala or newer recommended)
- JDK 17
- Android SDK with API 34 installed (compileSdk/targetSdk 34, minSdk 26 / Android 8.0+)

## Building

Open the repository root directly in Android Studio and let it sync, or from
the command line:

```bash
./gradlew assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

> **Note:** this project was authored in a sandboxed environment without the Android SDK or
> Google's Maven repository reachable, so `./gradlew assembleDebug` has not been run
> end-to-end here. The Gradle wrapper (with jar) is committed and the project structure follows
> standard Android Gradle Plugin conventions, but please run a build locally / in CI before
> relying on it.

## Project structure

```
app/src/main/java/com/simpleinvoice/app/
  data/model/     Room entities (BusinessProfile, Client, Invoice, InvoiceLineItem) + TaxIdType
  data/db/        DAOs, AppDatabase, type converters
  repository/     Repository layer wrapping the DAOs (business logic, transactions)
  ui/screens/     One package per feature: clients, business, invoices, reports
  ui/nav/         Navigation graph + bottom navigation bar
  ui/components/  Small shared composables (e.g. DateField)
  ui/theme/       Material 3 theme
  util/           Formatting, contact import, ViewModel factory helper
```

There's no backend, no Hilt/DI framework, and no PDF export — line items and totals are plain
Kotlin data classes, and dependency wiring is done manually in `SimpleInvoiceApp` (the
`Application` subclass) to keep the project simple and easy to read.
