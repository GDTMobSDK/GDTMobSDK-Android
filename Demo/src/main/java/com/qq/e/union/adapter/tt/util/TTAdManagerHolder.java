package com.qq.e.union.adapter.tt.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.qq.e.union.adapter.util.PersonalRecommendUtils;

/**
 * 可以用一个单例来保存TTAdManager实例，在需要初始化sdk的时候调用
 */
public class TTAdManagerHolder {

  private final static String TAG = TTAdManagerHolder.class.getSimpleName();

  private static volatile boolean sInit;

  public static TTAdManager get() {
    if (!sInit) {
      throw new RuntimeException("TTAdSdk is not init, please check.");
    }
    return TTAdSdk.getAdManager();
  }

  public static void init(Context context, String appId) {
    Log.d(TAG, "init: context: " + context + ", appId: " + appId + ", sInit: " + sInit);
    if (sInit) {
      return;
    }
    synchronized (TTAdManagerHolder.class) {
      if (!sInit) {

        // 穿山甲在3450版本对SDK的初始化方法进行了较大的改动，支持了同步初始化和异步初始化两种方式
        // 若您接入的是穿山甲Pro版本的SDK，则只能使用异步初始化的方式。同时混淆规则也要同步调整

        // 异步初始化
        TTAdSdk.init(context, buildConfig(context, appId), new TTAdSdk.InitCallback() {
          /**
           * 初始化成功回调
           * 注意：开发者需要在success回调之后再去请求广告
           */
          @Override
          public void success() {
            sInit = true;
            Log.d(TAG, "init success");
          }

          /**
           * @param code 初始化失败回调错误码
           * @param msg 初始化失败回调信息
           */
          @Override
          public void fail(int code, String msg) {
            Log.d(TAG, "init fail, code = " + code + "s = " + msg);
          }
        });
      }
    }
  }

  private static TTAdConfig buildConfig(Context context, String appId) {
    TTAdConfig.Builder builder = new TTAdConfig.Builder()
        .appId(appId)
        // 使用TextureView控件播放视频,默认为SurfaceView, 当有SurfaceView冲突的场景，可以使用TextureView。
        .useTextureView(true)
        // TODO 请开发者改成自己的应用名
        .appName("优量汇")
        .titleBarTheme(TTAdConstant.TITLE_BAR_THEME_DARK)
        // 是否允许sdk展示通知栏提示
        .allowShowNotify(true)
        // 是否在锁屏场景支持展示广告落地页
        .allowShowPageWhenScreenLock(true)
        // 测试阶段打开，可以通过日志排查问题，上线时去除该调用
        .debug(true)
        // 允许直接下载的网络状态集合
        .directDownloadNetworkType(TTAdConstant.NETWORK_STATE_WIFI, TTAdConstant.NETWORK_STATE_3G)
        .supportMultiProcess(false);

    if (!TextUtils.isEmpty(PersonalRecommendUtils.sTTState)) {
      // 个性化推荐广告设置
      builder.data("[{\"name\":\"" + PersonalRecommendUtils.sTTKey + "\",\"value\":\"" + PersonalRecommendUtils.sTTState + "\"}]");
    }
    return builder.build();
  }
}
