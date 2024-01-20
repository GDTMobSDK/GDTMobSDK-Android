package com.qq.e.union.adapter.tt.splash;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTSplashAd;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.mediation.interfaces.BaseSplashAd;
import com.qq.e.union.adapter.tt.util.TTAdManagerHolder;
import com.qq.e.union.adapter.tt.util.TTLoadAdUtil;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;
import com.qq.e.union.adapter.util.PxUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * 穿山甲开屏广告适配器
 * 作用：封装穿山甲开屏广告，适配优量汇开屏广告
 */
public class TTSplashAdAdapter extends BaseSplashAd implements TTAdManagerHolder.InitCallBack {

  private static final String TAG = TTSplashAdAdapter.class.getSimpleName();

  private static final String KEY_APPID = "appId";
  /**
   * 头条 SDK 要求参数
   */
  // 广告容器的宽度对应于屏幕的最低占比
  private static final double MIN_CONTAINER_WIDTH_RATE = 0.7d;
  // 广告容器的高度对应于屏幕的最低占比
  private static final double MIN_CONTAINER_HEIGHT_RATE = 0.5d;
  /**
   * 头条 SDK 要求参数 end
   */

  // 广告加载超时默认值，开发者自行配置
  private int fetchAdDelay = 3000;
  // 广告默认曝光时间，开发者自行配置
  private int exposureAdDelay = 5000;

  private final String posId;
  private final String mAppId;
  private TTAdNative mTTAdNative;
  private TTSplashAd mTTSplashAd;
  private ADListener adListener;
  private View skipView;        // 开发者传入的跳过按钮
  private ViewGroup container;
  private boolean finished;
  // 广告有效时间默认 30 分钟，开发可自行设定，但最大有效时间不能超过当前 SDK 指定有效时间(具体请参考穿山甲 SDK 官方文档)
  private long mExpireTimestamp;
  private int containerHeight; // 广告容器的高度，单位为px
  private int containerWidth; // 广告容器的宽度，单位px
  private Context context;
  private View splashView; // 开屏广告View
  private int ecpm = Constant.VALUE_NO_ECPM;
  private String requestId;
  private boolean mIsStartDownload;
  private boolean mIsPaused;

  private Handler mainHandler = new Handler(Looper.getMainLooper());

  public TTSplashAdAdapter(Context context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    TTAdManagerHolder.init(context, appId);
    mTTAdNative = TTAdManagerHolder.get().createAdNative(context);
    this.context = context;
    this.posId = posId;
    mAppId = appId;
  }

  @Override
  public void setADListener(ADListener listener) {
    this.adListener = listener;
  }

  @Override
  public void setFetchDelay(int fetchDelay) {
    if (fetchDelay == 0) {
      return;
    }
    if (fetchDelay < 3000) {
      fetchDelay = 3000;
    }
    if (fetchDelay > 5000) {
      fetchDelay = 5000;
    }
    fetchAdDelay = fetchDelay;
  }

  @Override
  public void setSkipView(View view) {
    if (view == null) {
      return;
    }
    skipView = view;
  }

  /**
   * 外部广告，无加载参数
   */
  @Override
  public void setLoadAdParams(LoadAdParams params) { }

