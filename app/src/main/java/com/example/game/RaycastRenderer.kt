package com.example.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class RaycastRenderer {

  // Raycasting parameters
  private val fov = 1.15f // ~66 degrees in radians
  private val halfFov = fov / 2f
  private val maxDepth = 16f

  // Cached Z-buffer for sprite depth sorting
  private var zBuffer = FloatArray(160)

  fun render(
    drawScope: DrawScope,
    map: Array<IntArray>,
    mapWidth: Int,
    mapHeight: Int,
    playerX: Float,
    playerY: Float,
    playerAngle: Float,
    headBob: Float,
    attackProgress: Float, // 0.0 to 1.0
    isMagicCast: Boolean,
    isShieldActive: Boolean,
    hurtVignetteAlpha: Float,
    torchFlicker: Float,
    enemies: List<Enemy>,
    items: List<DungeonItem>,
    projectiles: List<Projectile>,
    combatTexts: List<CombatText>,
    weaponTier: Int
  ) {
    val width = drawScope.size.width
    val height = drawScope.size.height
    if (width <= 0 || height <= 0) return

    val numRays = (width / 5f).toInt().coerceIn(120, 240)
    if (zBuffer.size != numRays) {
      zBuffer = FloatArray(numRays)
    }

    val horizon = height * 0.5f + headBob

    // 1. Render Ceiling & Floor Gradients with Atmospheric Lighting
    renderCeilingAndFloor(drawScope, width, height, horizon, torchFlicker)

    // 2. Cast Rays & Render Walls
    val rayWidth = width / numRays.toFloat()

    for (i in 0 until numRays) {
      val rayAngle = playerAngle - halfFov + (i.toFloat() / numRays.toFloat()) * fov
      val rayDirX = cos(rayAngle)
      val rayDirY = sin(rayAngle)

      // DDA Algorithm
      var mapX = playerX.toInt()
      var mapY = playerY.toInt()

      val deltaDistX = if (rayDirX != 0f) Math.abs(1f / rayDirX) else 1e30f
      val deltaDistY = if (rayDirY != 0f) Math.abs(1f / rayDirY) else 1e30f

      var stepX: Int
      var sideDistX: Float
      if (rayDirX < 0) {
        stepX = -1
        sideDistX = (playerX - mapX) * deltaDistX
      } else {
        stepX = 1
        sideDistX = (mapX + 1.0f - playerX) * deltaDistX
      }

      var stepY: Int
      var sideDistY: Float
      if (rayDirY < 0) {
        stepY = -1
        sideDistY = (playerY - mapY) * deltaDistY
      } else {
        stepY = 1
        sideDistY = (mapY + 1.0f - playerY) * deltaDistY
      }

      var hit = false
      var side = 0 // 0 = X hit (vertical), 1 = Y hit (horizontal)
      var wallType = WallType.EMPTY
      var wallHitPos = 0f

      var iterations = 0
      while (!hit && iterations < 32) {
        iterations++
        if (sideDistX < sideDistY) {
          sideDistX += deltaDistX
          mapX += stepX
          side = 0
        } else {
          sideDistY += deltaDistY
          mapY += stepY
          side = 1
        }

        if (mapX in 0 until mapWidth && mapY in 0 until mapHeight) {
          val tile = map[mapY][mapX]
          if (tile > 0 && tile != WallType.DOOR_OPEN.id) {
            hit = true
            wallType = WallType.fromId(tile)
          }
        } else {
          hit = true
          wallType = WallType.STONE_WALL
        }
      }

      // Perpendicular wall distance to remove fisheye
      val perpWallDist = if (side == 0) {
        (mapX - playerX + (1 - stepX) / 2f) / rayDirX
      } else {
        (mapY - playerY + (1 - stepY) / 2f) / rayDirY
      }.coerceAtLeast(0.1f)

      // Calculate wall hit coordinate (0..1) for texture pattern
      if (side == 0) {
        wallHitPos = playerY + perpWallDist * rayDirY
      } else {
        wallHitPos = playerX + perpWallDist * rayDirX
      }
      wallHitPos -= wallHitPos.toInt().toFloat()

      zBuffer[i] = perpWallDist

      // Wall projection height
      val lineHeight = (height / (perpWallDist * cos(rayAngle - playerAngle))) * 0.92f
      val drawStart = (horizon - lineHeight * 0.5f).coerceAtLeast(0f)
      val drawEnd = (horizon + lineHeight * 0.5f).coerceAtMost(height)

      // Light attenuation
      val distanceFactor = (1.0f / (1.0f + perpWallDist * 0.22f + perpWallDist * perpWallDist * 0.035f)) * torchFlicker
      val brightness = (distanceFactor * if (side == 1) 0.72f else 1.0f).coerceIn(0.04f, 1.0f)

      drawWallColumn(
        drawScope = drawScope,
        x = i * rayWidth,
        width = rayWidth + 0.6f, // prevent gaps
        top = drawStart,
        bottom = drawEnd,
        wallType = wallType,
        brightness = brightness,
        wallHitPos = wallHitPos,
        lineHeight = lineHeight,
        side = side
      )
    }

    // 3. Render 3D Sprites (Items, Projectiles, Enemies) sorted from back to front
    renderSprites(
      drawScope = drawScope,
      width = width,
      height = height,
      horizon = horizon,
      playerX = playerX,
      playerY = playerY,
      playerAngle = playerAngle,
      numRays = numRays,
      rayWidth = rayWidth,
      enemies = enemies,
      items = items,
      projectiles = projectiles,
      torchFlicker = torchFlicker
    )

    // 4. Render Floating 3D Combat Text (Damage numbers, buffs)
    renderCombatTexts(
      drawScope = drawScope,
      width = width,
      height = height,
      horizon = horizon,
      playerX = playerX,
      playerY = playerY,
      playerAngle = playerAngle,
      combatTexts = combatTexts
    )

    // 5. First-Person Hands & Weapons / Shield overlay
    renderFirstPersonWeapon(
      drawScope = drawScope,
      width = width,
      height = height,
      attackProgress = attackProgress,
      isMagicCast = isMagicCast,
      isShieldActive = isShieldActive,
      weaponTier = weaponTier,
      headBob = headBob
    )

    // 6. Hurt Vignette Effect
    if (hurtVignetteAlpha > 0.01f) {
      drawScope.drawRect(
        brush = Brush.radialGradient(
          colors = listOf(Color.Transparent, Color(0xFFE53935).copy(alpha = hurtVignetteAlpha * 0.75f)),
          center = Offset(width / 2f, height / 2f),
          radius = width * 0.65f
        ),
        size = Size(width, height)
      )
    }
  }

  private fun renderCeilingAndFloor(
    drawScope: DrawScope,
    width: Float,
    height: Float,
    horizon: Float,
    torchFlicker: Float
  ) {
    // Ceiling (Dark dungeon cavern vault)
    drawScope.drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF040608), Color(0xFF10141D)),
        startY = 0f,
        endY = horizon
      ),
      topLeft = Offset(0f, 0f),
      size = Size(width, horizon)
    )

    // Floor (Stone dungeon flagstone with warm torch reflection)
    val torchGlowAlpha = (0.22f * torchFlicker).coerceIn(0.1f, 0.4f)
    drawScope.drawRect(
      brush = Brush.verticalGradient(
        colors = listOf(Color(0xFF0E1118), Color(0xFF1A1C24)),
        startY = horizon,
        endY = height
      ),
      topLeft = Offset(0f, horizon),
      size = Size(width, height - horizon)
    )

    // Torch radius pool on the ground near the player
    drawScope.drawOval(
      brush = Brush.radialGradient(
        colors = listOf(
          Color(0xFFFF9800).copy(alpha = torchGlowAlpha),
          Color(0xFF8D4004).copy(alpha = torchGlowAlpha * 0.5f),
          Color.Transparent
        ),
        center = Offset(width * 0.5f, height * 0.85f),
        radius = width * 0.55f
      ),
      topLeft = Offset(width * 0.1f, height * 0.55f),
      size = Size(width * 0.8f, height * 0.6f)
    )
  }

  private fun drawWallColumn(
    drawScope: DrawScope,
    x: Float,
    width: Float,
    top: Float,
    bottom: Float,
    wallType: WallType,
    brightness: Float,
    wallHitPos: Float,
    lineHeight: Float,
    side: Int
  ) {
    val h = bottom - top
    if (h <= 0) return

    val baseColor = when (wallType) {
      WallType.STONE_WALL -> {
        val r = (0x3A * brightness).toInt().coerceIn(10, 255)
        val g = (0x40 * brightness).toInt().coerceIn(12, 255)
        val b = (0x4D * brightness).toInt().coerceIn(15, 255)
        Color(r, g, b)
      }
      WallType.MOSSY_WALL -> {
        val r = (0x2A * brightness).toInt().coerceIn(8, 255)
        val g = (0x4D * brightness).toInt().coerceIn(18, 255)
        val b = (0x35 * brightness).toInt().coerceIn(10, 255)
        Color(r, g, b)
      }
      WallType.TORCH_WALL -> {
        // Wall lit with amber glow
        val r = (0x6E * brightness).toInt().coerceIn(20, 255)
        val g = (0x4A * brightness).toInt().coerceIn(14, 255)
        val b = (0x30 * brightness).toInt().coerceIn(10, 255)
        Color(r, g, b)
      }
      WallType.RUNE_WALL -> {
        val r = (0x24 * brightness).toInt().coerceIn(10, 255)
        val g = (0x32 * brightness).toInt().coerceIn(15, 255)
        val b = (0x64 * brightness).toInt().coerceIn(30, 255)
        Color(r, g, b)
      }
      WallType.DOOR_LOCKED -> {
        // Heavy iron door
        val r = (0x55 * brightness).toInt().coerceIn(15, 255)
        val g = (0x45 * brightness).toInt().coerceIn(12, 255)
        val b = (0x35 * brightness).toInt().coerceIn(10, 255)
        Color(r, g, b)
      }
      WallType.EXIT_PORTAL -> {
        // Mystical staircase arch
        val r = (0x6A * brightness).toInt().coerceIn(20, 255)
        val g = (0x2D * brightness).toInt().coerceIn(10, 255)
        val b = (0x7D * brightness).toInt().coerceIn(30, 255)
        Color(r, g, b)
      }
      else -> Color.DarkGray
    }

    // Brick mortar texture lines check
    val isVerticalMortar = (wallHitPos < 0.05f || (wallHitPos > 0.48f && wallHitPos < 0.53f))
    val finalColor = if (isVerticalMortar) {
      baseColor.copy(
        red = (baseColor.red * 0.6f).coerceIn(0f, 1f),
        green = (baseColor.green * 0.6f).coerceIn(0f, 1f),
        blue = (baseColor.blue * 0.6f).coerceIn(0f, 1f)
      )
    } else {
      baseColor
    }

    drawScope.drawRect(
      color = finalColor,
      topLeft = Offset(x, top),
      size = Size(width, h)
    )

    // Wall features overlay (Torch sconce, door lock, or rune glow)
    if (wallType == WallType.TORCH_WALL && wallHitPos in 0.42f..0.58f) {
      val torchY = top + h * 0.38f
      val torchH = h * 0.22f
      val flameColor = Color(0xFFFFB74D).copy(alpha = brightness.coerceIn(0.3f, 1.0f))
      drawScope.drawRect(
        color = flameColor,
        topLeft = Offset(x, torchY),
        size = Size(width, torchH)
      )
    } else if (wallType == WallType.RUNE_WALL && wallHitPos in 0.35f..0.65f) {
      val runeY = top + h * 0.42f
      val runeH = h * 0.26f
      val runeGlow = Color(0xFF64B5F6).copy(alpha = (0.7f * brightness).coerceIn(0.2f, 0.9f))
      drawScope.drawRect(
        color = runeGlow,
        topLeft = Offset(x, runeY),
        size = Size(width, runeH)
      )
    } else if (wallType == WallType.DOOR_LOCKED) {
      // Golden keyhole & iron bands
      if (wallHitPos in 0.45f..0.55f) {
        val lockY = top + h * 0.5f
        val lockH = h * 0.14f
        drawScope.drawRect(
          color = Color(0xFFFFD54F).copy(alpha = brightness),
          topLeft = Offset(x, lockY),
          size = Size(width, lockH)
        )
      }
      // Horizontal iron reinforcement bands
      val band1Y = top + h * 0.22f
      val band2Y = top + h * 0.78f
      val bandH = h * 0.05f
      drawScope.drawRect(
        color = Color(0xFF2B2B33).copy(alpha = brightness),
        topLeft = Offset(x, band1Y),
        size = Size(width, bandH)
      )
      drawScope.drawRect(
        color = Color(0xFF2B2B33).copy(alpha = brightness),
        topLeft = Offset(x, band2Y),
        size = Size(width, bandH)
      )
    } else if (wallType == WallType.EXIT_PORTAL) {
      // Golden mystical portal glow
      val portalGlow = Color(0xFFFFD54F).copy(alpha = (0.8f * brightness).coerceIn(0.2f, 0.95f))
      drawScope.drawRect(
        color = portalGlow,
        topLeft = Offset(x, top + h * 0.2f),
        size = Size(width, h * 0.6f)
      )
    }
  }

  private fun renderSprites(
    drawScope: DrawScope,
    width: Float,
    height: Float,
    horizon: Float,
    playerX: Float,
    playerY: Float,
    playerAngle: Float,
    numRays: Int,
    rayWidth: Float,
    enemies: List<Enemy>,
    items: List<DungeonItem>,
    projectiles: List<Projectile>,
    torchFlicker: Float
  ) {
    data class SpriteDrawable(
      val distSq: Float,
      val x: Float,
      val y: Float,
      val enemy: Enemy? = null,
      val item: DungeonItem? = null,
      val projectile: Projectile? = null
    )

    val sprites = mutableListOf<SpriteDrawable>()

    enemies.forEach { e ->
      if (e.deathTimer > 0f) {
        val dSq = (playerX - e.x) * (playerX - e.x) + (playerY - e.y) * (playerY - e.y)
        sprites.add(SpriteDrawable(dSq, e.x, e.y, enemy = e))
      }
    }

    items.forEach { itm ->
      if (!itm.isCollected) {
        val dSq = (playerX - itm.x) * (playerX - itm.x) + (playerY - itm.y) * (playerY - itm.y)
        sprites.add(SpriteDrawable(dSq, itm.x, itm.y, item = itm))
      }
    }

    projectiles.forEach { p ->
      if (p.isAlive) {
        val dSq = (playerX - p.x) * (playerX - p.x) + (playerY - p.y) * (playerY - p.y)
        sprites.add(SpriteDrawable(dSq, p.x, p.y, projectile = p))
      }
    }

    // Sort far to near for painters algorithm
    sprites.sortByDescending { it.distSq }

    sprites.forEach { sprite ->
      val dx = sprite.x - playerX
      val dy = sprite.y - playerY

      // Transform with inverse camera matrix
      val cosA = cos(-playerAngle)
      val sinA = sin(-playerAngle)
      val transX = dx * cosA - dy * sinA // distance forward
      val transY = dx * sinA + dy * cosA // lateral distance

      if (transX > 0.25f) {
        val screenX = (width / 2f) * (1f + transY / (transX * kotlin.math.tan(halfFov)))
        val spriteSize = (height / transX) * 0.85f

        // Check if visible in screen bounds and not behind a closer wall
        val rayIdx = (screenX / rayWidth).toInt().coerceIn(0, numRays - 1)
        if (transX < zBuffer[rayIdx] + 0.35f && screenX in -spriteSize..(width + spriteSize)) {
          val spriteTop = horizon - spriteSize * 0.5f
          val spriteAlpha = (1.0f / (1.0f + transX * 0.25f) * torchFlicker).coerceIn(0.15f, 1.0f)

          when {
            sprite.enemy != null -> {
              drawEnemySprite(
                drawScope = drawScope,
                enemy = sprite.enemy,
                centerX = screenX,
                centerY = horizon,
                size = spriteSize,
                alpha = spriteAlpha * sprite.enemy.deathTimer
              )
            }
            sprite.item != null -> {
              drawItemSprite(
                drawScope = drawScope,
                item = sprite.item,
                centerX = screenX,
                centerY = horizon + spriteSize * 0.25f,
                size = spriteSize * 0.55f,
                alpha = spriteAlpha
              )
            }
            sprite.projectile != null -> {
              drawProjectileSprite(
                drawScope = drawScope,
                projectile = sprite.projectile,
                centerX = screenX,
                centerY = horizon,
                size = spriteSize * 0.4f
              )
            }
          }
        }
      }
    }
  }

  private fun drawEnemySprite(
    drawScope: DrawScope,
    enemy: Enemy,
    centerX: Float,
    centerY: Float,
    size: Float,
    alpha: Float
  ) {
    val top = centerY - size * 0.45f
    val w = size * 0.5f
    val left = centerX - w * 0.5f

    when (enemy.type) {
      EnemyType.SKELETON -> {
        // Skeleton Bones & Armor
        val boneColor = Color(0xFFEDE7D9).copy(alpha = alpha)
        val eyeColor = Color(0xFFFF1744).copy(alpha = alpha)
        val armorColor = Color(0xFF424242).copy(alpha = alpha)

        // Head / Skull
        drawScope.drawOval(
          color = boneColor,
          topLeft = Offset(left + w * 0.28f, top),
          size = Size(w * 0.44f, size * 0.28f)
        )
        // Glowing Red Sockets
        drawScope.drawCircle(
          color = eyeColor,
          radius = size * 0.025f,
          center = Offset(left + w * 0.4f, top + size * 0.12f)
        )
        drawScope.drawCircle(
          color = eyeColor,
          radius = size * 0.025f,
          center = Offset(left + w * 0.6f, top + size * 0.12f)
        )
        // Ribcage & Body
        drawScope.drawRect(
          color = armorColor,
          topLeft = Offset(left + w * 0.25f, top + size * 0.26f),
          size = Size(w * 0.5f, size * 0.35f)
        )
        // Rib lines
        for (i in 1..3) {
          drawScope.drawLine(
            color = boneColor,
            start = Offset(left + w * 0.28f, top + size * (0.28f + i * 0.07f)),
            end = Offset(left + w * 0.72f, top + size * (0.28f + i * 0.07f)),
            strokeWidth = size * 0.02f
          )
        }
        // Sword in hand
        val swordColor = Color(0xFFCFD8DC).copy(alpha = alpha)
        drawScope.drawLine(
          color = swordColor,
          start = Offset(left + w * 0.85f, top + size * 0.15f),
          end = Offset(left + w * 0.95f, top + size * 0.65f),
          strokeWidth = size * 0.035f
        )
      }

      EnemyType.GOBLIN -> {
        // Goblin: Green skin, pointed ears, leather vest, twin daggers
        val skinColor = Color(0xFF66BB6A).copy(alpha = alpha)
        val leatherColor = Color(0xFF6D4C41).copy(alpha = alpha)
        val eyeColor = Color(0xFFFFEE58).copy(alpha = alpha)

        // Pointed Goblin Ears
        val leftEar = Path().apply {
          moveTo(left + w * 0.25f, top + size * 0.12f)
          lineTo(left + w * 0.05f, top + size * 0.04f)
          lineTo(left + w * 0.28f, top + size * 0.22f)
          close()
        }
        val rightEar = Path().apply {
          moveTo(left + w * 0.75f, top + size * 0.12f)
          lineTo(left + w * 0.95f, top + size * 0.04f)
          lineTo(left + w * 0.72f, top + size * 0.22f)
          close()
        }
        drawScope.drawPath(leftEar, skinColor)
        drawScope.drawPath(rightEar, skinColor)

        // Head
        drawScope.drawOval(
          color = skinColor,
          topLeft = Offset(left + w * 0.25f, top + size * 0.05f),
          size = Size(w * 0.5f, size * 0.26f)
        )
        // Fierce yellow eyes
        drawScope.drawCircle(
          color = eyeColor,
          radius = size * 0.028f,
          center = Offset(left + w * 0.38f, top + size * 0.15f)
        )
        drawScope.drawCircle(
          color = eyeColor,
          radius = size * 0.028f,
          center = Offset(left + w * 0.62f, top + size * 0.15f)
        )
        // Body with leather vest
        drawScope.drawRect(
          color = leatherColor,
          topLeft = Offset(left + w * 0.22f, top + size * 0.28f),
          size = Size(w * 0.56f, size * 0.32f)
        )
      }

      EnemyType.MINOTAUR_BOSS -> {
        // Giant Minotaur Boss: Horns, dark fur, glowing molten eyes, battle hammer
        val bossColor = Color(0xFF3E2723).copy(alpha = alpha)
        val hornColor = Color(0xFFD7CCC8).copy(alpha = alpha)
        val moltenEye = Color(0xFFFF5722).copy(alpha = alpha)

        // Massive Bull Horns
        val leftHorn = Path().apply {
          moveTo(left + w * 0.25f, top + size * 0.1f)
          lineTo(left - w * 0.1f, top - size * 0.08f)
          lineTo(left + w * 0.2f, top + size * 0.2f)
          close()
        }
        val rightHorn = Path().apply {
          moveTo(left + w * 0.75f, top + size * 0.1f)
          lineTo(left + w * 1.1f, top - size * 0.08f)
          lineTo(left + w * 0.8f, top + size * 0.2f)
          close()
        }
        drawScope.drawPath(leftHorn, hornColor)
        drawScope.drawPath(rightHorn, hornColor)

        // Head
        drawScope.drawOval(
          color = bossColor,
          topLeft = Offset(left + w * 0.18f, top),
          size = Size(w * 0.64f, size * 0.32f)
        )
        // Molten eyes
        drawScope.drawCircle(
          color = moltenEye,
          radius = size * 0.04f,
          center = Offset(left + w * 0.36f, top + size * 0.15f)
        )
        drawScope.drawCircle(
          color = moltenEye,
          radius = size * 0.04f,
          center = Offset(left + w * 0.64f, top + size * 0.15f)
        )
        // Massive Muscular Torso & Spiked Belt
        drawScope.drawRect(
          color = Color(0xFF261814).copy(alpha = alpha),
          topLeft = Offset(left + w * 0.1f, top + size * 0.3f),
          size = Size(w * 0.8f, size * 0.45f)
        )
        // Spiked Shoulder Pauldrons
        drawScope.drawCircle(
          color = Color(0xFF757575).copy(alpha = alpha),
          radius = size * 0.08f,
          center = Offset(left + w * 0.1f, top + size * 0.32f)
        )
        drawScope.drawCircle(
          color = Color(0xFF757575).copy(alpha = alpha),
          radius = size * 0.08f,
          center = Offset(left + w * 0.9f, top + size * 0.32f)
        )
      }
    }

    // Health Bar above enemy head
    if (enemy.hp > 0 && enemy.hp < enemy.maxHp) {
      val barW = w * 0.9f
      val barH = (size * 0.05f).coerceAtLeast(6f)
      val barX = centerX - barW * 0.5f
      val barY = top - barH * 2.2f

      // Background
      drawScope.drawRect(
        color = Color(0xCC000000),
        topLeft = Offset(barX, barY),
        size = Size(barW, barH)
      )
      // HP fill
      val hpPct = (enemy.hp.toFloat() / enemy.maxHp.toFloat()).coerceIn(0f, 1f)
      val hpColor = if (hpPct > 0.5f) Color(0xFF4CAF50) else if (hpPct > 0.25f) Color(0xFFFF9800) else Color(0xFFE53935)
      drawScope.drawRect(
        color = hpColor,
        topLeft = Offset(barX, barY),
        size = Size(barW * hpPct, barH)
      )
    }
  }

  private fun drawItemSprite(
    drawScope: DrawScope,
    item: DungeonItem,
    centerX: Float,
    centerY: Float,
    size: Float,
    alpha: Float
  ) {
    val left = centerX - size * 0.5f
    val top = centerY - size * 0.5f

    when (item.type) {
      ItemType.HEALTH_POTION -> {
        // Red Health Potion flask
        drawScope.drawCircle(
          color = Color(0xFFE53935).copy(alpha = alpha),
          radius = size * 0.4f,
          center = Offset(centerX, centerY + size * 0.1f)
        )
        // Flask neck & cork
        drawScope.drawRect(
          color = Color(0xFF8D6E63).copy(alpha = alpha),
          topLeft = Offset(centerX - size * 0.12f, centerY - size * 0.45f),
          size = Size(size * 0.24f, size * 0.25f)
        )
        // Red magic sparkle
        drawScope.drawCircle(
          color = Color(0xFFFFCDD2).copy(alpha = alpha),
          radius = size * 0.12f,
          center = Offset(centerX - size * 0.12f, centerY)
        )
      }

      ItemType.MANA_POTION -> {
        // Blue Mana Potion flask
        drawScope.drawCircle(
          color = Color(0xFF1E88E5).copy(alpha = alpha),
          radius = size * 0.4f,
          center = Offset(centerX, centerY + size * 0.1f)
        )
        drawScope.drawRect(
          color = Color(0xFF8D6E63).copy(alpha = alpha),
          topLeft = Offset(centerX - size * 0.12f, centerY - size * 0.45f),
          size = Size(size * 0.24f, size * 0.25f)
        )
        drawScope.drawCircle(
          color = Color(0xFFBBDEFB).copy(alpha = alpha),
          radius = size * 0.12f,
          center = Offset(centerX - size * 0.12f, centerY)
        )
      }

      ItemType.GOLD_CHEST -> {
        // Wooden treasure chest with gold straps
        val chestColor = Color(0xFF5D4037).copy(alpha = alpha)
        val goldColor = Color(0xFFFFD54F).copy(alpha = alpha)

        drawScope.drawRect(
          color = chestColor,
          topLeft = Offset(left, top + size * 0.2f),
          size = Size(size, size * 0.6f)
        )
        // Gold bands
        drawScope.drawRect(
          color = goldColor,
          topLeft = Offset(left + size * 0.18f, top + size * 0.2f),
          size = Size(size * 0.12f, size * 0.6f)
        )
        drawScope.drawRect(
          color = goldColor,
          topLeft = Offset(left + size * 0.7f, top + size * 0.2f),
          size = Size(size * 0.12f, size * 0.6f)
        )
        // Gold lock
        drawScope.drawCircle(
          color = goldColor,
          radius = size * 0.12f,
          center = Offset(centerX, centerY + size * 0.2f)
        )
      }

      ItemType.DUNGEON_KEY -> {
        // Ancient Bronze Key
        val keyColor = Color(0xFFFFB300).copy(alpha = alpha)
        // Key head loop
        drawScope.drawCircle(
          color = keyColor,
          radius = size * 0.24f,
          center = Offset(centerX, centerY - size * 0.2f),
          style = Stroke(width = size * 0.08f)
        )
        // Key shaft
        drawScope.drawRect(
          color = keyColor,
          topLeft = Offset(centerX - size * 0.05f, centerY - size * 0.05f),
          size = Size(size * 0.1f, size * 0.45f)
        )
        // Key teeth
        drawScope.drawRect(
          color = keyColor,
          topLeft = Offset(centerX + size * 0.05f, centerY + size * 0.2f),
          size = Size(size * 0.16f, size * 0.08f)
        )
      }

      ItemType.SWORD_UPGRADE -> {
        // Glowing Enchanted Sword
        val bladeColor = Color(0xFF00E5FF).copy(alpha = alpha)
        val hiltColor = Color(0xFFFFD54F).copy(alpha = alpha)
        // Diagonal enchanted blade
        drawScope.drawLine(
          color = bladeColor,
          start = Offset(left + size * 0.15f, top + size * 0.85f),
          end = Offset(left + size * 0.85f, top + size * 0.15f),
          strokeWidth = size * 0.12f
        )
        // Crossguard
        drawScope.drawLine(
          color = hiltColor,
          start = Offset(left + size * 0.1f, top + size * 0.65f),
          end = Offset(left + size * 0.45f, top + size * 0.95f),
          strokeWidth = size * 0.08f
        )
      }
    }
  }

  private fun drawProjectileSprite(
    drawScope: DrawScope,
    projectile: Projectile,
    centerX: Float,
    centerY: Float,
    size: Float
  ) {
    // Fireball
    drawScope.drawCircle(
      brush = Brush.radialGradient(
        colors = listOf(Color(0xFFFFF9C4), Color(0xFFFF9800), Color(0xFFE65100), Color.Transparent),
        center = Offset(centerX, centerY),
        radius = size * 0.8f
      ),
      radius = size * 0.8f,
      center = Offset(centerX, centerY)
    )
  }

  private fun renderCombatTexts(
    drawScope: DrawScope,
    width: Float,
    height: Float,
    horizon: Float,
    playerX: Float,
    playerY: Float,
    playerAngle: Float,
    combatTexts: List<CombatText>
  ) {
    combatTexts.forEach { ct ->
      val dx = ct.worldX - playerX
      val dy = ct.worldY - playerY

      val cosA = cos(-playerAngle)
      val sinA = sin(-playerAngle)
      val transX = dx * cosA - dy * sinA
      val transY = dx * sinA + dy * cosA

      if (transX > 0.3f) {
        val screenX = (width / 2f) * (1f + transY / (transX * kotlin.math.tan(halfFov)))
        val screenY = horizon - (height / transX) * 0.5f - ct.offsetY

        if (screenX in 20f..(width - 20f) && screenY in 20f..(height - 20f)) {
          drawScope.drawContext.canvas.nativeCanvas.apply {
            val paint = android.graphics.Paint().apply {
              color = ct.colorLong.toInt()
              this.alpha = (ct.alpha * 255).toInt().coerceIn(0, 255)
              textSize = (36f * (1.2f / transX).coerceIn(0.6f, 1.6f)).coerceIn(24f, 54f)
              typeface = android.graphics.Typeface.DEFAULT_BOLD
              textAlign = android.graphics.Paint.Align.CENTER
              setShadowLayer(8f, 0f, 0f, android.graphics.Color.BLACK)
            }
            drawText(ct.text, screenX, screenY, paint)
          }
        }
      }
    }
  }

  private fun renderFirstPersonWeapon(
    drawScope: DrawScope,
    width: Float,
    height: Float,
    attackProgress: Float,
    isMagicCast: Boolean,
    isShieldActive: Boolean,
    weaponTier: Int,
    headBob: Float
  ) {
    if (isShieldActive) {
      // Draw Shield raised in front
      val shieldW = width * 0.65f
      val shieldH = height * 0.45f
      val shieldX = (width - shieldW) * 0.5f
      val shieldY = height - shieldH * 0.75f + headBob

      drawScope.drawRoundRect(
        color = Color(0xFF37474F),
        topLeft = Offset(shieldX, shieldY),
        size = Size(shieldW, shieldH),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(32f, 32f)
      )
      // Bronze rim & emblem
      drawScope.drawRoundRect(
        color = Color(0xFFFFB300),
        topLeft = Offset(shieldX + 16f, shieldY + 16f),
        size = Size(shieldW - 32f, shieldH - 32f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
        style = Stroke(width = 12f)
      )
      return
    }

    if (isMagicCast) {
      // Draw Magical Arcane Hand & Fire swirling
      val handX = width * 0.7f
      val handY = height * 0.75f + headBob
      drawScope.drawCircle(
        brush = Brush.radialGradient(
          colors = listOf(Color(0xFFFFF59D), Color(0xFFFF9800), Color(0xFFD84315), Color.Transparent),
          center = Offset(handX, handY),
          radius = width * 0.22f
        ),
        radius = width * 0.22f,
        center = Offset(handX, handY)
      )
      return
    }

    // Default: Sword swing or idle in first person right corner
    // Weapon colors based on tier
    val bladeColor = when (weaponTier) {
      1 -> Color(0xFFECEFF1) // Iron
      2 -> Color(0xFF80DEEA) // Valyrian / Runic Steel
      else -> Color(0xFFFF7043) // Flaming Obsidian
    }
    val hiltColor = Color(0xFFFFB300)

    val swingAngle = if (attackProgress > 0f) {
      // Smooth slash arc from top-right to bottom-left
      val p = attackProgress
      -35f + p * 85f
    } else {
      0f
    }

    val weaponBaseX = width * 0.76f + (if (attackProgress > 0f) -sin(attackProgress * Math.PI.toFloat()) * width * 0.28f else 0f)
    val weaponBaseY = height * 0.88f + headBob + (if (attackProgress > 0f) sin(attackProgress * Math.PI.toFloat()) * height * 0.08f else 0f)

    // Save canvas & rotate for slash motion
    drawScope.drawContext.canvas.save()
    drawScope.drawContext.canvas.translate(weaponBaseX, weaponBaseY)
    drawScope.drawContext.canvas.rotate(swingAngle)
    drawScope.drawContext.canvas.translate(-weaponBaseX, -weaponBaseY)

    // Blade
    val bladeWidth = width * 0.045f
    val bladeLength = height * 0.42f
    drawScope.drawRect(
      color = bladeColor,
      topLeft = Offset(weaponBaseX - bladeWidth * 0.5f, weaponBaseY - bladeLength),
      size = Size(bladeWidth, bladeLength)
    )

    // Blade sharp tip
    val tipPath = Path().apply {
      moveTo(weaponBaseX - bladeWidth * 0.5f, weaponBaseY - bladeLength)
      lineTo(weaponBaseX, weaponBaseY - bladeLength - height * 0.06f)
      lineTo(weaponBaseX + bladeWidth * 0.5f, weaponBaseY - bladeLength)
      close()
    }
    drawScope.drawPath(tipPath, bladeColor)

    // Crossguard
    val guardWidth = width * 0.16f
    val guardHeight = height * 0.024f
    drawScope.drawRect(
      color = hiltColor,
      topLeft = Offset(weaponBaseX - guardWidth * 0.5f, weaponBaseY),
      size = Size(guardWidth, guardHeight)
    )

    // Hilt / Handle & Pommel
    drawScope.drawRect(
      color = Color(0xFF4E342E),
      topLeft = Offset(weaponBaseX - bladeWidth * 0.4f, weaponBaseY + guardHeight),
      size = Size(bladeWidth * 0.8f, height * 0.1f)
    )
    drawScope.drawCircle(
      color = hiltColor,
      radius = bladeWidth * 0.7f,
      center = Offset(weaponBaseX, weaponBaseY + guardHeight + height * 0.1f)
    )

    // Slash Trail Visual Effect
    if (attackProgress in 0.15f..0.85f) {
      val trailAlpha = sin((attackProgress - 0.15f) / 0.7f * Math.PI.toFloat()).coerceIn(0f, 1f)
      drawScope.drawArc(
        color = bladeColor.copy(alpha = trailAlpha * 0.65f),
        startAngle = -80f,
        sweepAngle = 70f,
        useCenter = false,
        topLeft = Offset(weaponBaseX - width * 0.35f, weaponBaseY - bladeLength * 1.2f),
        size = Size(width * 0.7f, bladeLength * 1.5f),
        style = Stroke(width = 16f)
      )
    }

    drawScope.drawContext.canvas.restore()
  }
}
