package com.qq.e.union.demo;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.androidquery.callback.BitmapAjaxCallback;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.comm.managers.status.SDKStatus;
import com.qq.e.union.demo.util.ToastUtil;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by hechao on 2018/2/8.
 */

public class DemoUtil {

  private static boolean sNeedSetBidECPM = false;

  public static final void hideSoftInput(Activity activity) {
    InputMethodManager imm =
        (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
    View focusView = activity.getCurrentFocus();
    if (focusView != null) {
      imm.hideSoftInputFromWindow(focusView.getWindowToken(), 0); //强制隐藏键盘
    }
  }

  public static void setAQueryImageUserAgent(){
    BitmapAjaxCallback.setAgent("GDTMobSDK-AQuery-"+ SDKStatus.getIntegrationSDKVersion());
  }

  public static void setNeedSetBidECPM(boolean need) {
    sNeedSetBidECPM = need;
  }

  public static boolean isNeedSetBidECPM() {
    return sNeedSetBidECPM;
  }

  @NonNull
  public static LoadAdParams getLoadAdParams(String value) {
    Map<String, String> info = new HashMap<>();
    info.put("custom_key", value);
    info.put("staIn", "com.qq.e.demo");
    info.put("thrmei", "29232329");
    LoadAdParams loadAdParams = new LoadAdParams();
    loadAdParams.setDevExtra(info);
    return loadAdParams;
  }

  public static boolean isAdValid(boolean loadSuccess, boolean isValid, boolean showAd) {
    if (!loadSuccess) {
      ToastUtil.l("请加载广告成功后再进行校验 ！ ");
    } else {
      if (!showAd || !isValid) {
        ToastUtil.l("广告" + (isValid ? "有效" : "无效"));
      }
    }
    return isValid;
  }

  public static int getOrientation(int orientation) {
    switch (orientation) {
      case 0:
      case 2:
        return 1;
      case 1:
      case 3:
        return 0;
    }
    return 1;
  }

}
