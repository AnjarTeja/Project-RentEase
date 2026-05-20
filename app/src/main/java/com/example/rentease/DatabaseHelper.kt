package com.example.rentease

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.security.MessageDigest

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        const val DATABASE_NAME = "rentease_db"
        const val DATABASE_VERSION = 1
        const val TABLE_USERS = "users"
        const val COL_ID = "id"
        const val COL_NAME = "name"
        const val COL_EMAIL = "email"
        const val COL_PASSWORD = "password"
        const val COL_PHONE = "phone"
        const val COL_CREATED_AT = "created_at"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createTableQuery = """
            CREATE TABLE $TABLE_USERS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_NAME TEXT NOT NULL,
                $COL_EMAIL TEXT UNIQUE NOT NULL,
                $COL_PASSWORD TEXT NOT NULL,
                $COL_PHONE TEXT,
                $COL_CREATED_AT DATETIME DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()

        db.execSQL(createTableQuery)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        onCreate(db)
    }

    // Hash password using SHA-256
    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val messageDigest = md.digest(password.toByteArray())
        return messageDigest.joinToString("") { "%02x".format(it) }
    }

    // Register user
    fun registerUser(name: String, email: String, password: String, phone: String): Boolean {
        return try {
            val db = this.writableDatabase
            val contentValues = ContentValues().apply {
                put(COL_NAME, name)
                put(COL_EMAIL, email)
                put(COL_PASSWORD, hashPassword(password))
                put(COL_PHONE, phone)
            }
            val result = db.insert(TABLE_USERS, null, contentValues)
            db.close()
            result != -1L
        } catch (e: Exception) {
            false
        }
    }

    // Login user - verify credentials
    fun loginUser(email: String, password: String): Boolean {
        return try {
            val db = this.readableDatabase
            val query = "SELECT $COL_ID FROM $TABLE_USERS WHERE $COL_EMAIL = ? AND $COL_PASSWORD = ?"
            val cursor: Cursor = db.rawQuery(query, arrayOf(email, hashPassword(password)))
            val isValid = cursor.count > 0
            cursor.close()
            db.close()
            isValid
        } catch (e: Exception) {
            false
        }
    }

    // Get user data by email
    fun getUserByEmail(email: String): Map<String, String>? {
        return try {
            val db = this.readableDatabase
            val query = "SELECT * FROM $TABLE_USERS WHERE $COL_EMAIL = ?"
            val cursor: Cursor = db.rawQuery(query, arrayOf(email))

            if (cursor.count > 0) {
                cursor.moveToFirst()
                val userData = mapOf(
                    COL_ID to cursor.getString(cursor.getColumnIndexOrThrow(COL_ID)),
                    COL_NAME to cursor.getString(cursor.getColumnIndexOrThrow(COL_NAME)),
                    COL_EMAIL to cursor.getString(cursor.getColumnIndexOrThrow(COL_EMAIL)),
                    COL_PHONE to cursor.getString(cursor.getColumnIndexOrThrow(COL_PHONE))
                )
                cursor.close()
                db.close()
                userData
            } else {
                cursor.close()
                db.close()
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    // Check if email already exists
    fun emailExists(email: String): Boolean {
        return try {
            val db = this.readableDatabase
            val query = "SELECT $COL_ID FROM $TABLE_USERS WHERE $COL_EMAIL = ?"
            val cursor: Cursor = db.rawQuery(query, arrayOf(email))
            val exists = cursor.count > 0
            cursor.close()
            db.close()
            exists
        } catch (e: Exception) {
            false
        }
    }
}
