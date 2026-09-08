package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.DungeonDatabase
import com.example.data.DungeonRecord
import com.example.game.AudioSynth
import com.example.game.CombatText
import com.example.game.DungeonItem
import com.example.game.DungeonLevelData
import com.example.game.DungeonLevels
import com.example.game.Enemy
import com.example.game.EnemyState
import com.example.game.EnemyType
import com.example.game.ItemType
import com.example.game.PlayerStats
import com.example.game.Projectile
import com.example.game.WallType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.random.Random

enum class GameState {
  RUNNING, PAUSED, LEVEL_COMPLETED, GAME_OVER, VICTORY
}

data class DungeonUiState(
  val currentFloor: Int = 1,
  val levelTitle: String = "",
  val levelSubtitle: String = "",
  val mapWidth: Int = 16,
  val mapHeight: Int = 16,
  val playerX: Float = 2.5f,
  val playerY: Float = 3.5f,
  val playerAngle: Float = 0f,
  val headBob: Float = 0f,
  val playerStats: PlayerStats = PlayerStats(),
  val enemies: List<Enemy> = emptyList(),
  val items: List<DungeonItem> = emptyList(),
  val projectiles: List<Projectile> = emptyList(),
  val combatTexts: List<CombatText> = emptyList(),
  val attackProgress: Float = 0f,
  val isMagicCast: Boolean = false,
  val isShieldActive: Boolean = false,
  val hurtVignetteAlpha: Float = 0f,
  val torchFlicker: Float = 1.0f,
  val gameState: GameState = GameState.RUNNING,
  val interactPrompt: String? = null,
  val totalEnemiesKilled: Int = 0,
  val totalGoldEarned: Int = 0,
  val isSoundMuted: Boolean = false
)

class DungeonViewModel(application: Application) : AndroidViewModel(application) {

  private val db = DungeonDatabase.getDatabase(application)
  val highScores: StateFlow<List<DungeonRecord>> = db.dungeonDao()
    .getHighScores()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _uiState = MutableStateFlow(DungeonUiState())
  val uiState: StateFlow<DungeonUiState> = _uiState.asStateFlow()

  // Mutable game state internals
  private var currentLevel = 1
  private lateinit var currentLevelData: DungeonLevelData
  private lateinit var currentMap: Array<IntArray>
  private val exploredGrid = BooleanArray(32 * 32)

  private var playerX = 2.5f
  private var playerY = 3.5f
  private var playerAngle = 0f
  private var headBob = 0f
  private var bobStep = 0f
  private var hurtAlpha = 0f
  private var attackAnim = 0f
  private var magicAnim = 0f
  private var isShield = false

  private var enemiesList = mutableListOf<Enemy>()
  private var itemsList = mutableListOf<DungeonItem>()
  private var projectilesList = mutableListOf<Projectile>()
  private var combatTextList = mutableListOf<CombatText>()

  private var playerStats = PlayerStats()
  private var currentGameState = GameState.RUNNING

  private var moveForwardInput = 0f
  private var moveStrafeInput = 0f
  private var turnInput = 0f

  private var gameLoopJob: Job? = null
  private var gameTimeSec = 0f
  private var totalKills = 0
  private var totalGold = 0
  private var nextId = 1000

  init {
    loadLevel(1, initialStats = PlayerStats())
    startGameLoop()
  }

  fun getExploredTile(x: Int, y: Int): Boolean {
    val idx = y * currentLevelData.width + x
    return idx in exploredGrid.indices && exploredGrid[idx]
  }

  fun getMapTile(x: Int, y: Int): Int {
    if (y in 0 until currentLevelData.height && x in 0 until currentLevelData.width) {
      return currentMap[y][x]
    }
    return 1
  }

  fun getMapArray(): Array<IntArray> = currentMap

  fun loadLevel(floor: Int, initialStats: PlayerStats) {
    currentLevel = floor
    currentLevelData = DungeonLevels.getLevel(floor)
    currentMap = Array(currentLevelData.height) { y ->
      currentLevelData.map[y].clone()
    }

    // Reset exploration for this floor
    exploredGrid.fill(false)

    playerX = currentLevelData.playerStartX
    playerY = currentLevelData.playerStartY
    playerAngle = currentLevelData.playerStartAngle
    playerStats = initialStats.copy()

    enemiesList = currentLevelData.initialEnemies.map { it.copy() }.toMutableList()
    itemsList = currentLevelData.initialItems.map { it.copy() }.toMutableList()
    projectilesList.clear()
    combatTextList.clear()

    currentGameState = GameState.RUNNING
    updateExploration(playerX.toInt(), playerY.toInt())

    syncUiState()
  }

