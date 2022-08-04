package com.qq.e.union.adapter.bd.nativ;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.baidu.mobads.sdk.api.ExpressResponse;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.ads.nativ.NativeExpressADView;
import com.qq.e.ads.nativ.NativeExpressMediaListener;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADEventListener;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.comm.compliance.DownloadConfirmCallBack;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.listeners.NegativeFeedbackListener;
import com.qq.e.comm.pi.AdData;
import com.qq.e.union.adapter.util.Constant;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

public class BDNativeExpressAdDataAdapter extends NativeExpressADView implements ADEventListener {
  private static final String TAG = BDNativeExpressAdDataAdapter.class.getSimpleName();

  private static final int TOP_IMAGE_TEXT = 28; // 上图下文
  private static final int TOP_TEXT_IMAGE = 29; // 上文下图
  private static final int LOGO_IMAGE = 30; // 大图logo
  private static final int LEFT_IMAGE_TEXT = 33; //左图右文
  private static final int RIGHT_IMAGE_TEXT = 34; // 右图左文
  private static final int THREE_IMAGE = 35; // 三图图文
  private static final int THREE_IMAGE_WITH_LOGO = 36; //三图图文+logo
  private static final int VIDEO = 37; // 视频


  private ExpressResponse mExpressResponse;
  private final AdData mAdData;
  private ADListener mListener;
  private WeakReference<Context> mWeakReference;
  private String mEcpmLevel;

  public BDNativeExpressAdDataAdapter(Context context, ExpressResponse data) {
    super(context);
    this.mExpressResponse = data;
    mWeakReference = new WeakReference(context);
    mAdData = new AdData() {
      @Override
      public String getTitle() {
        return null;
      }

      @Override
      public String getDesc() {
        return null;
      }

      /**
       *  百青藤样式无法与优量汇样式完全匹配，需要开发者注意样式的适配。
       */
      @Override
      public int getAdPatternType() {
        switch (mExpressResponse.getStyleType()) {
          case TOP_IMAGE_TEXT:
          case TOP_TEXT_IMAGE:
          case LOGO_IMAGE:
          case LEFT_IMAGE_TEXT:
          case RIGHT_IMAGE_TEXT:
            return AdPatternType.NATIVE_2IMAGE_2TEXT;
          case THREE_IMAGE:
          case THREE_IMAGE_WITH_LOGO:
            return AdPatternType.NATIVE_3IMAGE;
          case VIDEO:
            return AdPatternType.NATIVE_VIDEO;
          default:
            return AdPatternType.NATIVE_1IMAGE_2TEXT;
        }
      }

      @Override
      public int getECPM() {
        return BDNativeExpressAdDataAdapter.this.getECPM();
      }

      @Override
      public String getECPMLevel() {
        return mExpressResponse.getECPMLevel();
      }

      @Override
      public void setECPMLevel(String level) {
        mEcpmLevel = level;
      }

      @Override
      public Map<String, Object> getExtraInfo() {
        return new HashMap<>();
      }

      @Override
      public String getProperty(String property) {
        return null;
      }

      @Override
      public <T> T getProperty(Class<T> type) {
        return null;
      }

      @Override
      public boolean equalsAdData(AdData adData) {
        return false;
      }

      @Override
      public int getVideoDuration() {
        return 0;
      }
    };
    bindDislike();
    tryBindInteractionAdListener();
    tryBindAdPrivacyListener();
  }

  private void tryBindAdPrivacyListener() {
    if (mExpressResponse == null) {
      return;
    }
    mExpressResponse.setAdPrivacyListener(new ExpressResponse.ExpressAdDownloadWindowListener() {
      @Override
      public void onADPrivacyClick() {
        Log.i(TAG, "onADPrivacyClick");
      }

      @Override
      public void onADPermissionShow() {
        Log.i(TAG, "onADPermissionShow");
      }

      @Override
      public void onADPermissionClose() {
        Log.i(TAG, "onADPermissionClose");
      }

      @Override
      public void adDownloadWindowShow() {
        Log.i(TAG, "AdDownloadWindowShow");
      }

      @Override
      public void adDownloadWindowClose() {
        Log.i(TAG, "adDownloadWindowClose");
      }
    });
  }

