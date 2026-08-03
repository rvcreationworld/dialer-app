package com.rajdialer.app.data.repository

import android.content.Context
import android.provider.ContactsContract
import com.rajdialer.app.model.Contact
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository responsible for reading the user's contacts strictly via Android ContactsContract.
 */
class ContactsRepository(private val context: Context) {


    companion object {
        private var cachedContacts: List<Contact>? = null
        private var isCacheDirty = true
        
        fun invalidateCache() { isCacheDirty = true }
    }

    suspend fun getContacts(forceRefresh: Boolean = false): List<Contact> = withContext(Dispatchers.IO) {
        if (!forceRefresh && !isCacheDirty && cachedContacts != null) {
            return@withContext cachedContacts!!
        }
        
        val contactsList = mutableListOf<Contact>()

        
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI,
            ContactsContract.CommonDataKinds.Phone.STARRED
        )
        
        val cursor = context.contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        
        cursor?.use {
            val idIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoUriIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            val starredIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.STARRED)
            
            val addedIds = mutableSetOf<String>()
            
            while (it.moveToNext()) {
                val id = it.getString(idIndex) ?: continue
                
                // Prevent duplicate entries for the exact same contact ID in this raw list
                if (addedIds.contains(id)) continue 
                addedIds.add(id)
                
                val name = it.getString(nameIndex) ?: "Unknown"
                val number = it.getString(numberIndex) ?: ""
                val photoUri = if (photoUriIndex != -1) it.getString(photoUriIndex) else null
                val isFavorite = if (starredIndex != -1) (it.getInt(starredIndex) > 0) else false
                
                contactsList.add(
                    Contact(
                        id = id,
                        name = name,
                        number = number,
                        photoUri = photoUri,
                        isFavorite = isFavorite
                    )
                )
            }
        }
        
        cachedContacts = contactsList
        isCacheDirty = false
        return@withContext contactsList
    }
}
