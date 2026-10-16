# Tasker Integration Guide

The Invoice Maker app can accept payment notifications from Tasker, automatically matching received payments to invoices and marking them as paid.

## How It Works

When your banking app (like Up Bank) sends a payment notification, Tasker can intercept it, extract the payment details, and send them to Invoice Maker. The app will:

1. Search for matching invoices using the payment amount, client name, and reference number
2. Automatically mark the invoice as paid if a match is found
3. Record the payment in the Payment Log (accessible from the ☰ menu)

## Setting Up Tasker

### 1. Create a Tasker Task

Create a new task in Tasker that will be triggered by banking app notifications. The task needs to send a broadcast intent with the payment details.

### 2. Send the Payment Intent

Use the **Send Intent** action in Tasker with these details:

- **Action**: `com.craiglambie.invoicemaker.RECORD_PAYMENT`
- **Package**: `com.craiglambie.invoicemaker`
- **Target**: `Broadcast Receiver`

### 3. Add Extras (Parameters)

The intent must include the following extras as string or number values:

| Extra Name | Type | Required | Description |
|-----------|------|----------|-------------|
| `amount` | Number | Yes | The payment amount (e.g., 330.00) |
| `currency` | String | No | ISO 4217 currency code (default: "AUD") |
| `reference` | String | Yes | Invoice reference from notification (e.g., "Inv001") |
| `clientName` | String | Yes | Client/payer name from notification (e.g., "John Smith") |
| `notes` | String | No | Optional notes about the payment |

### Example Task Configuration

For an **Up Bank** notification with text: "Received $330 From John Smith Reference: Inv001"

**Tasker Variables** (extract from notification):
```
%bank_amount = 330
%bank_name = John Smith
%bank_ref = Inv001
```

**Send Intent Action**:
- Action: `com.craiglambie.invoicemaker.RECORD_PAYMENT`
- Class: (leave blank)
- Package: `com.craiglambie.invoicemaker`
- Target: `Broadcast Receiver`

**Extras**:
```
amount:330 (type: number)
currency:AUD (type: string)
reference:Inv001 (type: string)
clientName:John Smith (type: string)
notes:Up Bank notification (type: string)
```

## Matching Logic

The app matches payments to invoices by checking:

1. **Amount**: The total invoice amount must equal the payment amount
2. **Client Name**: The invoice client name must contain or be contained in the payment client name (case-insensitive)
3. **Reference**: The invoice number must contain the payment reference (case-insensitive)

All three criteria must match for the invoice to be automatically marked as paid.

### Matching Example

- Invoice: INV-001, Client: "John Smith", Total: $330.00
- Payment: Amount: $330, Client: "John Smith", Reference: "Inv001"
- ✓ Match! (reference "Inv001" is contained in "INV-001")

## Viewing Payment Records

Go to **☰ Menu → Payment log** to see all payments recorded by Tasker:

- Shows which invoices were matched and marked paid
- Shows payments that were recorded but didn't match an invoice
- Each entry includes the timestamp and payment details

## Troubleshooting

### Payment not matching
- Check that the amount is exactly correct (including decimals)
- Verify the client name matches (even partial matches work, case-insensitive)
- Confirm the invoice reference is contained in the payment reference
- If the invoice is already marked paid, it won't be updated

### Intent not being received
- Verify the action name is exactly: `com.craiglambie.invoicemaker.RECORD_PAYMENT`
- Check that the package name is: `com.craiglambie.invoicemaker`
- Ensure the target is set to `Broadcast Receiver`
- Try manually testing the intent from Tasker

## Privacy & Security

- All payment data is stored locally on your device
- The app does not send payment data to any external service
- Payments are matched against your local invoice database only
- Payment logs can be viewed anytime via the Payment log screen
