package com.qq.e.union.adapter.kuaishou.splash;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsSplashScreenAd;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.mediation.interfaces.BaseSplashAd;
import com.qq.e.union.adapter.kuaishou.util.KSSDKInitUtil;
import com.qq.e.union.adapter.util.Constant;

/**
 * 快手联盟开屏广告适配器
 */
public class KSSplashAdAdapter extends BaseSplashAd {
  private static final String TAG = KSSplashAdAdapter.class.getSimpleName();
  private final String mPosId;
  private KsSplashScreenAd mSplashScreenAd;
  private ADListener mADListener;
  private final long mExpireTimestamp = 30 * 60 * 1000; // 广告有效时间默认 30 分钟，开发可自行设定

  public KSSplashAdAdapter(Context context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    KSSDKInitUtil.init(context, appId);
    mPosId = posId;
  }

  @Override
  public void setADListener(ADListener listener) {
    mADListener = listener;
  }

  @Override
  public void setFetchDelay(int fetchDelay) { /* 快手暂不支持 */}

  @Override
  public void setSkipView(View view) { /* 快手暂不支持 */}

  @Override
  public void setLoadAdParams(LoadAdParams params) { /* 快手暂不支持 */}

  @Override
  public void fetchAdOnly() {
    long posId = 0;
    try {
       posId = Long.parseLong(mPosId);
    } catch (Exception e) {
      e.printStackTrace();
      if (mADListener != null) {
        mADListener.onADEvent(new ADEvent(EVENT_TYPE_NO_AD, new Object[]{"广告位id错误"}));
      }
      return;
    }
    KsScene scene =
        new KsScene.Builder(posId).build(); // 此为测试posId，请联系快手平台申请正式posId
    KsAdSDK.getLoadManager().loadSplashScreenAd(scene, new KsLoadManager.SplashScreenAdListener() {
      @Override
      public void onError(int code, String msg) {
        Log.d(TAG, "开屏广告请求失败" + code + msg);
        if (mADListener != null) {
          mADListener.onADEvent(new ADEvent(EVENT_TYPE_NO_AD, new Object[]{code}));
        }
      }

      @Override
      public void onRequestResult(int adNumber) {
        Log.d(TAG, "开屏广告请求填充个数: " + adNumber);
      }

      @Override
      public void onSplashScreenAdLoad(@NonNull KsSplashScreenAd splashScreenAd) {
        Log.d(TAG, "开始数据返回成功");
        mSplashScreenAd = splashScreenAd;
        if (mADListener != null) {
          mADListener.onADEvent(new ADEvent(EVENT_TYPE_AD_LOADED,
              new Object[]{SystemClock.elapsedRealtime() + mExpireTimestamp}));
        }
      }
    });
  }

  @Override
  public void showAd(ViewGroup container) {
    if (container != null && mSplashScreenAd != null) {
      container.removeAllViews();
      View view = mSplashScreenAd.getView(container.getContext(),
          new KsSplashScreenAd.SplashScreenAdInteractionListener() {
            @Override
            public void onAdClicked() {
              Log.d(TAG, "开屏广告点击");
              //onAdClick 会吊起h5或者应用商店。 不直接跳转，等返回后再跳转。
              if (mADListener != null) {
                mADListener.onADEvent(new ADEvent(EVENT_TYPE_AD_CLICKED));
              }
            }

            @Override
            public void onAdShowError(int code, String extra) {
              Log.d(TAG, "开屏广告显示错误 " + code + " extra " + extra);
            }

            @Override
            public void onAdShowEnd() {
              Log.d(TAG, "开屏广告显示结束");
              if (mADListener != null) {
                mADListener.onADEvent(new ADEvent(EVENT_TYPE_AD_DISMISSED));
              }
            }

            @Override
            public void onAdShowStart() {
              Log.d(TAG, "开屏广告显示开始");
              if (mADListener != null) {
                mADListener.onADEvent(new ADEvent(EVENT_TYPE_AD_EXPOSURE));
                mADListener.onADEvent(new ADEvent(EVENT_TYPE_AD_PRESENT));
              }
            }

            @Override
            public void onSkippedAd() {
              Log.d(TAG, "用户跳过开屏广告");
            }
          });
      container.addView(view);
    }
  }

  /**
   * 需要统一单位为分
   */
  @Override
  public int getECPM() {
    if (mSplashScreenAd != null) {
      mSplashScreenAd.getECPM();
    }
    return Constant.VALUE_NO_ECPM;
  }

  @Override
  public String getECPMLevel() {
    return null; /* 快手暂不支持 */
  }

  @Override
  public void setSupportZoomOut(boolean isSupport) {
    /* 快手暂不支持 */
  }

  @Override
  public void zoomOutAnimationFinish() {
    /* 快手暂不支持 */
  }

  @Override
  public Bitmap getZoomOutBitmap() {
    return null; /* 快手暂不支持 */
  }

  @Override
  public void fetchFullScreenAdOnly() {
    // 快手暂无全屏接口，用普通接口代替
    fetchAdOnly();
  }

  @Override
  public void showFullScreenAd(ViewGroup container) {
    // 快手暂无全屏接口，用普通接口代替
    showAd(container);
  }

  @Override
  public void setDeveloperLogo(int logoRes) {
    /* 快手暂不支持 */
  }

  @Override
  public void setDeveloperLogo(byte[] logoData) {
    /* 快手暂不支持 */
  }
}
