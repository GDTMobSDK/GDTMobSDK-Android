package com.qq.e.union.adapter.mintegral.util;

import android.content.Context;

import com.mintegral.msdk.MIntegralSDK;
import com.mintegral.msdk.out.MIntegralSDKFactory;

import java.util.Map;

public class MTGSDKInitUtil {
  private static volatile boolean mIsInit;

  public static void initSDK(Context context, String appId, String appKey) {

    if (context != null) { // 暂时去掉 mIsInit，如果后面 mtg adapter 使用再重新设计这部分代码
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
