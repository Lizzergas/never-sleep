package com.lizz.myapptemplate.notes.data

import com.lizz.myapptemplate.common.UserDataCleaner
import com.lizz.myapptemplate.database.NoteDao

/**
 * Notes are per-user but the Room cache is per-device: wipe it whenever the
 * session changes so one account's notes never leak into another's view.
 */
class NotesUserDataCleaner(
    private val noteDao: NoteDao,
) : UserDataCleaner {
    override suspend fun clearUserData() {
        noteDao.clear()
    }
}