  private void tryBindInteractionAdListener() {
    if (mExpressResponse == null) {
      return;
    }
    mExpressResponse.setInteractionListener(new ExpressResponse.ExpressInteractionListener() {
      @Override
      public void onAdClick() {
        Log.d(TAG, "onAdClicked");
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED,
            new Object[]{BDNativeExpressAdDataAdapter.this, ""}));
      }

      @Override
      public void onAdExposed() {
        Log.d(TAG, "onAdExposed");
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED,
            new Object[]{BDNativeExpressAdDataAdapter.this}));
      }

      @Override
      public void onAdRenderFail(View view, String s, int i) {
        Log.i(TAG, "onAdRenderFail");
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.AD_RENDER_FAILED,
            new Object[]{BDNativeExpressAdDataAdapter.this}));
      }

      @Override
      public void onAdRenderSuccess(View view, float width, float height) {
        Log.i(TAG, "onAdRenderSuccess");
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.AD_RENDER_SUCCESS,
            new Object[]{BDNativeExpressAdDataAdapter.this}));
        post(() -> {
          /**
           * ===【 注意 】===
           * 1. 展示前需要绑定当前activity，否则负反馈弹框无法弹出（负反馈无响应）
           * 2. 如果你配置了{@link com.baidu.mobads.sdk.api.BDAdConfig.Builder#useActivityDialog(Boolean)}为 false
           *    那么请务必在展现前调用该方法绑定activity，否则会使下载弹框无法弹出（下载类无响应）
           */
          if (mWeakReference.get() != null && mWeakReference.get() instanceof Activity) {
            mExpressResponse.bindInteractionActivity((Activity) mWeakReference.get());
          }
          addView(mExpressResponse.getExpressAdView());
        });
      }

      @Override
      public void onAdUnionClick() {
        Log.d(TAG, "onAdUnionClicked");
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED, new Object[]{this}));
      }
    });
  }

  private void bindDislike() {
    if (mExpressResponse == null) {
      return;
    }
    mExpressResponse.setAdDislikeListener(new ExpressResponse.ExpressDislikeListener() {
      @Override
      public void onDislikeWindowShow() {
        Log.i(TAG, "onDislikeWindowShow");
      }

      @Override
      public void onDislikeItemClick(String reason) {
        Log.i(TAG, "onDislikeItemClick reason:" + reason);
      }

      @Override
      public void onDislikeWindowClose() {
        Log.i(TAG, "onDislikeWindowClose");
      }
    });

  }

  @Override
  public void render() {
    mExpressResponse.render();
    Log.d(TAG, "onRender");
  }

  @Override
  public void destroy() {
    if (mExpressResponse != null) {
      mExpressResponse = null;
    }
  }

  @Override
  public AdData getBoundData() {
    return mAdData;
  }

  @Override
  public void setAdListener(ADListener listener) {
    mListener = listener;
  }

  @Override
  public String getECPMLevel() {
    return mAdData.getECPMLevel();
  }


  /**
   * ======================================================================
   * 以下方法暂不支持
   */

  @Override
  public int getECPM() {
    return Constant.VALUE_NO_ECPM;
  }

  @Override
  public Map<String, Object> getExtraInfo() {
    return mAdData.getExtraInfo();
  }

  @Override
  public void preloadVideo() {

  }

  @Override
  public void sendWinNotification(int price) {
  }

  @Override
  public void sendWinNotification(Map<String, Object> map) {

  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {

  }

  @Override
  public void sendLossNotification(Map<String, Object> map) {

  }

  @Override
  public void setBidECPM(int price) {

  }

  @Override
  public void onDownloadConfirm(Activity context, int scenes, String infoUrl,
                                DownloadConfirmCallBack callBack) {

  }

  @Override
  public String getApkInfoUrl() {
    return null;
  }

  @Override
  public void setDownloadConfirmListener(DownloadConfirmListener listener) {

  }

  @Override
  public void setMediaListener(NativeExpressMediaListener mediaListener) {

  }

  @Override
  public void setViewBindStatusListener(ViewBindStatusListener listener) {

  }

  @Override
  public void negativeFeedback() {

  }

  @Override
  public void setAdSize(ADSize adSize) {

  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public void setNegativeFeedbackListener(NegativeFeedbackListener listener) {

  }
}
