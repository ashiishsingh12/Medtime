package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        PatientProfileEntity::class,
        MedicineEntity::class,
        DoseLogEntity::class
    ],
    version = 3,
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

        fun getInstance(context: Context): MedTimeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedTimeDatabase::class.java,
                    "medtime.db"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
