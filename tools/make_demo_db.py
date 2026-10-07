"""Builds a database of fictional demo data for screenshots.

Usage: make_demo_db.py <template.db> <out.db> <invoice_count>

<template.db> is any backup made by the current app version; it supplies the schema (including
Room's identity hash). All of its rows are deleted before the demo data is added.
"""
import datetime
import shutil
import sqlite3
import sys

template, out, count = sys.argv[1], sys.argv[2], int(sys.argv[3])
shutil.copyfile(template, out)
db = sqlite3.connect(out)
for table in ("invoice_line_items", "invoices", "clients", "saved_items", "payment_accounts", "business_profile"):
    db.execute(f"DELETE FROM {table}")
db.execute("DELETE FROM sqlite_sequence")

db.execute(
    "INSERT INTO business_profile (id, businessName, address, email, phone, taxIdType, taxIdCustomLabel, "
    "taxIdNumber, defaultTaxRatePercent, defaultDueDays, currencyCode, internationalPaymentService, "
    "internationalPaymentLink) VALUES (1, ?, ?, ?, ?, 'GST', '', ?, 10, 7, 'AUD', 'Wise', ?)",
    ("Harbour Lane Design", "12 Example Street\nFitzroy VIC 3065", "hello@harbourlane.example",
     "0400 000 000", "12 345 678 901", "pay@harbourlane.example"),
)
db.executemany(
    "INSERT INTO payment_accounts (currencyCode, accountName, bankCode, accountNumber) VALUES (?, ?, ?, ?)",
    [("AUD", "Harbour Lane Design", "062-000", "1234 5678"),
     ("GBP", "Harbour Lane Design", "04-00-04", "12345678")],
)

clients = [
    ("Kestrel Coffee Co", "Mia Chen", "accounts@kestrel.example", "03 9000 0000", "88 Sample Road\nCollingwood VIC 3066", None),
    ("Thames & Finch Ltd", "Oliver Hart", "finance@thamesfinch.example", "020 7946 0000", "5 Demo Lane\nLondon EC1A 1AA", "GBP"),
    ("Bluebird Apps Inc", "Ava Martinez", "ap@bluebird.example", "+1 555 0100", "400 Placeholder Ave\nAustin TX 78701", "USD"),
    ("Saltwater Surf School", "Jack Wilson", "jack@saltwater.example", "0400 111 222", "1 Beach Parade\nTorquay VIC 3228", None),
]
for name, contact, email, phone, address, currency in clients:
    db.execute(
        "INSERT INTO clients (name, contactName, email, phone, address, notes, currencyCode) VALUES (?, ?, ?, ?, ?, '', ?)",
        (name, contact, email, phone, address, currency),
    )

db.executemany(
    "INSERT INTO saved_items (description, unitPrice) VALUES (?, ?)",
    [("Design consulting (hourly)", 120), ("Logo design package", 950), ("Website maintenance (monthly)", 280)],
)

# (client index, items, paid) cycling to give a believable mix of currencies and statuses.
templates = [
    (0, [("Logo design package", 1, 950), ("Business card layout", 1, 180)], True),
    (1, [("Brand guidelines document", 1, 1400)], False),
    (2, [("App store screenshots", 8, 45), ("Icon refresh", 1, 600)], True),
    (3, [("Website maintenance (monthly)", 1, 280)], False),
    (0, [("Design consulting (hourly)", 6, 120)], False),
    (1, [("Social media templates", 12, 35)], True),
    (3, [("Poster design", 2, 220), ("Print liaison", 1, 90)], True),
    (2, [("Design consulting (hourly)", 10, 95)], False),
    (0, [("Menu redesign", 1, 760)], False),
]
today = datetime.date.today()
first = today.replace(day=1)
currencies = {0: "AUD", 1: "GBP", 2: "USD", 3: "AUD"}
for i in range(count):
    client, items, paid = templates[i % len(templates)]
    issue = min(first + datetime.timedelta(days=i * 3 % max(today.day, 1)), today)
    due = issue + datetime.timedelta(days=7)
    tax = 10.0 if currencies[client] == "AUD" else 0.0
    cur = db.execute(
        "INSERT INTO invoices (invoiceNumber, clientId, issueDate, dueDate, taxRatePercent, notes, isPaid, paidDate, currencyCode) "
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
        (f"INV-{1041 + i:04d}", client + 1, issue.toordinal() - 719163, due.toordinal() - 719163, tax,
         "Thanks for your business!" if i % 2 == 0 else "", int(paid),
         (issue.toordinal() - 719163 + 3) if paid else None, currencies[client]),
    )
    for description, quantity, price in items:
        db.execute(
            "INSERT INTO invoice_line_items (invoiceId, description, quantity, unitPrice) VALUES (?, ?, ?, ?)",
            (cur.lastrowid, description, quantity, price),
        )

db.commit()
db.execute("VACUUM")
db.close()
