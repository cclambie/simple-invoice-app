# Invoice Maker Small Business (Android)

Package `com.craiglambie.invoicemaker` (fixed once published), title "Invoice Maker Small Business".
Kotlin sources are in `app/src/main/java/com/craiglambie/invoicemaker/`. Class names `SimpleInvoice*`,
the theme `Theme.SimpleInvoice` and the DB file `simple_invoice.db` keep their old names on purpose.

## Build and run

```
JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64 ./gradlew assembleDebug
adb -s <serial> install -r app/build/outputs/apk/debug/app-debug.apk
```

Always target a device with `-s`; more than one device may be attached. Test the app on a real phone;
the emulator (`invoice_test` AVD) is a last resort.

## Rules learned the hard way

- **Room migrations:** the DB is at version 6, with migrations 1→2 through 5→6 in `data/db/AppDatabase.kt`.
  Never put `@ColumnInfo(defaultValue=…)` on new columns. Changing one changes the identity hash and
  crashes existing installs. Set defaults in the migration SQL instead.
- **Versions are pinned together:** play-services-ads 25.5.0 needs Kotlin 2.2.21 with the compose
  plugin, KSP 2.2.21-2.0.5 (newer KSP needs a newer AGP than 8.5.2), Room 2.7.2, and compileSdk 35.
  Older ads SDKs failed to load ads.
- **Play policy:** no "free" in the title or icon.
- **Ads:** they use Google's test IDs (`app/build.gradle.kts`). Before publishing, add real AdMob IDs,
  Play Billing for Premium ($1) and Premium Plus ($2), the UMP consent form and the Data safety form.
  The tier rules are in `plan/Tier.kt`. Debug builds have a plan and invoice-count override on the
  Plans screen.

## Related

- **Website:** the app's landing page, backup guide and privacy policy live in the Lambie Labs repo
  (`../lambie-labs`, github.com/cclambie/lambie-labs), at
  https://labs.craiglambie.com/invoice-maker-small-business/.
- **Store listing:** `store/listing.md`, with icons and screenshots alongside. Screenshots use
  fictional data from `tools/make_demo_db.py`, never real customer data.
- **Test APK for other testers:** goes in `dist/`, which is gitignored.
