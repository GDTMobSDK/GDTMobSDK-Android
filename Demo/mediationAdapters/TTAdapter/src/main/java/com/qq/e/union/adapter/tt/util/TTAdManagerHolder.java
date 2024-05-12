package com.qq.e.union.adapter.tt.util;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
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
        if (Thread.currentThread() != Looper.getMainLooper().getThread()) {
          new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
              initAndStart(context, appId);
            }
          });
        } else {
          initAndStart(context, appId);
        }
        // 清理 7 天以上部分文件内容
        // DeleteLruApkUtils.deleteApkFile(context);
      }
    }
  }

  private static void initAndStart(Context context,String appId){
    /**
     *  * V>=56XX
     * 穿山甲SDK初始化API：该API必须在主线程中调用，穿山甲会将初始化操作放在子线程执行。
     * TTAdSdk.init仅进行初始化，不会获取个人信息, 如果要展示广告，需要再调用TTAdSdk.start方法
     */
    TTAdSdk.init(context, buildConfig(context, appId));

    /**
     * 穿山甲sdk启动入口，该API必须在主线程中调用，启动操作会在子线程执行，推荐使用该API执行穿山甲启动操作
     */
    TTAdSdk.start(new TTAdSdk.Callback() {
      /**
       * start成功回调
       * 注意：开发者需要在success回调之后再去请求广告
       */
      @Override
      public void success() {
        sInitStatus = InitStatus.INIT_SUCCESS;
        Log.d(TAG, "start success");
        // 初始化之后申请下权限，开发者如果不想申请可以将此处删除
        // TTAdSdk.getAdManager().requestPermissionIfNecessary(context);
        for (InitCallBack initCallBack: sCallBackList) {
          initCallBack.onInitSuccess();
        }
        sCallBackList.clear();
      }

      @Override
      public void fail(int code, String msg) {
        sInitStatus = InitStatus.INIT_FAIL;
        Log.d(TAG, "start fail, code = " + code + "s = " + msg);
        for (InitCallBack initCallBack: sCallBackList) {
          initCallBack.onInitFail();
        }
        sCallBackList.clear();
      }
    });
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
