package com.qq.e.union.adapter.tt.interstitial;

import android.app.Activity;
import android.app.Dialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.qq.e.ads.cfg.DownAPPConfirmPolicy;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.mediation.interfaces.BaseInterstitialAd;
import com.qq.e.union.adapter.tt.util.TTAdManagerHolder;
import com.qq.e.union.adapter.tt.util.TTLoadAdUtil;
import com.qq.e.union.adapter.util.AdapterImageLoader;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ContextUtils;
import com.qq.e.union.adapter.util.ErrorCode;
import com.qq.e.union.tt.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 穿山甲新插屏适配器
 * 作用：封装穿山甲，适配优量汇插屏全屏和插屏半屏广告
 */
public class TTInterstitialAdAdapter extends BaseInterstitialAd implements TTAdManagerHolder.InitCallBack {

  private final String TAG = getClass().getSimpleName();
  protected final String posId;
  private final TTAdNative mTTAdNative;
  private TTFullScreenVideoAd mTTFullVideoAd;
  private ADListener unifiedInterstitialADListener;
  private final WeakReference<Activity> activityReference;
  private boolean mIsValid = false;
  private int mEcpm = Constant.VALUE_NO_ECPM;
  private String mRequestId;

  public TTInterstitialAdAdapter(Activity context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    TTAdManagerHolder.init(context, appId);
    this.posId = posId; // 半屏测试id "947793385" 全屏测试id 947747681
    this.activityReference = new WeakReference<>(ContextUtils.getActivity(context));
    mTTAdNative = TTAdSdk.getAdManager().createAdNative(context);
  }

  @Override
  public void loadAd() {
    TTLoadAdUtil.load(this);
  }

  private void loadAdAfterInitSuccess() {
    mIsValid = false;
    mTTAdNative.loadFullScreenVideoAd(getAdSlot(), new TTAdNative.FullScreenVideoAdListener() {
          //请求广告失败
          @Override
          public void onError(int code, String message) {
            Log.d(TAG, "onError");
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.NO_AD,
                  ErrorCode.NO_AD_FILL, code, message));
            }
          }

          @Override
          public void onFullScreenVideoAdLoad(TTFullScreenVideoAd ad) {
            Log.d(TAG, "onFullScreenVideoAdLoad");
            mIsValid = true;
            mTTFullVideoAd = ad;
            try {
              Map<String, Object> extraInfo;
              if ((extraInfo = ad.getMediaExtraInfo()) != null) {
                mRequestId = extraInfo.get("request_id").toString();
                mEcpm = Integer.parseInt(extraInfo.get("price").toString());
              }
            } catch (Exception e) {
              Log.e(TAG, e.toString());
            }
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_LOADED));
            }
          }

          @Override
          public void onFullScreenVideoCached() {
            Log.d(TAG, "onFullScreenVideoCached");
          }

          // 广告物料加载完成的回调
          @Override
          public void onFullScreenVideoCached(TTFullScreenVideoAd ttFullScreenVideoAd) {
            Log.d(TAG, "onFullScreenVideoCached");
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.VIDEO_CACHE));
            }
          }
        }
    );
  }

  @Override
  public void loadFullScreenAD() {
    loadAd();
  }

  @Override
  public void show() {
    show(activityReference.get());
  }

  @Override
  public void show(Activity act) {
    if (act == null) {
      return;
    }
    if (mTTFullVideoAd != null) {
      mTTFullVideoAd.setFullScreenVideoAdInteractionListener(new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {
        @Override
        public void onAdShow() {
          Log.d(TAG, "onAdShow");
          if (unifiedInterstitialADListener != null) {
            unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_SHOW));
            unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED));
          }
        }

        @Override
        public void onAdVideoBarClick() {
          Log.d(TAG, "onAdVideoBarClick");
          if (unifiedInterstitialADListener != null) {
            unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
          }
        }

        @Override
        public void onAdClose() {
          Log.d(TAG, "onAdClose");
          if (unifiedInterstitialADListener != null) {
            unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLOSED));
          }
        }

        @Override
        public void onVideoComplete() {
          Log.d(TAG, "onVideoComplete");
          if (unifiedInterstitialADListener != null) {
            unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.VIDEO_COMPLETE));
          }
        }

        @Override
        public void onSkippedVideo() {
          Log.d(TAG, "onSkippedVideo");
        }
      });
      mTTFullVideoAd.showFullScreenVideoAd(act, TTAdConstant.RitScenes.GAME_GIFT_BONUS, null);
    }
  }

  @Override
  public void showAsPopupWindow() {
    show();
  }

  @Override
  public void showAsPopupWindow(Activity act) {
    show(act);
  }

  @Override
  public void setAdListener(ADListener listener) {
    unifiedInterstitialADListener = listener;
  }

  @Override
  public boolean isValid() {
    return mIsValid;
  }

  @Override
  public void showFullScreenAD(Activity activity) {
    show(activity);
  }

  private AdSlot getAdSlot() {
    return new AdSlot.Builder()
        .setCodeId(posId)
        .setSupportDeepLink(true)
        // .setAdLoadType(PRELOAD)//推荐使用，用于标注此次的广告请求用途为预加载（当做缓存）还是实时加载，方便后续为开发者优化相关策略
        .build();
  }

  @Override
  public void destory() {
    Log.d(TAG, "Callback --> destory");
    mTTFullVideoAd = null;
  }

  @Override
  public int getECPM() {
    return mEcpm;
  }

  @Override
  public String getReqId() {
    return mRequestId;
  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {
    super.sendLossNotification(price, reason, adnId);
    if (mTTFullVideoAd != null) {
      mTTFullVideoAd.loss((double) price, String.valueOf(reason), adnId);
    }
  }

  @Override
  public void sendWinNotification(int price) {
    super.sendWinNotification(price);
    if (mTTFullVideoAd != null) {
      mTTFullVideoAd.win((double) price);
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

  /******************************以下方法暂未使用*****************************/

  @Override
  public void close() {

  }

  @Override
  public String getECPMLevel() {
    return null;
  }

  @Override
  public void setVideoOption(VideoOption videoOption) {}

  @Override
  public void setMinVideoDuration(int minVideoDuration) {}

  @Override
  public void setMaxVideoDuration(int maxVideoDuration) {}

  @Override
  public int getAdPatternType() {
    return 0;
  }


  @Override
  public void setDownAPPConfirmPolicy(DownAPPConfirmPolicy policy) {}

  @Override
  public int getVideoDuration() {
    return 0;
  }

  @Override
  public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {}

  @Override
  public void onInitSuccess() {
    loadAdAfterInitSuccess();
  }

  @Override
  public void onInitFail() {
    Log.i(TAG, "穿山甲 SDK 初始化失败，无法加载广告");
    if (unifiedInterstitialADListener != null) {
      unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.NO_AD,
          ErrorCode.NO_AD_FILL, ErrorCode.DEFAULT_ERROR_CODE, ErrorCode.DEFAULT_ERROR_MESSAGE));
    }
  }
}
