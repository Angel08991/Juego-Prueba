package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VolumeMute
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DungeonRecord
import com.example.ui.theme.DungeonBorder
import com.example.ui.theme.DungeonSurface
import com.example.ui.theme.GoldYellow
import com.example.ui.theme.HealthRed
import com.example.ui.theme.TorchAmber

@Composable
fun LevelCompletedDialog(
  floor: Int,
  enemiesKilled: Int,
  goldEarned: Int,
  onNextFloor: () -> Unit
) {
  Dialog(onDismissRequest = {}) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("dialog_level_completed"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = DungeonSurface),
      border = androidx.compose.foundation.BorderStroke(2.dp, GoldYellow)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          imageVector = Icons.Default.EmojiEvents,
          contentDescription = null,
          tint = GoldYellow,
          modifier = Modifier.size(54.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "¡PISO $floor SUPERADO!",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
          ),
          color = GoldYellow,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Has derrotado a los guardianes y encontrado el portal hacia las profundidades.",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFCBD5E1),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          StatBadge(title = "Monstruos", value = enemiesKilled.toString(), color = HealthRed)
          StatBadge(title = "Oro", value = goldEarned.toString(), color = GoldYellow)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onNextFloor,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("btn_next_floor"),
          colors = ButtonDefaults.buttonColors(containerColor = TorchAmber),
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Descender al Piso ${floor + 1}",
              fontWeight = FontWeight.Bold,
              color = Color.Black,
              fontSize = 16.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun GameOverDialog(
  floor: Int,
  enemiesKilled: Int,
  goldEarned: Int,
  onRestart: () -> Unit
) {
  Dialog(onDismissRequest = {}) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("dialog_game_over"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = DungeonSurface),
      border = androidx.compose.foundation.BorderStroke(2.dp, HealthRed)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          imageVector = Icons.Default.Favorite,
          contentDescription = null,
          tint = HealthRed,
          modifier = Modifier.size(54.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "HAS CAÍDO EN COMBATE",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.2.sp
          ),
          color = HealthRed,
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Tu alma descansará eternamente en las catacumbas... ¡pero puedes intentarlo de nuevo!",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFCBD5E1),
          textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          StatBadge(title = "Piso", value = floor.toString(), color = TorchAmber)
          StatBadge(title = "Enemigos", value = enemiesKilled.toString(), color = HealthRed)
          StatBadge(title = "Oro", value = goldEarned.toString(), color = GoldYellow)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onRestart,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("btn_restart"),
          colors = ButtonDefaults.buttonColors(containerColor = HealthRed),
          shape = RoundedCornerShape(12.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Reintentar Mazmorra",
              fontWeight = FontWeight.Bold,
              color = Color.White,
              fontSize = 16.sp
            )
          }
        }
      }
    }
  }
}

@Composable
fun VictoryDialog(
  score: Int,
  gold: Int,
  kills: Int,
  onRestart: () -> Unit,
  onEndlessMode: () -> Unit
) {
  Dialog(onDismissRequest = {}) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("dialog_victory"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = DungeonSurface),
      border = androidx.compose.foundation.BorderStroke(2.dp, GoldYellow)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Icon(
          imageVector = Icons.Default.EmojiEvents,
          contentDescription = null,
          tint = GoldYellow,
          modifier = Modifier.size(60.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "¡VICTORIA ÉPICA!",
          style = MaterialTheme.typography.headlineSmall.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 2.sp
          ),
          color = GoldYellow,
          textAlign = TextAlign.Center
        )

        Text(
          text = "¡Has derrotado al temible Señor del Abismo y conquistado la Mazmorra!",
          style = MaterialTheme.typography.bodyMedium,
          color = Color(0xFFE2E8F0),
          textAlign = TextAlign.Center,
          modifier = Modifier.padding(top = 8.dp)
        )

        Spacer(modifier = Modifier.height(18.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceEvenly
        ) {
          StatBadge(title = "Puntaje", value = score.toString(), color = GoldYellow)
          StatBadge(title = "Bajas", value = kills.toString(), color = HealthRed)
          StatBadge(title = "Oro", value = gold.toString(), color = TorchAmber)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
          onClick = onEndlessMode,
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag("btn_endless_mode"),
          colors = ButtonDefaults.buttonColors(containerColor = TorchAmber),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(text = "Explorar Mazmorra Infinita", fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedButton(
          onClick = onRestart,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_victory_restart"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(text = "Comenzar Nueva Partida", color = Color.White)
        }
      }
    }
  }
}

@Composable
fun PauseAndScoresDialog(
  onResume: () -> Unit,
  onRestart: () -> Unit,
  isMuted: Boolean,
  onToggleMute: () -> Unit,
  highScores: List<DungeonRecord>
) {
  Dialog(onDismissRequest = onResume) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(16.dp)
        .testTag("dialog_pause"),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = DungeonSurface),
      border = androidx.compose.foundation.BorderStroke(1.5.dp, DungeonBorder)
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = "PAUSA - MAZMORRA 3D",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = TorchAmber
          )

          IconButton(
            onClick = onToggleMute,
            modifier = Modifier.testTag("btn_toggle_mute")
          ) {
            Icon(
              imageVector = if (isMuted) Icons.Default.VolumeMute else Icons.Default.VolumeUp,
              contentDescription = "Sonido",
              tint = if (isMuted) Color.Gray else TorchAmber
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = "🏆 Salón de Récords",
          style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
          color = GoldYellow,
          modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (highScores.isEmpty()) {
          Text(
            text = "Aún no hay récords grabados. ¡Conquista las profundidades!",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 12.dp)
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(140.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFF0F1218))
              .padding(8.dp)
          ) {
            items(highScores.take(5)) { record ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  text = "Piso ${record.floorReached} (${record.outcome})",
                  color = Color(0xFFCBD5E1),
                  fontSize = 12.sp
                )
                Text(
                  text = "${record.score} pts",
                  color = GoldYellow,
                  fontWeight = FontWeight.Bold,
                  fontSize = 12.sp
                )
              }
              HorizontalDivider(color = Color(0xFF1E2430), thickness = 0.5.dp)
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
          onClick = onResume,
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .testTag("btn_resume"),
          colors = ButtonDefaults.buttonColors(containerColor = TorchAmber),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(text = "Reanudar Aventura", fontWeight = FontWeight.Bold, color = Color.Black)
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedButton(
          onClick = onRestart,
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("btn_pause_restart"),
          shape = RoundedCornerShape(12.dp)
        ) {
          Text(text = "Reiniciar desde el Piso 1", color = Color(0xFFEF5350))
        }
      }
    }
  }
}

@Composable
fun StatBadge(title: String, value: String, color: Color) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(10.dp))
      .background(Color(0xFF1E2330))
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Text(text = title, fontSize = 11.sp, color = Color(0xFF94A3B8))
    Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = color)
  }
}
