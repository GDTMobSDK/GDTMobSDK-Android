package com.qq.e.union.adapter.bd.splash;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.mobads.sdk.api.RequestParameters;
import com.baidu.mobads.sdk.api.SplashAd;
import com.baidu.mobads.sdk.api.SplashInteractionListener;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.mediation.interfaces.BaseSplashAd;
import com.qq.e.union.adapter.bd.util.BDAdManager;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * 百度开屏广告适配器
 * 作用：封装百度开屏广告，适配优量汇开屏广告
 */
public class BDSplashAdAdapter extends BaseSplashAd {

  private static final String KEY_CAN_SPLASH_CLICK = "canSplashClick";
  private static final String TAG = BDSplashAdAdapter.class.getSimpleName();

  private final Context context;
  // 广告位id
  private final String posId;
  private SplashAd splashAd;
  private ADListener adListener;
  // 实例化开屏广告对象,建议为true，否则影响填充
  private boolean canSplashClick = true;
  // 广告有效时间默认 30 分钟，开发可自行设定，但最大有效时间不能超过当前 SDK 指定有效时间(具体请参考百度广告 SDK 官方文档)
  private long mExpireTimestamp;
  private boolean finished;
  // 开屏广告的容器
  private final Handler mainHandler;

  /**
   * @param context ⚠️ 注意：如果使用百青藤的开屏，context 不能传 ApplicationContext，否则会导致拉取广告失败。
   */
  public BDSplashAdAdapter(Context context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    this.context = context;
    this.posId = posId;
    BDAdManager.init(context, appId);
    try {
      JSONObject jsonObject = new JSONObject(ext);
      canSplashClick = jsonObject.optBoolean(KEY_CAN_SPLASH_CLICK);
    } catch (JSONException e) {
      e.printStackTrace();
    }
    mainHandler = new Handler(Looper.getMainLooper());
  }

  @Override
  public void setADListener(ADListener listener) {
    this.adListener = listener;
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
    fireAdEvent(AdEventType.AD_CLOSED);
  }

  private void onADFailed(int errorCode, String errorMessage) {
    synchronized (this) {
      if (finished) {
        return;
      }
      finished = true;
    }
    fireAdEvent(AdEventType.NO_AD, new Object[]{errorCode}, ErrorCode.DEFAULT_ERROR_CODE, errorMessage);
  }

  private void onADLoaded() {
    synchronized (this) {
      if (finished) {
        return;
      }
    }
    mExpireTimestamp = SystemClock.elapsedRealtime() + 30 * DateUtils.MINUTE_IN_MILLIS;
    fireAdEvent(AdEventType.AD_LOADED, new Object[]{mExpireTimestamp});
  }

  @Override
  public void fetchAdOnly() {
    SplashInteractionListener listener = new SplashInteractionListener() {
      @Override
      public void onADLoaded() {
        Log.d(TAG, "onADLoaded");
        BDSplashAdAdapter.this.onADLoaded();
      }

      @Override
      public void onAdDismissed() {
        Log.d(TAG, "onAdDismissed");
        onADFinished();
      }

      @Override
      public void onAdFailed(String arg0) {
        Log.d(TAG, "onAdFailed: " + arg0);
        BDSplashAdAdapter.this.onADFailed(ErrorCode.NO_AD_FILL, arg0);
      }

      @Override
      public void onLpClosed() {
        Log.d(TAG, "onLpClosed: ");
      }

      @Override
      public void onAdPresent() {
        Log.d(TAG, "onAdPresent");
        if (!finished) {
          fireAdEvent(AdEventType.AD_EXPOSED);
          fireAdEvent(AdEventType.AD_SHOW);
        }
      }

      @Override
      public void onAdClick() {
        Log.d(TAG, "onAdClick");
        if (!finished) {
          fireAdEvent(AdEventType.AD_CLICKED);
        }
        onADFinished();
      }

      @Override
      public void onAdCacheSuccess() {
        Log.d(TAG, "onAdCacheSuccess");
      }

      @Override
      public void onAdCacheFailed() {
        Log.d(TAG, "onAdCacheFailed");
      }
    };
    //  设置开屏广告请求参数，图片宽高单位dp 非必选
    final RequestParameters parameters = new RequestParameters.Builder()
        .setHeight(640)
        .setWidth(360)
        .build();
    splashAd = new SplashAd(context, posId, parameters, listener);
    splashAd.load();
  }

  @Override
  public void showAd(ViewGroup container) {
    if (container == null || splashAd == null) {
      Log.e(TAG, "showAd: container or splashAd == null");
      return;
    }
    splashAd.show(container);
  }

  private void fireAdEvent(int eventId, Object... params) {
    mainHandler.post(new Runnable() {
      @Override
      public void run() {
        if (adListener != null) {
          adListener.onADEvent(new ADEvent(eventId, params));
        }
      }
    });
  }

  @Override
  public void setFetchDelay(int fetchDelay) {
    /* 百度暂不支持 */
  }

  /**
   * 百度的跳过按钮是在服务端配置的，SDK 中不能自定义
   */
  @Override
  public void setSkipView(View view) {
    /* 百度暂不支持 */
  }

  @Override
  public void setLoadAdParams(LoadAdParams params) {
    /* 百度暂不支持 */
  }

  @Override
  public boolean isValid() {
    return SystemClock.elapsedRealtime() <= mExpireTimestamp;
  }

  @Override
  public int getECPM() {
    /* 百度暂不支持 */
    return Constant.VALUE_NO_ECPM;
  }

  @Override
  public String getECPMLevel() {
    /* 百度暂不支持 */
    return null;
  }

  @Override
  public String getReqId() {
    return null;
  }

  @Override
  public Map<String, Object> getExtraInfo() {
    return new HashMap<>();
  }

  @Override
  public void setSupportZoomOut(boolean isSupport) {
    /* 百度暂不支持 */
  }

  @Override
  public void zoomOutAnimationFinish() {
    /* 百度暂不支持 */
  }

  @Override
  public Bitmap getZoomOutBitmap() {
    /* 百度暂不支持 */
    return null;
  }

  @Override
  public void fetchFullScreenAdOnly() {
    /* 百度暂无全屏接口，用普通接口代替 */
    fetchAdOnly();
  }

  @Override
  public void showFullScreenAd(ViewGroup container) {
    /* 百度暂无全屏接口，用普通接口代替 */
    showAd(container);
  }

  @Override
  public void setDeveloperLogo(int logoRes) {
    /* 百度暂不支持 */
  }

  @Override
  public void setDeveloperLogo(byte[] logoData) {
    /* 百度暂不支持 */
  }

  @Override
  public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
    /* 百度暂不支持 */
  }
}
