package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.RaycastRenderer
import com.example.game.WallType
import com.example.ui.theme.DungeonBorder
import com.example.ui.theme.DungeonSurface
import com.example.ui.theme.DungeonSurfaceVariant
import com.example.ui.theme.GoldYellow
import com.example.ui.theme.HealthRed
import com.example.ui.theme.ManaBlue
import com.example.ui.theme.TorchAmber
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun DungeonScreen(
  viewModel: DungeonViewModel,
  modifier: Modifier = Modifier
) {
  val state by viewModel.uiState.collectAsState()
  val highScores by viewModel.highScores.collectAsState()
  val renderer = remember { RaycastRenderer() }
  var showMinimap by remember { mutableStateOf(true) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color.Black)
  ) {
    // 1. 3D Raycasting Viewport
    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
          detectDragGestures(
            onDrag = { change, dragAmount ->
              change.consume()
              // Horizontal swipe rotates camera smoothly
              viewModel.turnByDrag(dragAmount.x)
            }
          )
        }
    ) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        renderer.render(
          drawScope = this,
          map = viewModel.getMapArray(),
          mapWidth = state.mapWidth,
          mapHeight = state.mapHeight,
          playerX = state.playerX,
          playerY = state.playerY,
          playerAngle = state.playerAngle,
          headBob = state.headBob,
          attackProgress = state.attackProgress,
          isMagicCast = state.isMagicCast,
          isShieldActive = state.isShieldActive,
          hurtVignetteAlpha = state.hurtVignetteAlpha,
          torchFlicker = state.torchFlicker,
          enemies = state.enemies,
          items = state.items,
          projectiles = state.projectiles,
          combatTexts = state.combatTexts,
          weaponTier = state.playerStats.weaponTier
        )
      }

      // Crosshair Reticle
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .size(12.dp)
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val c = Offset(size.width / 2f, size.height / 2f)
          drawCircle(color = Color.White.copy(alpha = 0.5f), radius = 3.dp.toPx(), center = c)
        }
      }

      // Swipe to look guide hint (briefly visible initially)
      Text(
        text = "Desliza en pantalla para girar la vista",
        color = Color.White.copy(alpha = 0.4f),
        fontSize = 11.sp,
        modifier = Modifier
          .align(Alignment.Center)
          .padding(top = 44.dp)
      )
    }

    // 2. Top HUD Bar
    TopHudBar(
      state = state,
      showMinimap = showMinimap,
      onToggleMinimap = { showMinimap = !showMinimap },
      onPause = { viewModel.togglePause() },
      modifier = Modifier
        .align(Alignment.TopCenter)
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp)
    )

    // 3. Floating Minimap Radar (Toggleable)
    if (showMinimap) {
      MinimapRadar(
        viewModel = viewModel,
        state = state,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .padding(top = 76.dp, end = 12.dp)
      )
    }

    // 4. Contextual Interaction Prompt (e.g. Near Door, Chest, Portal)
    if (state.interactPrompt != null) {
      Card(
        modifier = Modifier
          .align(Alignment.Center)
          .padding(top = 130.dp)
          .clickable { viewModel.interact() }
          .testTag("prompt_interaction"),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = TorchAmber.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(Icons.Default.TouchApp, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = state.interactPrompt ?: "",
            color = Color.Black,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
          )
        }
      }
    }

    // 5. Bottom Touch Controls (D-Pad, Attack, Spell, Shield, Potions)
    BottomControlsLayout(
      state = state,
      onMove = { fwd, str -> viewModel.setJoystickInput(fwd, str) },
      onTurn = { turn -> viewModel.setTurnInput(turn) },
      onAttack = { viewModel.performAttack() },
      onCastFireball = { viewModel.castFireball() },
      onToggleShield = { active -> viewModel.toggleShield(active) },
      onInteract = { viewModel.interact() },
      onUseHealthPotion = { viewModel.useHealthPotion() },
      onUseManaPotion = { viewModel.useManaPotion() },
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
        .padding(bottom = 12.dp, start = 8.dp, end = 8.dp)
    )

    // 6. Dialogs (Level Completed, Game Over, Victory, Pause)
    when (state.gameState) {
      GameState.LEVEL_COMPLETED -> {
        LevelCompletedDialog(
          floor = state.currentFloor,
          enemiesKilled = state.totalEnemiesKilled,
          goldEarned = state.totalGoldEarned,
          onNextFloor = { viewModel.nextFloor() }
        )
      }
      GameState.GAME_OVER -> {
        GameOverDialog(
          floor = state.currentFloor,
          enemiesKilled = state.totalEnemiesKilled,
          goldEarned = state.totalGoldEarned,
          onRestart = { viewModel.restartGame() }
        )
      }
      GameState.VICTORY -> {
        val score = state.totalEnemiesKilled * 100 + state.totalGoldEarned * 10 + 2000
        VictoryDialog(
          score = score,
          gold = state.totalGoldEarned,
          kills = state.totalEnemiesKilled,
          onRestart = { viewModel.restartGame() },
          onEndlessMode = { viewModel.nextFloor() }
        )
      }
      GameState.PAUSED -> {
        PauseAndScoresDialog(
          onResume = { viewModel.togglePause() },
          onRestart = { viewModel.restartGame() },
          isMuted = state.isSoundMuted,
          onToggleMute = { viewModel.toggleMute() },
          highScores = highScores
        )
      }
      GameState.RUNNING -> {}
    }
  }
}

