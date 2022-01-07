package com.qq.e.union.adapter.bd.nativ;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import com.baidu.mobads.sdk.api.FeedNativeView;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.XAdNativeResponse;
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
import com.qq.e.comm.pi.AdData;
import com.qq.e.union.adapter.util.Constant;

import java.util.HashMap;
import java.util.Map;

public class BDNativeExpressAdDataAdapter extends NativeExpressADView implements ADEventListener {
  private static final String TAG = BDNativeExpressAdDataAdapter.class.getSimpleName();

  private NativeResponse mNativeResponse;
  private final AdData mAdData;
  private ADListener mListener;
  private final Context mContext;
  private String mEcpmLevel;

  public BDNativeExpressAdDataAdapter(Context context, NativeResponse data) {
    super(context);
    this.mNativeResponse = data;
    mContext = context;
    mAdData = new AdData() {
      @Override
      public String getTitle() {
        return data.getTitle();
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
        return AdPatternType.NATIVE_1IMAGE_2TEXT;
      }

      @Override
      public int getECPM() {
        return BDNativeExpressAdDataAdapter.this.getECPM();
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
    tryBindInteractionListener();
  }

  @Override
  public void render() {
    post(() -> addView(getView()));
    Log.d(TAG, "onRender");
    if (mListener == null) {
      return;
    }
    mListener.onADEvent(new ADEvent(AdEventType.AD_RENDER_SUCCESS, new Object[]{this}));
  }

  @Override
  public void destroy() {
    if (mNativeResponse != null) {
      mNativeResponse = null;
    }
  }

  @Override
  public AdData getBoundData() {
    return mAdData;
  }

  private View getView() {
    // 信息流智能优选
    FeedNativeView newAdView = new FeedNativeView(mContext);
    if (newAdView.getParent() != null) {
      ((ViewGroup) newAdView.getParent()).removeView(newAdView);
    }
    XAdNativeResponse response = (XAdNativeResponse) mNativeResponse;
    // 点击了负反馈渠道的回调
    response.setAdDislikeListener(this::removeAllViews);

    newAdView.setAdData((XAdNativeResponse) mNativeResponse);
    // 智能优选支持自定义视图样式，可以通过StyleParams来配置相关UI参数，开发者可自行参考百度接入文档进行设置
    return newAdView;
  }

  private void tryBindInteractionListener() {
    mNativeResponse.registerViewForInteraction(this, new NativeResponse.AdInteractionListener() {
      @Override
      public void onAdClick() {
        Log.d(TAG, "onADClicked");
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED,
            new Object[]{BDNativeExpressAdDataAdapter.this, ""}));
      }

      @Override
      public void onADExposed() {
        Log.d(TAG, "onADExposed");
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED,
            new Object[]{BDNativeExpressAdDataAdapter.this}));
      }

      @Override
      public void onADExposureFailed(int reason) {

      }

      @Override
      public void onADStatusChanged() {

      }

      @Override
      public void onAdUnionClick() {
        Log.d(TAG, "onADUnionClicked");
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED, new Object[]{this}));
      }
    });
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

  @Override
  public boolean isValid() {
    return true;
  }
}
