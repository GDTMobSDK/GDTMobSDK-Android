package com.qq.e.union.adapter.util;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import androidx.annotation.Nullable;

public class ContextUtils {
  @Nullable
  public static Activity getActivity(Context context) {
    if (!(context instanceof ContextWrapper)) {
      return null;
    }
    if (context instanceof Activity) {
      return (Activity) context;
    }
    return getActivity(((ContextWrapper) context).getBaseContext());
  }
}
