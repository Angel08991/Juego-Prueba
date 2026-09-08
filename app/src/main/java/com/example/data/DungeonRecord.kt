package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dungeon_records")
data class DungeonRecord(
  @PrimaryKey(autoGenerate = true)
  val id: Int = 0,
  val timestamp: Long = System.currentTimeMillis(),
  val floorReached: Int,
  val enemiesDefeated: Int,
  val goldCollected: Int,
  val score: Int,
  val outcome: String
)
