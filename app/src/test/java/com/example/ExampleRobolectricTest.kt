package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Spotify Launcher", appName)
  }

  @Test
  fun `verify brave spotify launcher constants`() {
    assertEquals("https://open.spotify.com/", BraveSpotifyLauncher.SPOTIFY_WEB_URL)
    assertTrue(BraveSpotifyLauncher.BRAVE_PACKAGES.contains("com.brave.browser"))
  }
}
