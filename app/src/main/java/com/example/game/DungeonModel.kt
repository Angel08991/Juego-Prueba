package com.example.game

enum class WallType(val id: Int) {
  EMPTY(0),
  STONE_WALL(1),
  MOSSY_WALL(2),
  TORCH_WALL(3),
  RUNE_WALL(4),
  DOOR_LOCKED(5),
  DOOR_OPEN(6),
  EXIT_PORTAL(7);

  companion object {
    fun fromId(id: Int): WallType = entries.find { it.id == id } ?: EMPTY
  }
}

enum class EnemyType(
  val displayName: String,
  val maxHp: Int,
  val damage: Int,
  val speed: Float,
  val xpReward: Int,
  val goldReward: Int,
  val attackRange: Float = 1.0f
) {
  SKELETON("Esqueleto Guerrero", 45, 8, 0.032f, 30, 15),
  GOBLIN("Goblin Pícaro", 30, 6, 0.042f, 20, 10),
  MINOTAUR_BOSS("Señor del Abismo", 160, 16, 0.024f, 150, 90, 1.2f)
}

enum class EnemyState {
  IDLE, CHASE, ATTACK, DEAD
}

data class Enemy(
  val id: Int,
  val type: EnemyType,
  var x: Float,
  var y: Float,
  var hp: Int,
  val maxHp: Int = type.maxHp,
  var state: EnemyState = EnemyState.IDLE,
  var attackCooldown: Int = 0,
  var deathTimer: Float = 1.0f // 1.0 down to 0.0 when dying
) {
  val isAlive: Boolean get() = hp > 0
}

enum class ItemType(val displayName: String) {
  HEALTH_POTION("Poción de Salud"),
  MANA_POTION("Poción de Maná"),
  GOLD_CHEST("Cofre del Tesoro"),
  DUNGEON_KEY("Llave Antigua"),
  SWORD_UPGRADE("Espada Encantada")
}

data class DungeonItem(
  val id: Int,
  val type: ItemType,
  val x: Float,
  val y: Float,
  var isCollected: Boolean = false,
  val value: Int = 0
)

data class Projectile(
  val id: Int,
  var x: Float,
  var y: Float,
  val dx: Float,
  val dy: Float,
  val damage: Int = 30,
  var isAlive: Boolean = true,
  var distanceTraveled: Float = 0f
)

data class CombatText(
  val id: Long,
  val text: String,
  val worldX: Float,
  val worldY: Float,
  var alpha: Float = 1.0f,
  var offsetY: Float = 0.0f,
  val colorLong: Long
)

data class PlayerStats(
  var level: Int = 1,
  var hp: Int = 100,
  var maxHp: Int = 100,
  var mp: Int = 50,
  var maxMp: Int = 50,
  var xp: Int = 0,
  var xpNext: Int = 60,
  var gold: Int = 0,
  var keys: Int = 0,
  var attackPower: Int = 15,
  var defense: Int = 3,
  var healthPotions: Int = 2,
  var manaPotions: Int = 1,
  var weaponTier: Int = 1, // 1: Hierro, 2: Acero Valyrio, 3: Filo de Fuego
  var weaponName: String = "Espada de Hierro"
)
