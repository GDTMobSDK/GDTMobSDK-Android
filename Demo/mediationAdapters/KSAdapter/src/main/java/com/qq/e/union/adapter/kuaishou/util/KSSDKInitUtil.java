package com.qq.e.union.adapter.kuaishou.util;

import android.content.Context;
import android.util.Log;

import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.SdkConfig;
import com.qq.e.union.adapter.util.PersonalRecommendUtils;


/**
 * 快手SDK 初始化工具类
 */
public class KSSDKInitUtil {

  private static volatile boolean mIsInit;

  public static void init(Context appContext, String appId) {
    Log.d("KSSDKInitUtil", "init: context: " + appContext + ", appid: " + appId + ", mIsInit: " + mIsInit);
    if (appContext != null && !mIsInit) {
      synchronized (KSSDKInitUtil.class) {
        if (!mIsInit) {
          KsAdSDK.init(appContext.getApplicationContext(),
              new SdkConfig.Builder().appId(appId) // 90009 为快手测试aapId，请联系快手平台申请正式AppId，必填
              .appName("test-android-sdk") // 测试appName，请填写您应用的名称，非必填
//            .appKey(APP_KEY) // 直播sdk安全验证，接入直播模块必填
//            .appWebKey(APP_WB_KEY) // 直播sdk安全验证，接入直播模块必填
              .showNotification(true) // 是否展示下载通知栏
              .debug(true).build());
          mIsInit = true;
          if (PersonalRecommendUtils.sKSState != null) {
            // 个性化推荐广告，请求广告的时候设置,true为启用
            KsAdSDK.setPersonalRecommend(PersonalRecommendUtils.sKSState);
          }
        }
      }
    }
  }
}
