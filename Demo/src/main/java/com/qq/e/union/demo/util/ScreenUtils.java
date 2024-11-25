package com.qq.e.union.demo.util;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.view.WindowManager;


import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

public class ScreenUtils {

  public static void toggleOrientation(Activity activity) {
    int currentOrientation = activity.getResources().getConfiguration().orientation;
    if (currentOrientation == ORIENTATION_PORTRAIT) {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    } else if (currentOrientation == ORIENTATION_LANDSCAPE) {
      activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
  }

  public static void toggleFullscreen(Activity activity) {
    boolean isFullscreen = (activity.getWindow().getAttributes().flags & WindowManager.LayoutParams.FLAG_FULLSCREEN)
        == WindowManager.LayoutParams.FLAG_FULLSCREEN;
    if (isFullscreen) {
      activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
    } else {
      activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
          WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }
  }
}
