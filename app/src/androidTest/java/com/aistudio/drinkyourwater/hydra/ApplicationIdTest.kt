package com.aistudio.drinkyourwater.hydra

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/** Guards against namespace/applicationId drifting apart again (they were mismatched before). */
@RunWith(AndroidJUnit4::class)
class ApplicationIdTest {
  @Test
  fun packageName_matchesApplicationId() {
    val appContext = InstrumentationRegistry.getInstrumentation().targetContext
    assertEquals("com.aistudio.drinkyourwater.hydra", appContext.packageName)
  }
}
