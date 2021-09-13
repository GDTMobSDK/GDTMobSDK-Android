package com.qq.e.union.adapter.mintegral.util;

import android.content.Context;

import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.out.MIntegralSDKFactory;

import java.util.Map;

public class MTGSDKInitUtil {
  private static boolean mIsInit;

  public static void initSDK(Context context, String appId, String appKey) {

    if (context != null && !mIsInit) {
      synchronized (MTGSDKInitUtil.class) {
        if (!mIsInit) {
          // mintegral 初始化
          MIntegralSDK sdk = MIntegralSDKFactory.getMIntegralSDK();
          Map<String, String> map = sdk.getMTGConfigurationMap(appId, appKey);
          sdk.init(map, context);
          mIsInit = true;
        }
      }
    }
  }
}