  private TTAdNative.SplashAdListener getSplashAdListener() {
    return new TTAdNative.SplashAdListener() {
      @Override
      public void onError(int code, String message) {
        Log.d(TAG, "onError: code: " + code + "message: " + message);
        onADFailed(ErrorCode.NO_AD_FILL, code, message);
      }

      @Override
      public void onTimeout() {
        Log.d(TAG, "onTimeout: ");
        onADFailed(ErrorCode.TIME_OUT, ErrorCode.DEFAULT_ERROR_CODE, ErrorCode.DEFAULT_ERROR_MESSAGE);
      }

      @Override
      public void onSplashAdLoad(TTSplashAd ad) {
        Log.d(TAG, "onSplashAdLoad: ad: " + ad);
        if (ad == null) {
          onADFailed(ErrorCode.NO_AD_FILL, ErrorCode.DEFAULT_ERROR_CODE, ErrorCode.DEFAULT_ERROR_MESSAGE);
          return;
        }
        mTTSplashAd = ad;
        try {
          ecpm = (int) ad.getMediaExtraInfo().get("price");
        } catch (Exception e) {
          Log.d(TAG, "get ecpm error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: ecpm = " + ecpm);
        try {
          Object o = ad.getMediaExtraInfo().get("request_id");
          if (o != null) {
            requestId = o.toString();
          }
        } catch (Exception e) {
          Log.d(TAG, "get request_id error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: requestId = " + requestId);
        if (adListener != null) {
          mExpireTimestamp = SystemClock.elapsedRealtime() + 30 * DateUtils.MINUTE_IN_MILLIS;
          adListener.onADEvent(new ADEvent(AdEventType.AD_LOADED, new Object[]{mExpireTimestamp}));
        }
        // 获取SplashView
       splashView = ad.getSplashView();
        // 自定义「跳过」按钮，关闭开屏广告倒计时功能以及不显示跳过按钮
        if (skipView != null) {
          ad.setNotAllowSdkCountdown();
        }
        // 设置SplashView的交互监听器
        ad.setSplashInteractionListener(getInteractionListener(ad));
        if (isAppAd(ad)) {
          ad.setDownloadListener(new TTAppDownloadListener() {
            @Override
            public void onIdle() {
              mIsStartDownload = false;
            }

            @Override
            public void onDownloadActive(long totalBytes, long currBytes, String fileName,
                                         String appName) {
              Log.d(TAG, "onDownloadActive==totalBytes=" + totalBytes + ",currBytes=" + currBytes +
                  ",fileName=" + fileName + ",appName=" + appName);

              if (!mIsStartDownload) {
                mIsStartDownload = true;
                fireAdEvent(AdEventType.ADAPTER_APK_DOWNLOAD_START, appName);
              }

              if (mIsPaused) {
                mIsPaused = false;
                fireAdEvent(AdEventType.ADAPTER_APK_DOWNLOAD_RESUME, appName);
              }
            }

            @Override
            public void onDownloadPaused(long totalBytes, long currBytes, String fileName,
                                         String appName) {
              Log.d(TAG, "onDownloadPaused===totalBytes=" + totalBytes + ",currBytes=" + currBytes +
                  ",fileName=" + fileName + ",appName=" + appName);
              mIsPaused = true;
              fireAdEvent(AdEventType.ADAPTER_APK_DOWNLOAD_PAUSE, appName);
            }

            @Override
            public void onDownloadFailed(long totalBytes, long currBytes, String fileName,
                                         String appName) {
              Log.d(TAG, "onDownloadFailed==totalBytes=" + totalBytes + ",currBytes=" + currBytes +
                  ",fileName=" + fileName + ",appName=" + appName);
              fireAdEvent(AdEventType.ADAPTER_APK_DOWNLOAD_FAIL, appName);
            }

            @Override
            public void onDownloadFinished(long totalBytes, String fileName, String appName) {
              Log.d(TAG, "onDownloadFinished==totalBytes=" + totalBytes + ",fileName=" + fileName +
                  ",appName=" + appName);
              fireAdEvent(AdEventType.ADAPTER_APK_DOWNLOAD_FINISH, appName);
            }

            @Override
            public void onInstalled(String fileName, String appName) {
              Log.d(TAG, "onInstalled==" + ",fileName=" + fileName + ",appName=" + appName);
              fireAdEvent(AdEventType.ADAPTER_APK_INSTALLED, appName);
            }
          });
        }
      }

      private TTSplashAd.AdInteractionListener getInteractionListener(TTSplashAd ad) {
        return new TTSplashAd.AdInteractionListener() {
          @Override
          public void onAdClicked(View view, int type) {
            Log.d(TAG, "onAdClicked: type: " + type);
            if (!finished && adListener != null) {
              adListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
              if (isAppAd(ad)) {
                adListener.onADEvent(new ADEvent(AdEventType.APP_AD_CLICKED));
              }
            }
            if (type == TTAdConstant.INTERACTION_TYPE_BROWSER || type == TTAdConstant.INTERACTION_TYPE_LANDING_PAGE || type == TTAdConstant.INTERACTION_TYPE_DIAL) {
              mainHandler.postDelayed(TTSplashAdAdapter.this::onADFinished, 1000);
            }
          }

          @Override
          public void onAdShow(View view, int type) {
            Log.d(TAG, "onAdShow: type: " + type);
            if (!finished && adListener != null) {
              adListener.onADEvent(new ADEvent(AdEventType.AD_SHOW));
              adListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED));
            }
            // 自定义「跳过」按钮，需要自己实现曝光关闭
            if (skipView != null) {
              Runnable onADFinished = TTSplashAdAdapter.this::onADFinished;
              skipView.setOnClickListener(v -> {
                mainHandler.removeCallbacks(onADFinished);
                onADFinished();
              });
              mainHandler.postDelayed(onADFinished, exposureAdDelay);
            }
          }

          @Override
          public void onAdSkip() {
            Log.d(TAG, "onAdSkip: ");
            onADFinished();
          }

          @Override
          public void onAdTimeOver() {
            Log.d(TAG, "onAdTimeOver: ");
            onADFinished();
          }
        };
      }
    };
  }

