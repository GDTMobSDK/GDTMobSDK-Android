package com.qq.e.union.adapter.tt.nativ;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;

import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
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
import com.qq.e.comm.util.AdError;
import com.qq.e.union.adapter.util.Constant;

import java.util.HashMap;
import java.util.Map;


public class TTNativeExpressAdDataAdapter extends NativeExpressADView implements ADEventListener {
  private static final String TAG = TTNativeExpressAdDataAdapter.class.getSimpleName();

  private TTNativeExpressAd mTTNativeExpressAd;
  private NativeExpressMediaListener mNativeExpressMediaListener;
  private final AdData mAdData;
  private ADListener mListener;
  private final Context mContext;
  private String mEcpmLevel;
  private boolean mIsExposed;

  public TTNativeExpressAdDataAdapter(Context context, TTNativeExpressAd data) {
    super(context);
    this.mTTNativeExpressAd = data;
    mContext = context;
    // 穿山甲 close 逻辑在 dislike 回调中实现
    bindDislike(data);
    tryBindInteractionAdListener();
    tryBindVideoAdListener();
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
        switch (data.getImageMode()) {
          case TTAdConstant.IMAGE_MODE_VIDEO:
          case TTAdConstant.IMAGE_MODE_VIDEO_VERTICAL:
            return AdPatternType.NATIVE_VIDEO;
          case TTAdConstant.IMAGE_MODE_GROUP_IMG:
            return AdPatternType.NATIVE_3IMAGE;
          case TTAdConstant.IMAGE_MODE_LARGE_IMG:
            return AdPatternType.NATIVE_2IMAGE_2TEXT;
          case TTAdConstant.IMAGE_MODE_SMALL_IMG:
          default:
            return AdPatternType.NATIVE_1IMAGE_2TEXT;
        }
      }

      @Override
      public int getECPM() {
        return TTNativeExpressAdDataAdapter.this.getECPM();
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
        HashMap<String, Object> map = new HashMap<>();
        String s = null;
        try {
          Object o = data.getMediaExtraInfo().get("request_id");
          if (o != null) {
            s = o.toString();
          }
        } catch (Exception e) {
          Log.d(TAG, "get request_id error ", e);
        }
        map.put("request_id", s);
        return map;
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
    mTTNativeExpressAd.render();
  }

  @Override
  public void destroy() {
    if (mTTNativeExpressAd != null) {
      mTTNativeExpressAd.destroy();
      mTTNativeExpressAd = null;
    }
  }

  @Override
  public AdData getBoundData() {
    return mAdData;
  }

  private void tryBindInteractionAdListener() {
    mTTNativeExpressAd.setExpressInteractionListener(new TTNativeExpressAd.ExpressAdInteractionListener() {
      @Override
      public void onAdClicked(View view, int type) {
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED,
            new Object[]{TTNativeExpressAdDataAdapter.this}));
      }

      @Override
      public void onAdShow(View view, int type) {
        if (mListener == null && !mIsExposed) {
          return;
        }
        mIsExposed = true;
        mListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED,
            new Object[]{TTNativeExpressAdDataAdapter.this}));
      }

      @Override
      public void onRenderFail(View view, String msg, int code) {
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.AD_RENDER_FAILED,
            new Object[]{TTNativeExpressAdDataAdapter.this}));
      }

      @Override
      public void onRenderSuccess(View view, float width, float height) {
        mListener.onADEvent(new ADEvent(AdEventType.AD_RENDER_SUCCESS,
            new Object[]{TTNativeExpressAdDataAdapter.this}));
        post(() -> addView(mTTNativeExpressAd.getExpressAdView()));
      }
    });
  }

  private void tryBindVideoAdListener() {
    mTTNativeExpressAd.setVideoAdListener(new TTNativeExpressAd.ExpressVideoAdListener() {
      @Override
      public void onVideoLoad() {
        if (mNativeExpressMediaListener != null) {
          mNativeExpressMediaListener.onVideoCached(TTNativeExpressAdDataAdapter.this);
        }
      }

      @Override
      public void onVideoError(int errorCode, int extraCode) {
        if (mNativeExpressMediaListener != null) {
          mNativeExpressMediaListener.onVideoError(TTNativeExpressAdDataAdapter.this,
              new AdError(errorCode, ""));
        }
      }

      @Override
      public void onVideoAdStartPlay() {
        if (mNativeExpressMediaListener != null) {
          mNativeExpressMediaListener.onVideoStart(TTNativeExpressAdDataAdapter.this);
        }
      }

      @Override
      public void onVideoAdPaused() {
        if (mNativeExpressMediaListener != null) {
          mNativeExpressMediaListener.onVideoPause(TTNativeExpressAdDataAdapter.this);
        }
      }

      @Override
      public void onVideoAdComplete() {
        if (mNativeExpressMediaListener != null) {
          mNativeExpressMediaListener.onVideoComplete(TTNativeExpressAdDataAdapter.this);
        }
      }

      @Override
      public void onVideoAdContinuePlay() {

      }

      @Override
      public void onProgressUpdate(long t, long l1) {

      }

      @Override
      public void onClickRetry() {

      }
    });
  }

  private void bindDislike(TTNativeExpressAd ad) {
    //使用默认模板中默认dislike弹出样式
    ad.setDislikeCallback((Activity) mContext, new TTAdDislike.DislikeInteractionCallback() {
      @Override
      public void onShow() {
        Log.d(TAG, "弹出不感兴趣对话框");
      }

      @Override
      public void onSelected(int position, String value, boolean enforce) {
        //用户选择不喜欢原因后，移除广告展示
        Log.d(TAG, "移除广告");
        if (mListener != null) {
          mListener.onADEvent(new ADEvent(AdEventType.AD_CLOSED,
              new Object[]{TTNativeExpressAdDataAdapter.this}));
        }
        removeView(mTTNativeExpressAd.getExpressAdView());
        if (enforce) {
          Log.d(TAG, "强制移除广告");
        }
      }

      @Override
      public void onCancel() {

      }

    });
  }

  @Override
  public void setAdListener(ADListener listener) {
    mListener = listener;
  }

  @Override
  public void setMediaListener(NativeExpressMediaListener mediaListener) {
    mNativeExpressMediaListener = mediaListener;
  }

  @Override
  public String getECPMLevel() {
    return mAdData.getECPMLevel();
  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {
    if (mTTNativeExpressAd != null) {
      mTTNativeExpressAd.loss((double) price, String.valueOf(reason), adnId);
    }
  }

  @Override
  public void sendWinNotification(int price) {
    if (mTTNativeExpressAd != null) {
      mTTNativeExpressAd.win((double) price);
    }
  }

  @Override
  public void setBidECPM(int price) {
    if (mTTNativeExpressAd != null) {
      mTTNativeExpressAd.setPrice((double) price);
    }
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
