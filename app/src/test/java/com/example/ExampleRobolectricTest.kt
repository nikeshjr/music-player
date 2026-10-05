package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import org.junit.Rule

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createAndroidComposeRule<MainActivity>()

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Aura Music", appName)
  }

  @Test
  fun `test AGSL shader compilation`() {
    if (android.os.Build.VERSION.SDK_INT >= 33) {
      android.graphics.RuntimeShader(com.example.presentation.components.liquidglass.backdrop.internal.RoundedRectRefractionShaderString)
      android.graphics.RuntimeShader(com.example.presentation.components.liquidglass.backdrop.internal.RoundedRectRefractionWithDispersionShaderString)
      android.graphics.RuntimeShader(com.example.presentation.components.liquidglass.backdrop.internal.DefaultHighlightShaderString)
      android.graphics.RuntimeShader(com.example.presentation.components.liquidglass.backdrop.internal.AmbientHighlightShaderString)
    }
  }

  @Test
  fun `launch MainActivity on Android 16`() {
    val controller = org.robolectric.Robolectric.buildActivity(MainActivity::class.java)
    controller.setup()
  }
}
