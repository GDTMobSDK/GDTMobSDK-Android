package com.qq.e.union.adapter.tt.splash;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTSplashAd;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.mediation.interfaces.BaseSplashAd;
import com.qq.e.union.adapter.tt.util.TTAdManagerHolder;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;
import com.qq.e.union.adapter.util.PxUtils;

/**
 * 穿山甲开屏广告适配器
 * 作用：封装穿山甲开屏广告，适配优量汇开屏广告
 */
public class TTSplashAdAdapter extends BaseSplashAd {

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
  private TTAdNative mTTAdNative;
  private ADListener adListener;
  private View skipView;        // 开发者传入的跳过按钮
  private ViewGroup container;
  private boolean finished;
  // 广告有效时间默认 30 分钟，开发可自行设定，但最大有效时间不能超过当前 SDK 指定有效时间(具体请参考穿山甲 SDK 官方文档)
  private long mExpireTimestamp = 30 * 60 * 1000;
  private int containerHeight; // 广告容器的高度，单位为px
  private int containerWidth; // 广告容器的宽度，单位px
  private Context context;
  private View splashView; // 开屏广告View
  private int ecpm = Constant.VALUE_NO_ECPM;

  private Handler mainHandler = new Handler(Looper.getMainLooper());

  public TTSplashAdAdapter(Context context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    TTAdManagerHolder.init(context, appId);
    mTTAdNative = TTAdManagerHolder.get().createAdNative(context);
    this.context = context;
    this.posId = posId;
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
        onADFailed(ErrorCode.NO_AD_FILL);
      }

      @Override
      public void onTimeout() {
        Log.d(TAG, "onTimeout: ");
        onADFailed(ErrorCode.TIME_OUT);
      }

      @Override
      public void onSplashAdLoad(TTSplashAd ad) {
        Log.d(TAG, "onSplashAdLoad: ad: " + ad);
        if (ad == null) {
          onADFailed(ErrorCode.NO_AD_FILL);
          return;
        }
        if (adListener != null) {
          adListener.onADEvent(new ADEvent(EVENT_TYPE_AD_LOADED,
              new Object[]{SystemClock.elapsedRealtime() + mExpireTimestamp}));
        }
        try {
          ecpm = (int) ad.getMediaExtraInfo().get("price");
        } catch (Exception e) {
          Log.e(TAG, "get ecpm error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: ecpm = " + ecpm);
        // 获取SplashView
       splashView = ad.getSplashView();
        // 自定义「跳过」按钮，关闭开屏广告倒计时功能以及不显示跳过按钮
        if (skipView != null) {
          ad.setNotAllowSdkCountdown();
        }
        // 设置SplashView的交互监听器
        ad.setSplashInteractionListener(getInteractionListener());
      }

      private TTSplashAd.AdInteractionListener getInteractionListener() {
        return new TTSplashAd.AdInteractionListener() {
          @Override
          public void onAdClicked(View view, int type) {
            Log.d(TAG, "onAdClicked: type: " + type);
            if (!finished && adListener != null) {
              adListener.onADEvent(new ADEvent(EVENT_TYPE_AD_CLICKED));
            }
            if (type == TTAdConstant.INTERACTION_TYPE_BROWSER || type == TTAdConstant.INTERACTION_TYPE_LANDING_PAGE || type == TTAdConstant.INTERACTION_TYPE_DIAL) {
              mainHandler.postDelayed(TTSplashAdAdapter.this::onADFinished, 1000);
            }
          }

          @Override
          public void onAdShow(View view, int type) {
            Log.d(TAG, "onAdShow: type: " + type);
            if (!finished && adListener != null) {
              adListener.onADEvent(new ADEvent(EVENT_TYPE_AD_PRESENT));
              adListener.onADEvent(new ADEvent(EVENT_TYPE_AD_EXPOSURE));
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
      adListener.onADEvent(new ADEvent(EVENT_TYPE_AD_DISMISSED));
    }
  }

  private void onADFailed(final int errCode) {
    synchronized (this) {
      if (finished) {
        return;
      }
      finished = true;
    }
    if (adListener != null) {
      adListener.onADEvent(new ADEvent(EVENT_TYPE_NO_AD, new Object[]{errCode}));
    }
  }

  /**
   * 穿山甲开屏容器宽高在并行加载时,默认屏幕大小。
   */
  @Override
  public void fetchAdOnly() {
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
  public int getECPM() {
    return ecpm;
  }

  @Override
  public String getECPMLevel() {
    return null;
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
}