@Composable
fun TopHudBar(
  state: DungeonUiState,
  showMinimap: Boolean,
  onToggleMinimap: () -> Unit,
  onPause: () -> Unit,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier.testTag("top_hud_bar"),
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = DungeonSurface.copy(alpha = 0.92f)),
    border = androidx.compose.foundation.BorderStroke(1.dp, DungeonBorder)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 8.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      // Player Level & HP/MP Bars
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(TorchAmber)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "Nv. ${state.playerStats.level}",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = Color.Black
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = "Piso ${state.currentFloor}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
          )

          Spacer(modifier = Modifier.width(10.dp))

          // Gold counter
          Icon(Icons.Default.MonetizationOn, contentDescription = "Oro", tint = GoldYellow, modifier = Modifier.size(16.dp))
          Text(text = " ${state.playerStats.gold}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GoldYellow)

          Spacer(modifier = Modifier.width(10.dp))

          // Keys counter
          Icon(Icons.Default.Key, contentDescription = "Llaves", tint = TorchAmber, modifier = Modifier.size(16.dp))
          Text(text = " ${state.playerStats.keys}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TorchAmber)
        }

        Spacer(modifier = Modifier.height(6.dp))

        // HP & MP Progress Bars
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
          // HP
          Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(text = "HP", fontSize = 10.sp, color = HealthRed, fontWeight = FontWeight.Bold)
              Text(text = "${state.playerStats.hp}/${state.playerStats.maxHp}", fontSize = 10.sp, color = Color(0xFFE2E8F0))
            }
            LinearProgressIndicator(
              progress = { (state.playerStats.hp.toFloat() / state.playerStats.maxHp.toFloat()).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = HealthRed,
              trackColor = Color(0xFF3E1717)
            )
          }

          Spacer(modifier = Modifier.width(10.dp))

          // MP
          Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text(text = "MP", fontSize = 10.sp, color = ManaBlue, fontWeight = FontWeight.Bold)
              Text(text = "${state.playerStats.mp}/${state.playerStats.maxMp}", fontSize = 10.sp, color = Color(0xFFE2E8F0))
            }
            LinearProgressIndicator(
              progress = { (state.playerStats.mp.toFloat() / state.playerStats.maxMp.toFloat()).coerceIn(0f, 1f) },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = ManaBlue,
              trackColor = Color(0xFF0F2642)
            )
          }
        }
      }

      Spacer(modifier = Modifier.width(12.dp))

      // Toggle Minimap & Pause Buttons
      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = onToggleMinimap,
          modifier = Modifier
            .size(36.dp)
            .testTag("btn_toggle_minimap")
        ) {
          Icon(
            imageVector = Icons.Default.Map,
            contentDescription = "Mapa",
            tint = if (showMinimap) TorchAmber else Color.Gray,
            modifier = Modifier.size(20.dp)
          )
        }

        IconButton(
          onClick = onPause,
          modifier = Modifier
            .size(36.dp)
            .testTag("btn_pause")
        ) {
          Icon(
            imageVector = Icons.Default.Pause,
            contentDescription = "Pausa",
            tint = Color.White,
            modifier = Modifier.size(22.dp)
          )
        }
      }
    }
  }
}

