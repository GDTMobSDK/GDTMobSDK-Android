package com.qq.e.union.demo.util;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.qq.e.union.demo.DemoApplication;

public class ToastUtil {

  private static final Looper sMainLooper = Looper.getMainLooper();
  private static final Handler sMainHandler = new Handler(sMainLooper);

  public static void s(String msg) {
    if (Thread.currentThread() == sMainLooper.getThread()) {
      showToast(msg, Toast.LENGTH_SHORT);
    } else {
      sMainHandler.post(() -> showToast(msg, Toast.LENGTH_SHORT));
    }
  }

  public static void l(String msg) {
    if (Thread.currentThread() == sMainLooper.getThread()) {
      showToast(msg, Toast.LENGTH_LONG);
    } else {
      sMainHandler.post(() -> showToast(msg, Toast.LENGTH_LONG));
    }
  }

  private static void showToast(String msg, int duration) {
    Toast.makeText(DemoApplication.getAppContext(), msg, duration).show();
  }
}