  private fun startGameLoop() {
    gameLoopJob?.cancel()
    gameLoopJob = viewModelScope.launch {
      var lastTime = System.nanoTime()
      while (isActive) {
        val now = System.nanoTime()
        val deltaSec = ((now - lastTime) / 1_000_000_000.0f).coerceIn(0.005f, 0.05f)
        lastTime = now

        if (currentGameState == GameState.RUNNING) {
          updateGame(deltaSec)
        }
        delay(16) // ~60fps
      }
    }
  }

  private fun updateGame(delta: Float) {
    gameTimeSec += delta

    // 1. Player Turning
    if (turnInput != 0f) {
      playerAngle += turnInput * delta
      // Normalize angle to [0, 2pi)
      while (playerAngle < 0f) playerAngle += (Math.PI * 2).toFloat()
      while (playerAngle >= Math.PI * 2) playerAngle -= (Math.PI * 2).toFloat()
    }

    // 2. Player Movement with Wall Collision
    val isMoving = moveForwardInput != 0f || moveStrafeInput != 0f
    if (isMoving) {
      val moveSpeed = 3.2f * delta * (if (isShield) 0.5f else 1.0f)
      val forwardX = cos(playerAngle) * moveForwardInput * moveSpeed
      val forwardY = sin(playerAngle) * moveForwardInput * moveSpeed

      val strafeAngle = playerAngle + (Math.PI / 2).toFloat()
      val strafeX = cos(strafeAngle) * moveStrafeInput * moveSpeed
      val strafeY = sin(strafeAngle) * moveStrafeInput * moveSpeed

      val targetX = playerX + forwardX + strafeX
      val targetY = playerY + forwardY + strafeY

      // Slide collision check with padding
      val padding = 0.28f
      if (!isWallCollision(targetX + padding, playerY) && !isWallCollision(targetX - padding, playerY)) {
        playerX = targetX
      }
      if (!isWallCollision(playerX, targetY + padding) && !isWallCollision(playerX, targetY - padding)) {
        playerY = targetY
      }

      // Head bobbing
      bobStep += delta * 10f
      headBob = sin(bobStep) * 12f
      if (sin(bobStep) > 0.95f && Random.nextFloat() < 0.25f) {
        AudioSynth.playStep()
      }

      updateExploration(playerX.toInt(), playerY.toInt())
    } else {
      headBob *= 0.85f
    }

    // 3. Torch Flicker
    val torchFlicker = (1.0f + 0.08f * sin(gameTimeSec * 7f) + 0.05f * sin(gameTimeSec * 16f)).coerceIn(0.85f, 1.25f)

    // 4. Attack Animation & Magic Animation Progress
    if (attackAnim > 0f) {
      attackAnim += delta * 4.2f
      if (attackAnim >= 1.0f) attackAnim = 0f
    }
    if (magicAnim > 0f) {
      magicAnim += delta * 3.5f
      if (magicAnim >= 1.0f) magicAnim = 0f
    }

    // Hurt Vignette Fade
    if (hurtAlpha > 0f) {
      hurtAlpha = (hurtAlpha - delta * 2.2f).coerceAtLeast(0f)
    }

    // 5. Update Projectiles
    val projIter = projectilesList.iterator()
    while (projIter.hasNext()) {
      val p = projIter.next()
      p.x += p.dx * delta * 7.5f
      p.y += p.dy * delta * 7.5f
      p.distanceTraveled += delta * 7.5f

      // Check wall hit
      if (isWallCollision(p.x, p.y) || p.distanceTraveled > 12f) {
        p.isAlive = false
        projIter.remove()
        continue
      }

      // Check enemy hit
      enemiesList.forEach { enemy ->
        if (enemy.isAlive) {
          val dist = hypot(enemy.x - p.x, enemy.y - p.y)
          if (dist < 0.65f) {
            enemy.hp -= p.damage
            p.isAlive = false
            AudioSynth.playHit()
            addCombatText("¡FUEGO! -${p.damage}", enemy.x, enemy.y, 0xFFFF9800)
            if (enemy.hp <= 0) {
              onEnemyDefeated(enemy)
            }
          }
        }
      }
      if (!p.isAlive) {
        projIter.remove()
      }
    }

    // 6. Update Enemies (AI & Pathing)
    enemiesList.forEach { enemy ->
      if (!enemy.isAlive) {
        if (enemy.deathTimer > 0f) {
          enemy.deathTimer = (enemy.deathTimer - delta * 1.5f).coerceAtLeast(0f)
        }
        return@forEach
      }

      val distToPlayer = hypot(playerX - enemy.x, playerY - enemy.y)

      // Agro check
      if (distToPlayer < 7.5f && hasLineOfSight(enemy.x, enemy.y, playerX, playerY)) {
        enemy.state = EnemyState.CHASE
      }

      if (enemy.state == EnemyState.CHASE) {
        if (distToPlayer > enemy.type.attackRange) {
          // Move towards player
          val dirX = (playerX - enemy.x) / distToPlayer
          val dirY = (playerY - enemy.y) / distToPlayer
          val nextX = enemy.x + dirX * enemy.type.speed * delta * 50f
          val nextY = enemy.y + dirY * enemy.type.speed * delta * 50f

          if (!isWallCollision(nextX, enemy.y)) enemy.x = nextX
          if (!isWallCollision(enemy.x, nextY)) enemy.y = nextY
        } else {
          // In attack range
          if (enemy.attackCooldown <= 0) {
            // Enemy attacks player!
            enemy.attackCooldown = (55 / (if (enemy.type == EnemyType.GOBLIN) 1.4f else 1.0f)).toInt()
            val rawDamage = enemy.type.damage
            val mitigated = if (isShield) (rawDamage * 0.25f).toInt().coerceAtLeast(1) else (rawDamage - playerStats.defense).coerceAtLeast(2)
            playerStats.hp = (playerStats.hp - mitigated).coerceAtLeast(0)
            hurtAlpha = 0.9f
            AudioSynth.playPlayerHurt()
            addCombatText("-$mitigated", playerX, playerY, 0xFFE53935)

            if (playerStats.hp <= 0) {
              handleGameOver()
            }
          }
        }
      }

      if (enemy.attackCooldown > 0) {
        enemy.attackCooldown--
      }
    }

    // 7. Update Combat Floating Texts
    val ctIter = combatTextList.iterator()
    while (ctIter.hasNext()) {
      val ct = ctIter.next()
      ct.offsetY += delta * 45f
      ct.alpha -= delta * 1.2f
      if (ct.alpha <= 0f) {
        ctIter.remove()
      }
    }

    // 8. Auto-pickup nearby keys or potions if walked directly over, otherwise prompt
    itemsList.forEach { itm ->
      if (!itm.isCollected) {
        val dist = hypot(playerX - itm.x, playerY - itm.y)
        if (dist < 0.75f) {
          collectItem(itm)
        }
      }
    }

    // 9. Interaction Proximity Prompt (Locked doors, Exit portals, Chests)
    checkNearbyInteractions()

    // 10. Mana Regeneration
    if (playerStats.mp < playerStats.maxMp && (gameTimeSec.toInt() % 3 == 0) && Random.nextFloat() < 0.05f) {
      playerStats.mp = (playerStats.mp + 1).coerceAtMost(playerStats.maxMp)
    }

    syncUiState()
  }

