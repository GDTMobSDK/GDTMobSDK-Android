package com.qq.e.union.demo.util;

import android.content.Context;
import android.content.res.Resources;
import android.util.TypedValue;

public class PxUtil {

  public static int dpToPx(Context context, int dp) {
    Resources r = context.getApplicationContext().getResources();
    float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    return (int) px;
  }

  public static int pxToDp(Context context, int px) {
    float scale = context.getApplicationContext().getResources().getDisplayMetrics().density;
    return (int) (px / scale + 0.5f);
  }

  public static float dpToPx(Context context, float dp) {
    Resources r = context.getApplicationContext().getResources();
    float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, r.getDisplayMetrics());
    return px;
  }

  public static float pxToDp(Context context, float px) {
    float scale = context.getApplicationContext().getResources().getDisplayMetrics().density;
    return (px / scale + 0.5f);
  }


}
