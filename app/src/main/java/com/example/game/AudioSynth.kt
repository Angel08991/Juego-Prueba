package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin
import kotlin.random.Random

object AudioSynth {
  private val scope = CoroutineScope(Dispatchers.Default)
  var isMuted = false

  private fun playPcm(sampleRate: Int = 22050, generator: (Int, Int) -> ShortArray) {
    if (isMuted) return
    scope.launch {
      try {
        val totalSamples = (sampleRate * 0.35f).toInt()
        val buffer = generator(sampleRate, totalSamples)
        val track = AudioTrack.Builder()
          .setAudioAttributes(
            AudioAttributes.Builder()
              .setUsage(AudioAttributes.USAGE_GAME)
              .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
              .build()
          )
          .setAudioFormat(
            AudioFormat.Builder()
              .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
              .setSampleRate(sampleRate)
              .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
              .build()
          )
          .setBufferSizeInBytes(buffer.size * 2)
          .setTransferMode(AudioTrack.MODE_STATIC)
          .build()

        track.write(buffer, 0, buffer.size)
        track.play()
        // Wait then release
        kotlinx.coroutines.delay((buffer.size * 1000L / sampleRate) + 50)
        track.stop()
        track.release()
      } catch (_: Exception) {
        // Audio synthesis fallback
      }
    }
  }

  fun playSlash() {
    playPcm { sampleRate, _ ->
      val length = (sampleRate * 0.16f).toInt()
      val buffer = ShortArray(length)
      val random = Random(42)
      for (i in 0 until length) {
        val progress = i.toFloat() / length
        val freq = 700.0 * (1.0 - progress * 0.6)
        val wave = sin(2.0 * Math.PI * i * freq / sampleRate)
        val noise = (random.nextFloat() * 2f - 1f) * 0.3f
        val envelope = (1f - progress) * (1f - progress)
        buffer[i] = ((wave * 0.7 + noise) * envelope * 24000).toInt().coerceIn(-32767, 32767).toShort()
      }
      buffer
    }
  }

  fun playHit() {
    playPcm { sampleRate, _ ->
      val length = (sampleRate * 0.18f).toInt()
      val buffer = ShortArray(length)
      val random = Random(123)
      for (i in 0 until length) {
        val progress = i.toFloat() / length
        val noise = (random.nextFloat() * 2f - 1f) * 0.7f
        val thud = sin(2.0 * Math.PI * i * 110.0 * (1.0 - progress * 0.5) / sampleRate)
        val envelope = (1f - progress)
        buffer[i] = ((noise + thud * 0.6) * envelope * 28000).toInt().coerceIn(-32767, 32767).toShort()
      }
      buffer
    }
  }

  fun playFireball() {
    playPcm { sampleRate, _ ->
      val length = (sampleRate * 0.28f).toInt()
      val buffer = ShortArray(length)
      val random = Random(99)
      for (i in 0 until length) {
        val progress = i.toFloat() / length
        val freq = 300.0 + sin(progress * 15.0) * 80.0
        val wave = sin(2.0 * Math.PI * i * freq / sampleRate)
        val noise = (random.nextFloat() * 2f - 1f) * 0.5f
        val envelope = if (progress < 0.2f) progress / 0.2f else (1f - progress)
        buffer[i] = ((wave * 0.6 + noise * 0.5) * envelope * 25000).toInt().coerceIn(-32767, 32767).toShort()
      }
      buffer
    }
  }

  fun playChest() {
    playPcm { sampleRate, _ ->
      val length = (sampleRate * 0.35f).toInt()
      val buffer = ShortArray(length)
      val notes = doubleArrayOf(440.0, 554.37, 659.25, 880.0) // A, C#, E, A high
      val noteLength = length / notes.size
      for (i in 0 until length) {
        val noteIdx = (i / noteLength).coerceAtMost(notes.size - 1)
        val freq = notes[noteIdx]
        val noteProgress = (i % noteLength).toFloat() / noteLength
        val wave = sin(2.0 * Math.PI * i * freq / sampleRate)
        val envelope = (1f - noteProgress * 0.8f)
        buffer[i] = (wave * envelope * 22000).toInt().coerceIn(-32767, 32767).toShort()
      }
      buffer
    }
  }

  fun playStep() {
    playPcm { sampleRate, _ ->
      val length = (sampleRate * 0.08f).toInt()
      val buffer = ShortArray(length)
      val random = Random(System.nanoTime())
      for (i in 0 until length) {
        val progress = i.toFloat() / length
        val noise = (random.nextFloat() * 2f - 1f)
        val envelope = (1f - progress) * (1f - progress)
        buffer[i] = (noise * envelope * 12000).toInt().coerceIn(-32767, 32767).toShort()
      }
      buffer
    }
  }

  fun playLevelUp() {
    playPcm { sampleRate, _ ->
      val length = (sampleRate * 0.45f).toInt()
      val buffer = ShortArray(length)
      val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50) // C, E, G, high C
      val noteLength = length / notes.size
      for (i in 0 until length) {
        val noteIdx = (i / noteLength).coerceAtMost(notes.size - 1)
        val freq = notes[noteIdx]
        val wave = sin(2.0 * Math.PI * i * freq / sampleRate) + 0.3 * sin(4.0 * Math.PI * i * freq / sampleRate)
        val progress = (i % noteLength).toFloat() / noteLength
        val envelope = (1f - progress * 0.7f)
        buffer[i] = (wave * envelope * 24000).toInt().coerceIn(-32767, 32767).toShort()
      }
      buffer
    }
  }

  fun playPlayerHurt() {
    playPcm { sampleRate, _ ->
      val length = (sampleRate * 0.15f).toInt()
      val buffer = ShortArray(length)
      for (i in 0 until length) {
        val progress = i.toFloat() / length
        val freq = 160.0 * (1.0 - progress * 0.5)
        val wave = sin(2.0 * Math.PI * i * freq / sampleRate)
        val envelope = (1f - progress)
        buffer[i] = (wave * envelope * 26000).toInt().coerceIn(-32767, 32767).toShort()
      }
      buffer
    }
  }

  fun playUnlock() {
    playPcm { sampleRate, _ ->
      val length = (sampleRate * 0.15f).toInt()
      val buffer = ShortArray(length)
      for (i in 0 until length) {
        val progress = i.toFloat() / length
        val freq = if (progress < 0.5f) 800.0 else 1200.0
        val wave = sin(2.0 * Math.PI * i * freq / sampleRate)
        val envelope = 1f - progress
        buffer[i] = (wave * envelope * 20000).toInt().coerceIn(-32767, 32767).toShort()
      }
      buffer
    }
  }
}
