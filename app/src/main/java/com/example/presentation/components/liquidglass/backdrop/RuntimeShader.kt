/*
 * Vendored from Kyant0/backdrop v2.0.0 (io.github.kyant0:backdrop)
 * https://github.com/Kyant0/backdrop — Copyright 2025 Kyant0, Apache License 2.0
 *
 * Vendored so the library ships as source with this app (binary AARs compiled
 * against older Compose broke at runtime) and to add a backdrop resolution
 * scale for cheaper effect rendering. KMP expect/actual declarations were
 * merged into this single Android source set. Package renamed accordingly.
 */
package com.example.presentation.components.liquidglass.backdrop

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.toArgb
import org.intellij.lang.annotations.Language

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
fun RuntimeShader(@Language("AGSL") shaderString: String): RuntimeShader {
  val shader = android.graphics.RuntimeShader(shaderString)
  return AndroidRuntimeShader(shader)
}

fun RuntimeShader.asComposeShader(): Shader {
  return this.asAndroidRuntimeShader()
}

fun RuntimeShader.asAndroidRuntimeShader(): android.graphics.RuntimeShader {
  return (this as AndroidRuntimeShader).shader
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
internal class AndroidRuntimeShader(val shader: android.graphics.RuntimeShader) : RuntimeShader {

  override fun setFloatUniform(name: String, value: Float) {
    try { shader.setFloatUniform(name, value) } catch (_: Throwable) { }
  }

  override fun setFloatUniform(name: String, value1: Float, value2: Float) {
    try { shader.setFloatUniform(name, value1, value2) } catch (_: Throwable) { }
  }

  override fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float) {
    try { shader.setFloatUniform(name, value1, value2, value3) } catch (_: Throwable) { }
  }

  override fun setFloatUniform(
    name: String,
    value1: Float,
    value2: Float,
    value3: Float,
    value4: Float
  ) {
    try { shader.setFloatUniform(name, value1, value2, value3, value4) } catch (_: Throwable) { }
  }

  override fun setFloatUniform(name: String, values: FloatArray) {
    try { shader.setFloatUniform(name, values) } catch (_: Throwable) { }
  }

  override fun setIntUniform(name: String, value: Int) {
    try { shader.setIntUniform(name, value) } catch (_: Throwable) { }
  }

  override fun setIntUniform(name: String, value1: Int, value2: Int) {
    try { shader.setIntUniform(name, value1, value2) } catch (_: Throwable) { }
  }

  override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int) {
    try { shader.setIntUniform(name, value1, value2, value3) } catch (_: Throwable) { }
  }

  override fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int, value4: Int) {
    try { shader.setIntUniform(name, value1, value2, value3, value4) } catch (_: Throwable) { }
  }

  override fun setIntUniform(name: String, values: IntArray) {
    try { shader.setIntUniform(name, values) } catch (_: Throwable) { }
  }

  override fun setColorUniform(name: String, color: Color) {
    try { shader.setColorUniform(name, color.toArgb()) } catch (_: Throwable) { }
  }
}

interface RuntimeShader {

  fun setFloatUniform(name: String, value: Float)

  fun setFloatUniform(name: String, value1: Float, value2: Float)

  fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float)

  fun setFloatUniform(name: String, value1: Float, value2: Float, value3: Float, value4: Float)

  fun setFloatUniform(name: String, values: FloatArray)

  fun setIntUniform(name: String, value: Int)

  fun setIntUniform(name: String, value1: Int, value2: Int)

  fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int)

  fun setIntUniform(name: String, value1: Int, value2: Int, value3: Int, value4: Int)

  fun setIntUniform(name: String, values: IntArray)

  fun setColorUniform(name: String, color: Color)
}
