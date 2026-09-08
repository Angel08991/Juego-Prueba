package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.DungeonScreen
import com.example.ui.DungeonViewModel
import com.example.ui.PauseAndScoresDialog
import com.example.ui.theme.DungeonBorder
import com.example.ui.theme.DungeonSurface
import com.example.ui.theme.DungeonTheme
import com.example.ui.theme.GoldYellow
import com.example.ui.theme.TorchAmber

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      DungeonTheme {
        val viewModel: DungeonViewModel = viewModel()
        var hasStartedGame by remember { mutableStateOf(false) }
        var showScoresFromMenu by remember { mutableStateOf(false) }

        Scaffold(
          modifier = Modifier.fillMaxSize(),
          containerColor = Color.Black
        ) { innerPadding ->
          if (!hasStartedGame) {
            DungeonMainMenu(
              onStartGame = {
                viewModel.restartGame()
                hasStartedGame = true
              },
              onShowScores = { showScoresFromMenu = true },
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
            )

            if (showScoresFromMenu) {
              val scores by viewModel.highScores.collectAsState()
              val state by viewModel.uiState.collectAsState()
              PauseAndScoresDialog(
                onResume = { showScoresFromMenu = false },
                onRestart = {
                  showScoresFromMenu = false
                  viewModel.restartGame()
                  hasStartedGame = true
                },
                isMuted = state.isSoundMuted,
                onToggleMute = { viewModel.toggleMute() },
                highScores = scores
              )
            }
          } else {
            DungeonScreen(
              viewModel = viewModel,
              modifier = Modifier.padding(innerPadding)
            )
          }
        }
      }
    }
  }
}

@Composable
fun DungeonMainMenu(
  onStartGame: () -> Unit,
  onShowScores: () -> Unit,
  viewModel: DungeonViewModel,
  modifier: Modifier = Modifier
) {
  val state by viewModel.uiState.collectAsState()

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(Color(0xFF0B0D11))
  ) {
    // Hero Banner Image
    Image(
      painter = painterResource(id = R.drawable.bg_dungeon_banner),
      contentDescription = "Mazmorra 3D Banner",
      modifier = Modifier
        .fillMaxSize(),
      contentScale = ContentScale.Crop
    )

    // Dark Gradient Overlay for readability
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(
              Color(0x99000000),
              Color(0xD90D0F14),
              Color(0xFA0B0D11)
            )
          )
        )
    )

    // Sound toggle at top corner
    IconButton(
      onClick = { viewModel.toggleMute() },
      modifier = Modifier
        .align(Alignment.TopEnd)
        .padding(16.dp)
        .testTag("btn_menu_mute")
    ) {
      Icon(
        imageVector = if (state.isSoundMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
        contentDescription = "Sonido",
        tint = if (state.isSoundMuted) Color.Gray else TorchAmber,
        modifier = Modifier.size(28.dp)
      )
    }

    // Title & Actions Column
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .align(Alignment.BottomCenter)
        .padding(horizontal = 24.dp, vertical = 36.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      // Emblem icon
      Image(
        painter = painterResource(id = R.drawable.ic_dungeon_logo),
        contentDescription = "Logo",
        modifier = Modifier
          .size(80.dp)
          .clip(RoundedCornerShape(20.dp))
          .border(2.dp, TorchAmber, RoundedCornerShape(20.dp))
      )

      Spacer(modifier = Modifier.height(16.dp))

      Text(
        text = "MAZMORRA 3D",
        style = MaterialTheme.typography.headlineLarge.copy(
          fontWeight = FontWeight.Black,
          letterSpacing = 2.5.sp
        ),
        color = GoldYellow,
        textAlign = TextAlign.Center
      )

      Text(
        text = "Explora los oscuros pasadizos, combate esqueletos y monstruos legendarios, encuentra tesoros y derrota al Señor del Abismo.",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFFCBD5E1),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 8.dp, vertical = 12.dp)
      )

      // Feature highlights
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
      ) {
        MenuFeatureChip("🎮 3D Real", "Motor Raycasting")
        MenuFeatureChip("⚔️ Combate", "Espada y Magia")
        MenuFeatureChip("🗝️ Misterio", "Cofres y Llaves")
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Play Button
      Button(
        onClick = onStartGame,
        modifier = Modifier
          .fillMaxWidth()
          .height(56.dp)
          .testTag("btn_start_adventure"),
        colors = ButtonDefaults.buttonColors(containerColor = TorchAmber),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "ENTRAR A LA MAZMORRA",
            fontSize = 17.sp,
            fontWeight = FontWeight.Black,
            color = Color.Black,
            letterSpacing = 1.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // High scores button
      OutlinedButton(
        onClick = onShowScores,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp)
          .testTag("btn_menu_scores"),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, DungeonBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = DungeonSurface.copy(alpha = 0.8f))
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = GoldYellow, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Salón de Récords",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
          )
        }
      }
    }
  }
}

@Composable
fun MenuFeatureChip(title: String, subtitle: String) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0x991E2330))
      .border(1.dp, Color(0x66384358), RoundedCornerShape(10.dp))
      .padding(horizontal = 10.dp, vertical = 6.dp)
  ) {
    Text(text = title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TorchAmber)
    Text(text = subtitle, fontSize = 10.sp, color = Color(0xFF94A3B8))
  }
}
