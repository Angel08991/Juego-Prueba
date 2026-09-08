package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DungeonDao {
  @Query("SELECT * FROM dungeon_records ORDER BY score DESC, floorReached DESC LIMIT 20")
  fun getHighScores(): Flow<List<DungeonRecord>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertRecord(record: DungeonRecord): Long

  @Query("DELETE FROM dungeon_records")
  suspend fun clearAll()
}
