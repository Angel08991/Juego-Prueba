package com.example.game

data class DungeonLevelData(
  val floor: Int,
  val title: String,
  val subtitle: String,
  val width: Int,
  val height: Int,
  val map: Array<IntArray>,
  val playerStartX: Float,
  val playerStartY: Float,
  val playerStartAngle: Float,
  val initialEnemies: List<Enemy>,
  val initialItems: List<DungeonItem>
)

object DungeonLevels {

  fun getLevel(floor: Int): DungeonLevelData {
    return when (floor) {
      1 -> createLevel1()
      2 -> createLevel2()
      3 -> createLevel3()
      else -> createEndlessLevel(floor)
    }
  }

  private fun createLevel1(): DungeonLevelData {
    // 16x16 Dungeon Map
    // 1: Stone, 2: Mossy, 3: Torch Wall, 5: Locked Door, 7: Exit Portal
    val raw = arrayOf(
      intArrayOf(1, 1, 1, 3, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1),
      intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
      intArrayOf(1, 0, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
      intArrayOf(3, 0, 0, 0, 0, 1, 0, 0, 1, 1, 5, 1, 1, 0, 0, 1),
      intArrayOf(1, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0, 3),
      intArrayOf(1, 1, 0, 1, 1, 1, 0, 0, 1, 0, 7, 0, 1, 0, 0, 1),
      intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 1, 0, 0, 1),
      intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1, 0, 0, 1),
      intArrayOf(3, 0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 1),
      intArrayOf(1, 0, 1, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 3),
      intArrayOf(1, 0, 1, 0, 0, 0, 1, 0, 2, 2, 2, 2, 2, 0, 0, 1),
      intArrayOf(1, 0, 3, 0, 0, 0, 3, 0, 2, 0, 0, 0, 2, 0, 0, 1),
      intArrayOf(1, 0, 1, 0, 0, 0, 1, 0, 2, 0, 0, 0, 2, 0, 0, 1),
      intArrayOf(1, 0, 1, 1, 0, 1, 1, 0, 2, 0, 0, 0, 2, 0, 0, 1),
      intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 2, 2, 0, 2, 2, 0, 0, 1),
      intArrayOf(1, 1, 3, 1, 1, 1, 1, 1, 1, 1, 3, 1, 1, 1, 1, 1)
    )

    val enemies = listOf(
      Enemy(1, EnemyType.SKELETON, 6.5f, 2.5f, EnemyType.SKELETON.maxHp),
      Enemy(2, EnemyType.GOBLIN, 13.5f, 3.5f, EnemyType.GOBLIN.maxHp),
      Enemy(3, EnemyType.SKELETON, 3.5f, 11.5f, EnemyType.SKELETON.maxHp),
      Enemy(4, EnemyType.GOBLIN, 10.5f, 12.5f, EnemyType.GOBLIN.maxHp),
      Enemy(5, EnemyType.SKELETON, 13.5f, 13.5f, EnemyType.SKELETON.maxHp)
    )

    val items = listOf(
      DungeonItem(1, ItemType.HEALTH_POTION, 2.5f, 2.5f, value = 35),
      DungeonItem(2, ItemType.GOLD_CHEST, 14.0f, 1.5f, value = 40),
      DungeonItem(3, ItemType.DUNGEON_KEY, 3.5f, 12.5f, value = 1), // Key guarded in crypt room
      DungeonItem(4, ItemType.MANA_POTION, 10.5f, 13.5f, value = 30),
      DungeonItem(5, ItemType.GOLD_CHEST, 13.5f, 9.5f, value = 30)
    )

    return DungeonLevelData(
      floor = 1,
      title = "Nivel 1: Catacumbas de Piedra",
      subtitle = "Encuentra la llave antigua para abrir la puerta de hierro hacia el portal de descenso.",
      width = 16,
      height = 16,
      map = raw,
      playerStartX = 2.5f,
      playerStartY = 3.5f,
      playerStartAngle = 0f,
      initialEnemies = enemies,
      initialItems = items
    )
  }

  private fun createLevel2(): DungeonLevelData {
    // Level 2: Crypt of Shadows
    val raw = arrayOf(
      intArrayOf(2, 2, 2, 3, 2, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2),
      intArrayOf(2, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 2),
      intArrayOf(2, 0, 4, 4, 0, 0, 2, 0, 4, 4, 4, 4, 0, 0, 0, 2),
      intArrayOf(3, 0, 4, 4, 0, 0, 5, 0, 4, 0, 0, 4, 0, 0, 0, 3),
      intArrayOf(2, 0, 0, 0, 0, 0, 2, 0, 4, 0, 7, 4, 0, 0, 0, 2),
      intArrayOf(2, 2, 3, 2, 0, 2, 2, 0, 4, 4, 4, 4, 0, 0, 0, 2),
      intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2),
      intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2),
      intArrayOf(3, 0, 2, 2, 0, 2, 2, 0, 0, 4, 4, 4, 4, 0, 0, 2),
      intArrayOf(2, 0, 2, 0, 0, 0, 2, 0, 0, 4, 0, 0, 4, 0, 0, 3),
      intArrayOf(2, 0, 2, 0, 0, 0, 2, 0, 0, 4, 0, 0, 4, 0, 0, 2),
      intArrayOf(2, 0, 3, 0, 0, 0, 3, 0, 0, 4, 0, 0, 4, 0, 0, 2),
      intArrayOf(2, 0, 2, 0, 0, 0, 2, 0, 0, 4, 4, 0, 4, 0, 0, 2),
      intArrayOf(2, 0, 2, 2, 0, 2, 2, 0, 0, 0, 0, 0, 0, 0, 0, 2),
      intArrayOf(2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2),
      intArrayOf(2, 2, 3, 2, 2, 2, 2, 2, 2, 2, 3, 2, 2, 2, 2, 2)
    )

    val enemies = listOf(
      Enemy(11, EnemyType.SKELETON, 4.5f, 2.0f, EnemyType.SKELETON.maxHp),
      Enemy(12, EnemyType.GOBLIN, 12.5f, 2.5f, EnemyType.GOBLIN.maxHp),
      Enemy(13, EnemyType.GOBLIN, 14.5f, 7.5f, EnemyType.GOBLIN.maxHp),
      Enemy(14, EnemyType.SKELETON, 4.0f, 11.0f, EnemyType.SKELETON.maxHp),
      Enemy(15, EnemyType.SKELETON, 11.0f, 11.5f, EnemyType.SKELETON.maxHp),
      Enemy(16, EnemyType.GOBLIN, 7.5f, 13.5f, EnemyType.GOBLIN.maxHp)
    )

    val items = listOf(
      DungeonItem(11, ItemType.SWORD_UPGRADE, 4.0f, 12.0f, value = 2), // Upgraded sword!
      DungeonItem(12, ItemType.DUNGEON_KEY, 11.0f, 11.0f, value = 1),
      DungeonItem(13, ItemType.HEALTH_POTION, 1.5f, 1.5f, value = 40),
      DungeonItem(14, ItemType.MANA_POTION, 14.0f, 1.5f, value = 35),
      DungeonItem(15, ItemType.GOLD_CHEST, 14.0f, 14.0f, value = 65)
    )

    return DungeonLevelData(
      floor = 2,
      title = "Nivel 2: Cripta del Nigromante",
      subtitle = "Peligrosos goblins y esqueletos acechan. Encuentra la Espada Rúnica y la llave del santuario.",
      width = 16,
      height = 16,
      map = raw,
      playerStartX = 1.5f,
      playerStartY = 14.5f,
      playerStartAngle = -Math.PI.toFloat() / 2f,
      initialEnemies = enemies,
      initialItems = items
    )
  }

  private fun createLevel3(): DungeonLevelData {
    // Boss Chamber Level 3
    val raw = arrayOf(
      intArrayOf(4, 4, 4, 3, 4, 4, 4, 4, 4, 4, 3, 4, 4, 4, 4, 4),
      intArrayOf(4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4),
      intArrayOf(4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4),
      intArrayOf(3, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 3),
      intArrayOf(4, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 4),
      intArrayOf(4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4),
      intArrayOf(4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4),
      intArrayOf(4, 0, 0, 0, 0, 0, 3, 0, 0, 3, 0, 0, 0, 0, 0, 4),
      intArrayOf(4, 0, 0, 0, 0, 0, 0, 7, 0, 0, 0, 0, 0, 0, 0, 4), // Victory altar
      intArrayOf(4, 0, 0, 0, 0, 0, 3, 0, 0, 3, 0, 0, 0, 0, 0, 4),
      intArrayOf(4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4),
      intArrayOf(4, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 4),
      intArrayOf(3, 0, 0, 4, 4, 0, 0, 0, 0, 0, 0, 4, 4, 0, 0, 3),
      intArrayOf(4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4),
      intArrayOf(4, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 4),
      intArrayOf(4, 4, 4, 3, 4, 4, 4, 4, 4, 4, 3, 4, 4, 4, 4, 4)
    )

    val enemies = listOf(
      Enemy(21, EnemyType.MINOTAUR_BOSS, 7.5f, 7.5f, EnemyType.MINOTAUR_BOSS.maxHp),
      Enemy(22, EnemyType.SKELETON, 3.5f, 3.5f, EnemyType.SKELETON.maxHp),
      Enemy(23, EnemyType.SKELETON, 11.5f, 3.5f, EnemyType.SKELETON.maxHp),
      Enemy(24, EnemyType.GOBLIN, 3.5f, 11.5f, EnemyType.GOBLIN.maxHp),
      Enemy(25, EnemyType.GOBLIN, 11.5f, 11.5f, EnemyType.GOBLIN.maxHp)
    )

    val items = listOf(
      DungeonItem(21, ItemType.HEALTH_POTION, 2.0f, 7.5f, value = 50),
      DungeonItem(22, ItemType.MANA_POTION, 13.0f, 7.5f, value = 50),
      DungeonItem(23, ItemType.GOLD_CHEST, 2.0f, 2.0f, value = 120),
      DungeonItem(24, ItemType.GOLD_CHEST, 13.0f, 13.0f, value = 120)
    )

    return DungeonLevelData(
      floor = 3,
      title = "Nivel 3: El Altar del Minotauro",
      subtitle = "¡La batalla final! Derrota al temible Señor del Abismo para conquistar la mazmorra.",
      width = 16,
      height = 16,
      map = raw,
      playerStartX = 7.5f,
      playerStartY = 13.5f,
      playerStartAngle = -Math.PI.toFloat() / 2f,
      initialEnemies = enemies,
      initialItems = items
    )
  }

  private fun createEndlessLevel(floor: Int): DungeonLevelData {
    // Endless procedural descent after level 3
    val raw = Array(16) { y ->
      IntArray(16) { x ->
        if (x == 0 || y == 0 || x == 15 || y == 15) {
          if ((x + y) % 4 == 0) 3 else 1
        } else if ((x == 4 || x == 11) && (y in 4..11) && y != 7) {
          1
        } else if (x == 8 && y == 8) {
          7 // Exit
        } else 0
      }
    }

    val enemies = (1..6).map { i ->
      val type = if (i % 3 == 0) EnemyType.MINOTAUR_BOSS else if (i % 2 == 0) EnemyType.GOBLIN else EnemyType.SKELETON
      Enemy(
        id = 100 * floor + i,
        type = type,
        x = (2 + (i * 2) % 12).toFloat(),
        y = (2 + (i * 3) % 12).toFloat(),
        hp = (type.maxHp * (1f + (floor - 3) * 0.2f)).toInt()
      )
    }

    val items = listOf(
      DungeonItem(200 * floor + 1, ItemType.HEALTH_POTION, 2.5f, 2.5f, value = 40),
      DungeonItem(200 * floor + 2, ItemType.MANA_POTION, 13.5f, 2.5f, value = 35),
      DungeonItem(200 * floor + 3, ItemType.GOLD_CHEST, 13.5f, 13.5f, value = 75 + floor * 15)
    )

    return DungeonLevelData(
      floor = floor,
      title = "Nivel $floor: Mazmorra Infinita",
      subtitle = "Desciende a las profundidades infinitas. Los monstruos se vuelven más fuertes.",
      width = 16,
      height = 16,
      map = raw,
      playerStartX = 2.0f,
      playerStartY = 13.0f,
      playerStartAngle = -Math.PI.toFloat() / 2f,
      initialEnemies = enemies,
      initialItems = items
    )
  }
}
