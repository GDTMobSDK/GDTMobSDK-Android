package com.qq.e.union.demo.view;

import android.content.Context;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

public class ViewUtils {

  public static void removeFromParent(View view) {
    if (view != null) {
      ViewParent vp = view.getParent();
      if (vp instanceof ViewGroup) {
        ((ViewGroup) vp).removeView(view);
      }
    }
  }
}
