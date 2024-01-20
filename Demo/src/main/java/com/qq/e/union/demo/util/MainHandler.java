package com.qq.e.union.demo.util;

import android.os.Handler;
import android.os.Looper;

public class MainHandler {

  private static final Looper mainLooper = Looper.getMainLooper();
  private static Handler mainHandler;

  private static Handler getMain() {
    if (mainHandler == null) {
      mainHandler = new Handler(mainLooper);
    }
    return mainHandler;
  }

  public static boolean postIfNotMain(Runnable r) {
    if (r == null) {
      return false;
    }
    if (Thread.currentThread() == mainLooper.getThread()) {
      r.run();
      return true;
    } else {
      return post(r);
    }
  }

  public static boolean post(Runnable r) {
    return getMain().post(r);
  }
}
