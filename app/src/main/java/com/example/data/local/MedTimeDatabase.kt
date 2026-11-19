package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PatientProfileEntity::class,
        MedicineEntity::class,
        DoseLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class MedTimeDatabase : RoomDatabase() {
    abstract fun medicationDao(): MedicationDao

    companion object {
        @Volatile
        private var INSTANCE: MedTimeDatabase? = null

        fun getInstance(context: Context): MedTimeDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    MedTimeDatabase::class.java,
                    "medtime.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
