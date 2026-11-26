package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserAccountEntity::class,
        FamilyMemberEntity::class,
        PatientProfileEntity::class,
        MedicineEntity::class,
        DoseLogEntity::class
    ],
    version = 8,
    exportSchema = false
)
abstract class MedTimeDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: MedTimeDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE patient_profile ADD COLUMN eveningHour INTEGER NOT NULL DEFAULT 18")
                db.execSQL("ALTER TABLE patient_profile ADD COLUMN eveningMinute INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medicines ADD COLUMN eveningEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE medicines ADD COLUMN eveningHour INTEGER NOT NULL DEFAULT 18")
                db.execSQL("ALTER TABLE medicines ADD COLUMN eveningMinute INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN morningMealRelation TEXT NOT NULL DEFAULT 'AFTER_MEAL'")
                db.execSQL("ALTER TABLE medicines ADD COLUMN afternoonMealRelation TEXT NOT NULL DEFAULT 'AFTER_MEAL'")
                db.execSQL("ALTER TABLE medicines ADD COLUMN eveningMealRelation TEXT NOT NULL DEFAULT 'AFTER_MEAL'")
                db.execSQL("ALTER TABLE medicines ADD COLUMN nightMealRelation TEXT NOT NULL DEFAULT 'AFTER_MEAL'")
                db.execSQL(
                    """
                    UPDATE medicines SET
                        morningMealRelation = mealRelation,
                        afternoonMealRelation = mealRelation,
                        eveningMealRelation = mealRelation,
                        nightMealRelation = mealRelation
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS family_members (
                        id TEXT NOT NULL PRIMARY KEY,
                        name TEXT NOT NULL,
                        relation TEXT NOT NULL,
                        sortOrder INTEGER NOT NULL DEFAULT 0,
                        isDefault INTEGER NOT NULL DEFAULT 0
                    )
                    """.trimIndent()
                )
                db.execSQL("INSERT OR IGNORE INTO family_members (id, name, relation, sortOrder, isDefault) VALUES ('self', 'Self', 'Self', 0, 1)")
                db.execSQL("INSERT OR IGNORE INTO family_members (id, name, relation, sortOrder, isDefault) VALUES ('mom', 'Mom', 'Parent', 1, 1)")
                db.execSQL("INSERT OR IGNORE INTO family_members (id, name, relation, sortOrder, isDefault) VALUES ('dad', 'Dad', 'Parent', 2, 1)")
                db.execSQL("ALTER TABLE medicines ADD COLUMN memberId TEXT NOT NULL DEFAULT 'self'")
                db.execSQL("ALTER TABLE dose_logs ADD COLUMN memberId TEXT NOT NULL DEFAULT 'self'")
                db.execSQL("ALTER TABLE dose_logs ADD COLUMN memberName TEXT NOT NULL DEFAULT 'Self'")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN startDateKey TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE medicines ADD COLUMN endDateKey TEXT DEFAULT NULL")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS user_accounts (
                        id TEXT NOT NULL PRIMARY KEY,
                        firstName TEXT NOT NULL,
                        lastName TEXT NOT NULL,
                        gender TEXT NOT NULL,
                        dob TEXT NOT NULL,
                        state TEXT NOT NULL,
                        city TEXT NOT NULL,
                        email TEXT NOT NULL,
                        phone TEXT NOT NULL,
                        passwordSalt TEXT NOT NULL,
                        passwordHash TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_accounts_email ON user_accounts(email)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_user_accounts_phone ON user_accounts(phone)")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_accounts ADD COLUMN role TEXT NOT NULL DEFAULT 'USER'")
                db.execSQL(
                    """
                    UPDATE user_accounts
                    SET role = 'ADMIN'
                    WHERE lower(email) IN ('ashiish.singh12@gamil.com', 'ashiish.singh12@gmail.com')
                       OR phone = '9149044036'
                    """.trimIndent()
                )
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE medicines ADD COLUMN recurrenceFrequency TEXT NOT NULL DEFAULT 'DAILY'")
                db.execSQL("ALTER TABLE medicines ADD COLUMN recurrenceInterval INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE medicines ADD COLUMN recurrenceDays TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE medicines ADD COLUMN recurrenceEndType TEXT NOT NULL DEFAULT 'NEVER'")
                db.execSQL("ALTER TABLE medicines ADD COLUMN recurrenceCount INTEGER DEFAULT NULL")
                db.execSQL(
                    """
                    UPDATE medicines
                    SET recurrenceEndType = CASE
                        WHEN endDateKey IS NOT NULL AND endDateKey != '' THEN 'ON_DATE'
                        ELSE 'NEVER'
                    END
                    """.trimIndent()
                )
            }
        }

        fun getInstance(context: Context): MedTimeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedTimeDatabase::class.java,
                    "medtime.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8
                    )
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
