package me.williamhester.kdash.api

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef
import com.sun.jna.platform.win32.WinUser
import com.sun.jna.win32.W32APIOptions

class Broadcaster {

  fun sendBroadcastMessage() {
//    user32.SendMessage(WinUser.HWND_BROADCAST, )
  }

  sealed class Message(val param1: Long) {

  }

  private companion object {
    val user32 = Native.loadLibrary("user32", User32::class.java, W32APIOptions.DEFAULT_OPTIONS) as User32
  }
}