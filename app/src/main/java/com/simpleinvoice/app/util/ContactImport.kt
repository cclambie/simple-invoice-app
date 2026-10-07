package com.simpleinvoice.app.util

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

data class ContactBasics(
    val contactId: String,
    val name: String,
    val phone: String,
    val email: String
)

/**
 * Reads the display name, first phone number, and first email address for a contact
 * picked via [androidx.activity.result.contract.ActivityResultContracts.PickContact].
 * Requires the READ_CONTACTS permission to already be granted.
 */
fun readContactBasics(context: Context, contactUri: Uri): ContactBasics? {
    val resolver = context.contentResolver
    val (contactId, name) = queryContactIdAndName(resolver, contactUri) ?: return null
    val phone = queryFirstValue(
        resolver,
        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
        ContactsContract.CommonDataKinds.Phone.NUMBER,
        ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
        contactId
    ).orEmpty()
    val email = queryFirstValue(
        resolver,
        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
        ContactsContract.CommonDataKinds.Email.ADDRESS,
        ContactsContract.CommonDataKinds.Email.CONTACT_ID,
        contactId
    ).orEmpty()
    return ContactBasics(contactId = contactId, name = name, phone = phone, email = email)
}

private fun queryContactIdAndName(resolver: ContentResolver, contactUri: Uri): Pair<String, String>? {
    resolver.query(
        contactUri,
        arrayOf(ContactsContract.Contacts._ID, ContactsContract.Contacts.DISPLAY_NAME),
        null,
        null,
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            val id = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts._ID))
            val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)).orEmpty()
            return id to name
        }
    }
    return null
}

private fun queryFirstValue(
    resolver: ContentResolver,
    contentUri: Uri,
    valueColumn: String,
    contactIdColumn: String,
    contactId: String
): String? {
    resolver.query(
        contentUri,
        arrayOf(valueColumn),
        "$contactIdColumn = ?",
        arrayOf(contactId),
        null
    )?.use { cursor ->
        if (cursor.moveToFirst()) {
            return cursor.getString(cursor.getColumnIndexOrThrow(valueColumn))
        }
    }
    return null
}
