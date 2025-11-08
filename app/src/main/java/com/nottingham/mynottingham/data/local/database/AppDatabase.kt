package com.nottingham.mynottingham.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.nottingham.mynottingham.data.local.database.dao.BookingDao
import com.nottingham.mynottingham.data.local.database.dao.ErrandDao
import com.nottingham.mynottingham.data.local.database.dao.UserDao
import com.nottingham.mynottingham.data.local.database.entities.BookingEntity
import com.nottingham.mynottingham.data.local.database.entities.ErrandEntity
import com.nottingham.mynottingham.data.local.database.entities.UserEntity
import com.nottingham.mynottingham.util.Constants

/**
 * Room database for My Nottingham app
 * Contains all entities and provides access to DAOs
 */
@Database(
    entities = [
        UserEntity::class,
        BookingEntity::class,
        ErrandEntity::class
    ],
    version = Constants.DATABASE_VERSION,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    // DAOs
    abstract fun userDao(): UserDao
    abstract fun bookingDao(): BookingDao
    abstract fun errandDao(): ErrandDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Get database instance (Singleton pattern)
         */
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    Constants.DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        /**
         * For testing purposes
         */
        fun destroyInstance() {
            INSTANCE = null
        }
    }
}
