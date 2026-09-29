package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
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
    assertEquals("DM Portal", appName)
  }

  @Test
  fun `default credentials are configured correctly`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val manager = com.example.data.CredentialManager(context)
    val creds = manager.getCredentials()
    assertEquals("8487086679", creds.phone)
    assertEquals("Kp87086679", creds.pass)
    assertEquals("https://dmfirst1.com/#/login", creds.loginUrl)
    assertEquals("https://dmfirst1.com/#/", creds.homeUrl)
  }

  @Test
  fun `room database inserts and queries wingo records`() = kotlinx.coroutines.test.runTest {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val db = androidx.room.Room.inMemoryDatabaseBuilder(
      context,
      com.example.data.db.AppDatabase::class.java
    ).allowMainThreadQueries().build()

    val dao = db.winGoRecordDao()
    val record = com.example.data.db.WinGoRecord(
      period = "20260927010543",
      number = 7,
      bigSmall = "Big",
      color = "Green",
      gameType = "WinGo 1Min",
      page = 1
    )
    dao.insertRecord(record)

    val queried = dao.getRecordByPeriod("20260927010543")
    org.junit.Assert.assertNotNull(queried)
    assertEquals(7, queried?.number)
    assertEquals("Big", queried?.bigSmall)
    assertEquals("Green", queried?.color)
    db.close()
  }

  @Test
  fun `portal view model initializes safely`() {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val vm = com.example.ui.PortalViewModel(application)
    org.junit.Assert.assertNotNull(vm.uiState.value)
    org.junit.Assert.assertEquals("8487086679", vm.uiState.value.credentials.phone)
  }
}