@Composable
fun MinimapRadar(
  viewModel: DungeonViewModel,
  state: DungeonUiState,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier
      .size(110.dp)
      .testTag("minimap_radar"),
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color(0xE60A0D12)),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, DungeonBorder)
  ) {
    Canvas(modifier = Modifier.fillMaxSize().padding(4.dp)) {
      val mapW = state.mapWidth
      val mapH = state.mapHeight
      val cellSizeX = size.width / mapW
      val cellSizeY = size.height / mapH

      // Draw explored walls & tiles
      for (y in 0 until mapH) {
        for (x in 0 until mapW) {
          if (viewModel.getExploredTile(x, y)) {
            val tile = viewModel.getMapTile(x, y)
            val rectOffset = Offset(x * cellSizeX, y * cellSizeY)
            val rectSize = androidx.compose.ui.geometry.Size(cellSizeX, cellSizeY)

            when (tile) {
              WallType.STONE_WALL.id, WallType.MOSSY_WALL.id, WallType.TORCH_WALL.id, WallType.RUNE_WALL.id -> {
                drawRect(color = Color(0xFF475569), topLeft = rectOffset, size = rectSize)
              }
              WallType.DOOR_LOCKED.id -> {
                drawRect(color = Color(0xFFFFB300), topLeft = rectOffset, size = rectSize)
              }
              WallType.DOOR_OPEN.id -> {
                drawRect(color = Color(0xFF64748B), topLeft = rectOffset, size = rectSize)
              }
              WallType.EXIT_PORTAL.id -> {
                drawRect(color = Color(0xFFAB47BC), topLeft = rectOffset, size = rectSize)
              }
              else -> {
                // Empty floor explored
                drawRect(color = Color(0xFF1E2430), topLeft = rectOffset, size = rectSize)
              }
            }
          }
        }
      }

      // Draw items (yellow dots for chests/items)
      state.items.forEach { item ->
        if (!item.isCollected && viewModel.getExploredTile(item.x.toInt(), item.y.toInt())) {
          drawCircle(
            color = GoldYellow,
            radius = 2.5.dp.toPx(),
            center = Offset(item.x * cellSizeX, item.y * cellSizeY)
          )
        }
      }

      // Draw nearby enemies (red dots)
      state.enemies.forEach { enemy ->
        if (enemy.isAlive && viewModel.getExploredTile(enemy.x.toInt(), enemy.y.toInt())) {
          drawCircle(
            color = HealthRed,
            radius = 3.dp.toPx(),
            center = Offset(enemy.x * cellSizeX, enemy.y * cellSizeY)
          )
        }
      }

      // Draw player dot with direction ray
      val px = state.playerX * cellSizeX
      val py = state.playerY * cellSizeY
      drawCircle(color = Color(0xFF00E5FF), radius = 3.5.dp.toPx(), center = Offset(px, py))

      // Direction line
      val dirLen = 8.dp.toPx()
      drawLine(
        color = Color(0xFF00E5FF),
        start = Offset(px, py),
        end = Offset(px + cos(state.playerAngle) * dirLen, py + sin(state.playerAngle) * dirLen),
        strokeWidth = 2.dp.toPx()
      )
    }
  }
}