  private fun checkNearbyInteractions() {
    var prompt: String? = null

    // Check adjacent tiles for locked doors or exit portals
    val px = playerX.toInt()
    val py = playerY.toInt()
    val offsets = listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)

    for ((dx, dy) in offsets) {
      val checkX = px + dx
      val checkY = py + dy
      if (checkX in 0 until currentLevelData.width && checkY in 0 until currentLevelData.height) {
        val tile = currentMap[checkY][checkX]
        if (tile == WallType.DOOR_LOCKED.id) {
          prompt = if (playerStats.keys > 0) "¡Usar Llave para abrir Puerta!" else "Puerta Bloqueada (Necesitas Llave)"
          break
        } else if (tile == WallType.EXIT_PORTAL.id) {
          prompt = if (currentLevel == 3) {
            val boss = enemiesList.find { it.type == EnemyType.MINOTAUR_BOSS }
            if (boss?.isAlive == true) "¡Derrota al Señor del Abismo!" else "¡Tocar Altar de la Victoria!"
          } else {
            "¡Descender al Nivel ${currentLevel + 1}!"
          }
          break
        }
      }
    }

    _uiState.value = _uiState.value.copy(interactPrompt = prompt)
  }

  private fun updateExploration(px: Int, py: Int) {
    val radius = 3
    for (dy in -radius..radius) {
      for (dx in -radius..radius) {
        val x = px + dx
        val y = py + dy
        if (x in 0 until currentLevelData.width && y in 0 until currentLevelData.height) {
          exploredGrid[y * currentLevelData.width + x] = true
        }
      }
    }
  }

  private fun isWallCollision(x: Float, y: Float): Boolean {
    val mapX = x.toInt()
    val mapY = y.toInt()
    if (mapX !in 0 until currentLevelData.width || mapY !in 0 until currentLevelData.height) {
      return true
    }
    val tile = currentMap[mapY][mapX]
    return tile > 0 && tile != WallType.DOOR_OPEN.id && tile != WallType.EXIT_PORTAL.id
  }

  private fun hasLineOfSight(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
    val dist = hypot(x2 - x1, y2 - y1)
    val steps = (dist * 4).toInt().coerceAtLeast(1)
    for (i in 1 until steps) {
      val t = i.toFloat() / steps
      val checkX = x1 + (x2 - x1) * t
      val checkY = y1 + (y2 - y1) * t
      if (isWallCollision(checkX, checkY)) return false
    }
    return true
  }

  fun setJoystickInput(forward: Float, strafe: Float) {
    moveForwardInput = forward.coerceIn(-1f, 1f)
    moveStrafeInput = strafe.coerceIn(-1f, 1f)
  }

  fun setTurnInput(turn: Float) {
    turnInput = turn.coerceIn(-2.5f, 2.5f)
  }

  fun turnByDrag(deltaPixels: Float) {
    playerAngle += deltaPixels * 0.0055f
    while (playerAngle < 0f) playerAngle += (Math.PI * 2).toFloat()
    while (playerAngle >= Math.PI * 2) playerAngle -= (Math.PI * 2).toFloat()
    syncUiState()
  }

  fun performAttack() {
    if (attackAnim > 0f || currentGameState != GameState.RUNNING) return
    attackAnim = 0.01f
    AudioSynth.playSlash()

    // Melee attack check in front of player
    val attackRange = 1.55f
    var hitAny = false

    enemiesList.forEach { enemy ->
      if (enemy.isAlive) {
        val dx = enemy.x - playerX
        val dy = enemy.y - playerY
        val dist = hypot(dx, dy)
        if (dist <= attackRange) {
          // Angle check to ensure enemy is in front cone
          val angleToEnemy = atan2(dy, dx)
          var diff = angleToEnemy - playerAngle
          while (diff < -Math.PI) diff += (Math.PI * 2).toFloat()
          while (diff > Math.PI) diff -= (Math.PI * 2).toFloat()

          if (Math.abs(diff) < 0.85f) { // ~50 degree front arc
            hitAny = true
            val isCrit = Random.nextFloat() < 0.25f
            val baseDmg = playerStats.attackPower + (playerStats.weaponTier * 6)
            val finalDmg = if (isCrit) (baseDmg * 1.75f).toInt() else (baseDmg + Random.nextInt(-2, 4))
            enemy.hp -= finalDmg
            AudioSynth.playHit()

            val text = if (isCrit) "¡CRÍTICO! -$finalDmg" else "-$finalDmg"
            val color = if (isCrit) 0xFFFFD54F else 0xFFFF5252
            addCombatText(text, enemy.x, enemy.y, color)

            if (enemy.hp <= 0) {
              onEnemyDefeated(enemy)
            }
          }
        }
      }
    }
  }

  fun castFireball() {
    if (playerStats.mp < 15 || magicAnim > 0f || currentGameState != GameState.RUNNING) return
    playerStats.mp -= 15
    magicAnim = 0.01f
    AudioSynth.playFireball()

    val dirX = cos(playerAngle)
    val dirY = sin(playerAngle)
    projectilesList.add(
      Projectile(
        id = nextId++,
        x = playerX + dirX * 0.4f,
        y = playerY + dirY * 0.4f,
        dx = dirX,
        dy = dirY,
        damage = 35 + playerStats.level * 5
      )
    )
    syncUiState()
  }

  fun toggleShield(active: Boolean) {
    isShield = active
    _uiState.value = _uiState.value.copy(isShieldActive = isShield)
  }

  fun useHealthPotion() {
    if (playerStats.healthPotions > 0 && playerStats.hp < playerStats.maxHp) {
      playerStats.healthPotions--
      val heal = 45
      playerStats.hp = (playerStats.hp + heal).coerceAtMost(playerStats.maxHp)
      AudioSynth.playChest()
      addCombatText("+$heal HP", playerX, playerY, 0xFF4CAF50)
      syncUiState()
    }
  }

  fun useManaPotion() {
    if (playerStats.manaPotions > 0 && playerStats.mp < playerStats.maxMp) {
      playerStats.manaPotions--
      val mana = 35
      playerStats.mp = (playerStats.mp + mana).coerceAtMost(playerStats.maxMp)
      AudioSynth.playChest()
      addCombatText("+$mana MP", playerX, playerY, 0xFF2196F3)
      syncUiState()
    }
  }

  fun interact() {
    val px = playerX.toInt()
    val py = playerY.toInt()
    val offsets = listOf(0 to 0, -1 to 0, 1 to 0, 0 to -1, 0 to 1)

    for ((dx, dy) in offsets) {
      val cx = px + dx
      val cy = py + dy
      if (cx in 0 until currentLevelData.width && cy in 0 until currentLevelData.height) {
        val tile = currentMap[cy][cx]

        if (tile == WallType.DOOR_LOCKED.id) {
          if (playerStats.keys > 0) {
            playerStats.keys--
            currentMap[cy][cx] = WallType.DOOR_OPEN.id
            AudioSynth.playUnlock()
            addCombatText("¡Puerta Desbloqueada!", cx + 0.5f, cy + 0.5f, 0xFFFFD54F)
            syncUiState()
            return
          } else {
            addCombatText("¡Necesitas una Llave!", cx + 0.5f, cy + 0.5f, 0xFFE53935)
            return
          }
        } else if (tile == WallType.EXIT_PORTAL.id) {
          if (currentLevel == 3) {
            val boss = enemiesList.find { it.type == EnemyType.MINOTAUR_BOSS }
            if (boss?.isAlive == true) {
              addCombatText("¡El Señor del Abismo aún vive!", cx + 0.5f, cy + 0.5f, 0xFFE53935)
              return
            } else {
              handleVictory()
              return
            }
          } else {
            // Level completed!
            currentGameState = GameState.LEVEL_COMPLETED
            AudioSynth.playLevelUp()
            syncUiState()
            return
          }
        }
      }
    }

    // Check nearby chests or items
    itemsList.forEach { itm ->
      if (!itm.isCollected) {
        val dist = hypot(playerX - itm.x, playerY - itm.y)
        if (dist < 1.4f) {
          collectItem(itm)
          return
        }
      }
    }
  }

  private fun collectItem(item: DungeonItem) {
    item.isCollected = true
    when (item.type) {
      ItemType.HEALTH_POTION -> {
        playerStats.healthPotions++
        AudioSynth.playChest()
        addCombatText("+1 Poción de Salud", item.x, item.y, 0xFF4CAF50)
      }
      ItemType.MANA_POTION -> {
        playerStats.manaPotions++
        AudioSynth.playChest()
        addCombatText("+1 Poción de Maná", item.x, item.y, 0xFF2196F3)
      }
      ItemType.GOLD_CHEST -> {
        val goldAmt = item.value.coerceAtLeast(25)
        playerStats.gold += goldAmt
        totalGold += goldAmt
        AudioSynth.playChest()
        addCombatText("+$goldAmt Oro", item.x, item.y, 0xFFFFD54F)
      }
      ItemType.DUNGEON_KEY -> {
        playerStats.keys++
        AudioSynth.playUnlock()
        addCombatText("¡Llave Antigua Encontrada!", item.x, item.y, 0xFFFFB300)
      }
      ItemType.SWORD_UPGRADE -> {
        playerStats.weaponTier = (playerStats.weaponTier + 1).coerceAtMost(3)
        playerStats.weaponName = if (playerStats.weaponTier == 2) "Espada de Acero Rúnico" else "Filo de Fuego Arcano"
        playerStats.attackPower += 8
        AudioSynth.playLevelUp()
        addCombatText("¡Arma Mejorada: ${playerStats.weaponName}!", item.x, item.y, 0xFF00E5FF)
      }
    }
    syncUiState()
  }

  private fun onEnemyDefeated(enemy: Enemy) {
    totalKills++
    val xpEarned = enemy.type.xpReward
    val goldEarned = enemy.type.goldReward
    playerStats.gold += goldEarned
    totalGold += goldEarned
    playerStats.xp += xpEarned

    addCombatText("+$xpEarned XP", enemy.x, enemy.y, 0xFF81C784)

    // Check level up
    if (playerStats.xp >= playerStats.xpNext) {
      playerStats.level++
      playerStats.xp -= playerStats.xpNext
      playerStats.xpNext = (playerStats.xpNext * 1.5f).toInt()
      playerStats.maxHp += 20
      playerStats.hp = playerStats.maxHp
      playerStats.maxMp += 15
      playerStats.mp = playerStats.maxMp
      playerStats.attackPower += 3
      playerStats.defense += 1
      AudioSynth.playLevelUp()
      addCombatText("¡NIVEL ${playerStats.level} ALCANZADO!", playerX, playerY, 0xFFFFD54F)
    }

    // Boss victory check
    if (enemy.type == EnemyType.MINOTAUR_BOSS) {
      addCombatText("¡SEÑOR DEL ABISMO DERROTADO!", enemy.x, enemy.y, 0xFFFFD54F)
    }
  }

  private fun addCombatText(text: String, x: Float, y: Float, colorLong: Long) {
    combatTextList.add(
      CombatText(
        id = System.nanoTime(),
        text = text,
        worldX = x,
        worldY = y,
        colorLong = colorLong
      )
    )
  }

  private fun handleGameOver() {
    currentGameState = GameState.GAME_OVER
    saveGameRecord("Derrota")
  }

  private fun handleVictory() {
    currentGameState = GameState.VICTORY
    AudioSynth.playLevelUp()
    saveGameRecord("Victoria")
  }

  private fun saveGameRecord(outcome: String) {
    viewModelScope.launch {
      val score = totalKills * 100 + totalGold * 10 + currentLevel * 500
      db.dungeonDao().insertRecord(
        DungeonRecord(
          floorReached = currentLevel,
          enemiesDefeated = totalKills,
          goldCollected = totalGold,
          score = score,
          outcome = outcome
        )
      )
    }
  }

  fun nextFloor() {
    loadLevel(currentLevel + 1, initialStats = playerStats)
  }

  fun restartGame() {
    totalKills = 0
    totalGold = 0
    loadLevel(1, initialStats = PlayerStats())
  }

  fun togglePause() {
    if (currentGameState == GameState.RUNNING) {
      currentGameState = GameState.PAUSED
    } else if (currentGameState == GameState.PAUSED) {
      currentGameState = GameState.RUNNING
    }
    syncUiState()
  }

  fun toggleMute() {
    AudioSynth.isMuted = !AudioSynth.isMuted
    _uiState.value = _uiState.value.copy(isSoundMuted = AudioSynth.isMuted)
  }

  private fun syncUiState() {
    _uiState.value = DungeonUiState(
      currentFloor = currentLevel,
      levelTitle = currentLevelData.title,
      levelSubtitle = currentLevelData.subtitle,
      mapWidth = currentLevelData.width,
      mapHeight = currentLevelData.height,
      playerX = playerX,
      playerY = playerY,
      playerAngle = playerAngle,
      headBob = headBob,
      playerStats = playerStats,
      enemies = enemiesList.toList(),
      items = itemsList.toList(),
      projectiles = projectilesList.toList(),
      combatTexts = combatTextList.toList(),
      attackProgress = attackAnim,
      isMagicCast = magicAnim > 0f,
      isShieldActive = isShield,
      hurtVignetteAlpha = hurtAlpha,
      torchFlicker = (1.0f + 0.08f * sin(gameTimeSec * 7f)).coerceIn(0.85f, 1.25f),
      gameState = currentGameState,
      interactPrompt = _uiState.value.interactPrompt,
      totalEnemiesKilled = totalKills,
      totalGoldEarned = totalGold,
      isSoundMuted = AudioSynth.isMuted
    )
  }
}
