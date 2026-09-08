package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [DungeonRecord::class], version = 1, exportSchema = false)
abstract class DungeonDatabase : RoomDatabase() {
  abstract fun dungeonDao(): DungeonDao

  companion object {
    @Volatile
    private var INSTANCE: DungeonDatabase? = null

    fun getDatabase(context: Context): DungeonDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          DungeonDatabase::class.java,
          "dungeon_master.db"
        ).fallbackToDestructiveMigration().build()
        INSTANCE = instance
        instance
      }
    }
  }
}