@Composable
fun BottomControlsLayout(
  state: DungeonUiState,
  onMove: (forward: Float, strafe: Float) -> Unit,
  onTurn: (turn: Float) -> Unit,
  onAttack: () -> Unit,
  onCastFireball: () -> Unit,
  onToggleShield: (Boolean) -> Unit,
  onInteract: () -> Unit,
  onUseHealthPotion: () -> Unit,
  onUseManaPotion: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Quick Potion & Weapon bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Health Potion Quick Button
      BadgedBox(
        badge = {
          Badge(
            containerColor = HealthRed,
            contentColor = Color.White
          ) {
            Text(state.playerStats.healthPotions.toString())
          }
        }
      ) {
        Surface(
          onClick = onUseHealthPotion,
          modifier = Modifier.size(46.dp).testTag("btn_potion_health"),
          shape = CircleShape,
          color = Color(0xFF2C1517),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, HealthRed)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text("🧪", fontSize = 20.sp)
          }
        }
      }

      // Weapon Name
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(DungeonSurface.copy(alpha = 0.85f))
          .border(1.dp, DungeonBorder, RoundedCornerShape(12.dp))
          .padding(horizontal = 12.dp, vertical = 6.dp)
      ) {
        Text(
          text = "🗡️ ${state.playerStats.weaponName}",
          color = Color(0xFFCBD5E1),
          fontSize = 11.sp,
          fontWeight = FontWeight.Medium
        )
      }

      // Mana Potion Quick Button
      BadgedBox(
        badge = {
          Badge(
            containerColor = ManaBlue,
            contentColor = Color.White
          ) {
            Text(state.playerStats.manaPotions.toString())
          }
        }
      ) {
        Surface(
          onClick = onUseManaPotion,
          modifier = Modifier.size(46.dp).testTag("btn_potion_mana"),
          shape = CircleShape,
          color = Color(0xFF101E2E),
          border = androidx.compose.foundation.BorderStroke(1.5.dp, ManaBlue)
        ) {
          Box(contentAlignment = Alignment.Center) {
            Text("✨", fontSize = 20.sp)
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Main Gamepad row: Left D-Pad & Turn Buttons | Right Action Buttons
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Bottom
    ) {
      // Left: 4-Way Movement D-Pad + Turning
      DungeonDPad(
        onMove = onMove,
        onTurn = onTurn,
        modifier = Modifier.padding(start = 4.dp)
      )

      // Right: Action Buttons (Attack, Fireball, Shield, Interact)
      Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(end = 4.dp)
      ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          // Cast Fireball Spell Button
          Surface(
            onClick = onCastFireball,
            modifier = Modifier.size(54.dp).testTag("btn_action_spell"),
            shape = CircleShape,
            color = if (state.playerStats.mp >= 15) Color(0xFFE65100) else Color(0xFF3E2723),
            border = androidx.compose.foundation.BorderStroke(2.dp, if (state.playerStats.mp >= 15) TorchAmber else Color.DarkGray)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LocalFireDepartment, contentDescription = "Bola de Fuego", tint = Color.White, modifier = Modifier.size(24.dp))
                Text("15 MP", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }

          // Shield Block Toggle / Hold
          Surface(
            onClick = { onToggleShield(!state.isShieldActive) },
            modifier = Modifier.size(54.dp).testTag("btn_action_shield"),
            shape = CircleShape,
            color = if (state.isShieldActive) Color(0xFF1E88E5) else Color(0xFF263238),
            border = androidx.compose.foundation.BorderStroke(2.dp, if (state.isShieldActive) Color(0xFF64B5F6) else Color.Gray)
          ) {
            Box(contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.Shield, contentDescription = "Escudo", tint = Color.White, modifier = Modifier.size(24.dp))
                Text(if (state.isShieldActive) "ACTIVO" else "ESCUDO", fontSize = 8.sp, color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          // Interact / Open Door / Chest button
          Surface(
            onClick = onInteract,
            modifier = Modifier.size(54.dp).testTag("btn_action_interact"),
            shape = CircleShape,
            color = Color(0xFF4A148C),
            border = androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFBA68C8))
          ) {
            Box(contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.LockOpen, contentDescription = "Interactuar", tint = Color.White, modifier = Modifier.size(22.dp))
                Text("ABRIR", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
              }
            }
          }

          // Big Sword Attack Button
          Surface(
            onClick = onAttack,
            modifier = Modifier.size(68.dp).testTag("btn_action_attack"),
            shape = CircleShape,
            color = TorchAmber,
            border = androidx.compose.foundation.BorderStroke(2.5.dp, Color(0xFFFFD54F)),
            shadowElevation = 8.dp
          ) {
            Box(contentAlignment = Alignment.Center) {
              Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("⚔️", fontSize = 26.sp)
                Text("ATACAR", fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color.Black)
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun DungeonDPad(
  onMove: (forward: Float, strafe: Float) -> Unit,
  onTurn: (turn: Float) -> Unit,
  modifier: Modifier = Modifier
) {
  // Cross D-Pad with Forward, Back, Strafe Left, Strafe Right + Turn Buttons
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    // Forward Button
    PadButton(
      icon = Icons.Default.ArrowUpward,
      tag = "btn_pad_forward",
      onPressStart = { onMove(1f, 0f) },
      onPressEnd = { onMove(0f, 0f) }
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // Strafe Left Button
      PadButton(
        icon = Icons.Default.ArrowBack,
        tag = "btn_pad_left",
        onPressStart = { onMove(0f, -1f) },
        onPressEnd = { onMove(0f, 0f) }
      )

      // Turn Left (<)
      Surface(
        onClick = { onTurn(-1.2f) },
        modifier = Modifier.size(38.dp).testTag("btn_turn_left"),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF232A38),
        border = androidx.compose.foundation.BorderStroke(1.dp, DungeonBorder)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text("⟲", fontSize = 18.sp, color = TorchAmber, fontWeight = FontWeight.Bold)
        }
      }

      // Turn Right (>)
      Surface(
        onClick = { onTurn(1.2f) },
        modifier = Modifier.size(38.dp).testTag("btn_turn_right"),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF232A38),
        border = androidx.compose.foundation.BorderStroke(1.dp, DungeonBorder)
      ) {
        Box(contentAlignment = Alignment.Center) {
          Text("⟳", fontSize = 18.sp, color = TorchAmber, fontWeight = FontWeight.Bold)
        }
      }

      // Strafe Right Button
      PadButton(
        icon = Icons.Default.ArrowForward,
        tag = "btn_pad_right",
        onPressStart = { onMove(0f, 1f) },
        onPressEnd = { onMove(0f, 0f) }
      )
    }

    Spacer(modifier = Modifier.height(4.dp))

    // Backward Button
    PadButton(
      icon = Icons.Default.ArrowDownward,
      tag = "btn_pad_backward",
      onPressStart = { onMove(-1f, 0f) },
      onPressEnd = { onMove(0f, 0f) }
    )
  }
}

@Composable
fun PadButton(
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  tag: String,
  onPressStart: () -> Unit,
  onPressEnd: () -> Unit
) {
  Surface(
    modifier = Modifier
      .size(46.dp)
      .testTag(tag)
      .pointerInput(Unit) {
        detectDragGestures(
          onDragStart = { onPressStart() },
          onDragEnd = { onPressEnd() },
          onDragCancel = { onPressEnd() },
          onDrag = { _, _ -> }
        )
      },
    shape = RoundedCornerShape(10.dp),
    color = Color(0xFF1E2430),
    border = androidx.compose.foundation.BorderStroke(1.5.dp, DungeonBorder),
    shadowElevation = 4.dp
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxSize()
        .clickable {
          onPressStart()
          // Brief click also moves
        }
    ) {
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier.size(24.dp)
      )
    }
  }
}
