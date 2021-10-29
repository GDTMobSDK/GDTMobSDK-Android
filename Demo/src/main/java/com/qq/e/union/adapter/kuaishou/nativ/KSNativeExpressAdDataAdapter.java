package com.qq.e.union.adapter.kuaishou.nativ;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.kwad.sdk.api.KsFeedAd;
import com.kwad.sdk.api.model.MaterialType;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.ads.nativ.NativeExpressADView;
import com.qq.e.ads.nativ.NativeExpressMediaListener;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADEventListener;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.compliance.DownloadConfirmCallBack;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.pi.AdData;

import java.util.HashMap;
import java.util.Map;

import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_AD_CLICKED;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_AD_CLOSED;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_AD_EXPOSURE;
import static com.qq.e.ads.nativ.NativeExpressAD.EVENT_TYPE_ON_RENDER_SUCCESS;

public class KSNativeExpressAdDataAdapter extends NativeExpressADView implements ADEventListener {
  private static final String TAG = "KSNativeExpressAdDataAd";

  private KsFeedAd mKsFeedAd;
  private final AdData mAdData;
  private ADListener mListener;
  private final Context mContext;
  private String mEcpmLevel;
  private boolean mHasCallbackRender;
  private final int mWidth;

  public KSNativeExpressAdDataAdapter(Context context, KsFeedAd data, int width) {
    super(context);
    this.mKsFeedAd = data;
    mContext = context;
    mWidth = width >= 0 ? width : ViewGroup.LayoutParams.MATCH_PARENT;
    // 穿山甲 close 逻辑在 dislike 回调中实现
    tryBindInteractionAdListener();
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
       *  穿山甲样式无法与优量汇样式完全匹配，需要开发者注意样式的适配。
       */
      @Override
      public int getAdPatternType() {
        switch (data.getMaterialType()) {
          case MaterialType.VIDEO:
            return AdPatternType.NATIVE_VIDEO;
          case MaterialType.GROUP_IMG:
            return AdPatternType.NATIVE_3IMAGE;
          case MaterialType.SINGLE_IMG:
            return AdPatternType.NATIVE_2IMAGE_2TEXT;
          default:
            return AdPatternType.NATIVE_1IMAGE_2TEXT;
        }
      }

      @Override
      public int getECPM() {
        return KSNativeExpressAdDataAdapter.this.getECPM();
      }

      @Override
      public String getECPMLevel() {
        return mEcpmLevel;
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
  }

  @Override
  public void render() {
    if (mListener != null && !mHasCallbackRender) {
      mHasCallbackRender = true;
      mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_RENDER_SUCCESS,
        new Object[]{KSNativeExpressAdDataAdapter.this}));
    }
    post(()-> {
      View feedView = mKsFeedAd.getFeedView(mContext);
      Log.d(TAG, "render: " + feedView + ", parent:" + feedView.getParent());
      ViewGroup.LayoutParams params = feedView.getLayoutParams();
      if (params == null) {
        params = new ViewGroup.LayoutParams(mWidth, ViewGroup.LayoutParams.WRAP_CONTENT);
      } else {
        params.width = mWidth;
      }
      addView(feedView, params);
    });
  }

  @Override
  public void destroy() {
  }

  @Override
  public AdData getBoundData() {
    return mAdData;
  }

  private void tryBindInteractionAdListener() {
    mKsFeedAd.setAdInteractionListener(
      new KsFeedAd.AdInteractionListener() {
        @Override
        public void onAdClicked() {
          Log.d(TAG, "onAdClicked: ");
          if (mListener != null) {
            mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_CLICKED,
              new Object[]{KSNativeExpressAdDataAdapter.this, ""}));
          }
        }

        @Override
        public void onAdShow() {
          Log.d(TAG, "onAdShow: ");
          if (mListener != null) {
            mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_EXPOSURE,
              new Object[]{KSNativeExpressAdDataAdapter.this}));
          }
        }

        @Override
        public void onDislikeClicked() {
          Log.d(TAG, "onDislikeClicked: ");
          if (mListener != null) {
            mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_CLOSED,
              new Object[]{KSNativeExpressAdDataAdapter.this}));
          }
        }

        @Override
        public void onDownloadTipsDialogShow() {
          Log.d(TAG, "onDownloadTipsDialogShow");
        }

        @Override
        public void onDownloadTipsDialogDismiss() {
          Log.d(TAG, "onDownloadTipsDialogShow");
        }
      });
  }


  @Override
  public void setAdListener(ADListener listener) {
    mListener = listener;
  }

  @Override
  public int getECPM() {
    return mKsFeedAd.getECPM();
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
  public void preloadVideo() {

  }

  @Override
  public void sendWinNotification(int price) {
  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {

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
}
