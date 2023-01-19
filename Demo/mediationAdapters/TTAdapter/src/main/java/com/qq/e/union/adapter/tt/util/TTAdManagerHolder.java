package com.qq.e.union.adapter.tt.util;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdManager;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.qq.e.union.adapter.util.PersonalRecommendUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 可以用一个单例来保存TTAdManager实例，在需要初始化sdk的时候调用
 */
public class TTAdManagerHolder {

  private final static String TAG = TTAdManagerHolder.class.getSimpleName();

  private static volatile InitStatus sInitStatus = InitStatus.UN_INIT;
  private static List<InitCallBack> sCallBackList = new ArrayList<>();

  public enum InitStatus{
    UN_INIT,
    INITIALIZING,
    INIT_SUCCESS,
    INIT_FAIL,
  }

  public static TTAdManager get() {
    return TTAdSdk.getAdManager();
  }

  public static void init(Context context, String appId) {
    Log.d(TAG, "init: context: " + context + ", appId: " + appId + ", sInit: " + sInitStatus);
    if (sInitStatus.equals(InitStatus.INIT_SUCCESS)) {
      return;
    }
    synchronized (TTAdManagerHolder.class) {
      if (sInitStatus.equals(InitStatus.UN_INIT)) {
          sInitStatus = InitStatus.INITIALIZING;
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
            sInitStatus = InitStatus.INIT_SUCCESS;
            Log.d(TAG, "init success");
            // 初始化之后申请下权限，开发者如果不想申请可以将此处删除
            // TTAdSdk.getAdManager().requestPermissionIfNecessary(context);
            for (InitCallBack initCallBack: sCallBackList) {
              initCallBack.onInitSuccess();
            }
            sCallBackList.clear();
          }

          /**
           * @param code 初始化失败回调错误码
           * @param msg 初始化失败回调信息
           */
          @Override
          public void fail(int code, String msg) {
            sInitStatus = InitStatus.INIT_FAIL;
            Log.d(TAG, "init fail, code = " + code + "s = " + msg);
            for (InitCallBack initCallBack: sCallBackList) {
              initCallBack.onInitFail();
            }
            sCallBackList.clear();
          }
        });

        // 清理 7 天以上部分文件内容
        // DeleteLruApkUtils.deleteApkFile(context);
      }
    }
  }

  private static TTAdConfig buildConfig(Context context, String appId) {
    TTAdConfig.Builder builder = new TTAdConfig.Builder()
        .appId(appId)
        // 使用TextureView控件播放视频,默认为SurfaceView, 当有SurfaceView冲突的场景，可以使用TextureView。
        .useTextureView(true)
        // TODO 请开发者改成自己的应用名
        .appName("app_name")
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

  public static InitStatus getSdkInitStatus(){
    return sInitStatus;
  }

  public static void registerInitCallback(InitCallBack initCallBack) {
    sCallBackList.add(initCallBack);
  }

  public interface InitCallBack{
    void onInitSuccess();

    void onInitFail();
  }
}
