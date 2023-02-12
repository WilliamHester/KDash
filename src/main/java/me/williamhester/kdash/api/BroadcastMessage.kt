package me.williamhester.kdash.api

import com.sun.jna.Native
import com.sun.jna.platform.win32.User32
import com.sun.jna.platform.win32.WinDef.HWND
import com.sun.jna.platform.win32.WinDef.LPARAM
import com.sun.jna.platform.win32.WinDef.WPARAM
import com.sun.jna.win32.W32APIOptions


sealed class BroadcastMessage(
  private val id: Int,
  private val arg1: Int,
  private val arg2: Int
) {
  fun send() {
    user32.SendNotifyMessage(User32.HWND_BROADCAST, newMessageId(), WPARAM(packInts(id, arg1).toLong()), LPARAM(arg2.toLong()))
  }

  class ReplaySetPlayPosition(
    posMode: ReplayPositionMode,
    frame: Int
  ) : BroadcastMessage(4, posMode.id, frame) {
    enum class ReplayPositionMode(val id: Int) {
      BEGIN(0),   // irsdk_RpyPos_Begin = 0,
      CURRENT(1), // irsdk_RpyPos_Current,
      END(2),     // irsdk_RpyPos_End,
      LAST(3),    // irsdk_RpyPos_Last                   // unused placeholder
    }
  }

  interface User32Extended : User32 {
    fun SendNotifyMessage(hWnd: HWND?, msg: Int, wParam: WPARAM, lParam: LPARAM): Boolean
  }

  companion object {
    private var user32 = Native.load("user32", User32Extended::class.java, W32APIOptions.DEFAULT_OPTIONS)

    private fun newMessageId(): Int {
      return user32.RegisterWindowMessage("IRSDK_BROADCASTMSG")
    }

    private fun packInts(high: Int, low: Int): Int {
      return high.shl(16).or(low)
    }
  }
}