  /**
   * 关闭开屏广告并通知开发者
   * 回调曝光和关闭
   */
  private void onADFinished() {
    synchronized (this) {
      if (finished) {
        return;
      }
      finished = true;
    }
    if (adListener != null) {
      adListener.onADEvent(new ADEvent(AdEventType.AD_CLOSED));
    }
  }

  private void onADFailed(final int errCode, Integer onlineErrorCode, String errorMessage) {
    synchronized (this) {
      if (finished) {
        return;
      }
      finished = true;
    }
    if (adListener != null) {
      adListener.onADEvent(new ADEvent(AdEventType.NO_AD, new Object[]{errCode}, onlineErrorCode, errorMessage));
    }
  }

  /**
   * 穿山甲开屏容器宽高在并行加载时,默认屏幕大小。
   */
  @Override
  public void fetchAdOnly() {
    TTLoadAdUtil.load(this);
  }

  private void fetchAdAfterInitSuccess(){
    AdSlot adSlot = new AdSlot.Builder()
        .setCodeId(posId)
        .setSupportDeepLink(true)
        .setImageAcceptedSize(PxUtils.getDeviceWidthInPixel(context), PxUtils.getDeviceHeightInPixel(context))
        .build();
    mTTAdNative.loadSplashAd(adSlot, getSplashAdListener(), fetchAdDelay);
  }

  @Override
  public void showAd(ViewGroup container) {
      if (container != null && splashView != null){
        container.removeAllViews();
        container.addView(splashView);
      }
  }

  @Override
  public boolean isValid() {
    return SystemClock.elapsedRealtime() <= mExpireTimestamp;
  }

  @Override
  public int getECPM() {
    return ecpm;
  }

  @Override
  public String getECPMLevel() {
    return null;
  }

  @Override
  public String getReqId() {
    return requestId;
  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {
    super.sendLossNotification(price, reason, adnId);
    if (mTTSplashAd != null) {
      mTTSplashAd.loss((double) price, String.valueOf(reason), adnId);
    }
  }

  @Override
  public void sendWinNotification(int price) {
    super.sendWinNotification(price);
    if (mTTSplashAd != null) {
      mTTSplashAd.win((double) price);
    }
  }

  @Override
  public void setBidECPM(int price) {
    super.setBidECPM(price);
  }

  @Override
  public Map<String, Object> getExtraInfo() {
    Map<String, Object> map = new HashMap<>();
    map.put("request_id", getReqId());
    return map;
  }

  @Override
  public void setSupportZoomOut(boolean isSupport) {
    // 穿山甲不支持此接口
  }

  @Override
  public void zoomOutAnimationFinish() {
    // 穿山甲不支持此接口
  }

  @Override
  public Bitmap getZoomOutBitmap() {
    return null; // 穿山甲不支持此接口
  }

  @Override
  public void fetchFullScreenAdOnly() {
    // 穿山甲暂无全屏接口，用普通接口代替
    fetchAdOnly();
  }

  @Override
  public void showFullScreenAd(ViewGroup container) {
    // 穿山甲暂无全屏接口，用普通接口代替
    showAd(container);
  }

  @Override
  public void setDeveloperLogo(int logoRes) {
    /* 穿山甲暂不支持 */
  }

  @Override
  public void setDeveloperLogo(byte[] logoData) {
    /* 穿山甲暂不支持 */
  }

  @Override
  public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
    /* 穿山甲暂不支持 */
  }

  @Override
  public void onInitSuccess() {
    fetchAdAfterInitSuccess();
  }

  @Override
  public void onInitFail() {
    Log.i(TAG, "穿山甲 SDK 初始化失败，无法加载广告");
    onADFailed(ErrorCode.NO_AD_FILL, ErrorCode.DEFAULT_ERROR_CODE, ErrorCode.DEFAULT_ERROR_MESSAGE);
  }

  private void fireAdEvent(int adEventType, String appName) {
    if (adListener != null) {
      adListener.onADEvent(new ADEvent(adEventType, posId, mAppId, getReqId(), appName));
    }
  }

  private boolean isAppAd(TTSplashAd ad){
    if (ad != null && ad.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD) {
      return true;
    }
    return false;
  }
}