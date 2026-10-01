package com.itantra.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [MessageEntity::class], version = 1, exportSchema = false)
abstract class iTantraDatabase : RoomDatabase() {

    abstract fun messageDao(): MessageDao

    companion object {
        @Volatile
        private var INSTANCE: iTantraDatabase? = null

        fun getInstance(context: Context): iTantraDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    iTantraDatabase::class.java,
                    "itantra_offline_mesh.db"
                ).fallbackToDestructiveMigration()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
