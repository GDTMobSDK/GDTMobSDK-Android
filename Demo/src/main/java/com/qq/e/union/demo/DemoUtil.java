package com.qq.e.union.demo;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.androidquery.callback.BitmapAjaxCallback;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.comm.managers.status.SDKStatus;

import java.util.HashMap;
import java.util.Map;


/**
 * Created by hechao on 2018/2/8.
 */

public class DemoUtil {

  public static int REPORT_BIDDING_DISABLE = 1;
  public static int REPORT_BIDDING_WIN = 0;
  public static int REPORT_BIDDING_LOSS = 1;
  private static int sIsReportBiddingLoss = -1;
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

  public static int isReportBiddingLoss() {
    return sIsReportBiddingLoss;
  }

  public static void setIsReportBiddingLoss(int isReportBiddingLoss) {
    sIsReportBiddingLoss = isReportBiddingLoss;
  }

  public static void setNeedSetBidECPM(boolean need) {
    sNeedSetBidECPM = need;
  }

  public static boolean isNeedSetBidECPM() {
    return sNeedSetBidECPM;
  }

  @NonNull
  static LoadAdParams getLoadAdParams(String value) {
    Map<String, String> info = new HashMap<>();
    info.put("custom_key", value);
    LoadAdParams loadAdParams = new LoadAdParams();
    loadAdParams.setDevExtra(info);
    return loadAdParams;
  }
}
