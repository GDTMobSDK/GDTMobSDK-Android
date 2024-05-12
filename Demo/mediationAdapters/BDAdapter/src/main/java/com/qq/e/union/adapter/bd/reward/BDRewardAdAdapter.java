package com.qq.e.union.adapter.bd.reward;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;

import com.baidu.mobads.sdk.api.RewardVideoAd;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.mediation.interfaces.BaseRewardAd;
import com.qq.e.union.adapter.bd.util.BDAdManager;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * 百度激励视频适配器
 * 作用：封装百度激励视频，适配优量汇激励视频
 */
public class BDRewardAdAdapter extends BaseRewardAd {

  private static final String TAG = BDRewardAdAdapter.class.getSimpleName();

  private WeakReference<Context> contextReference;
  private String posId;
  private ADListener listener;
  private int ecpm = Constant.VALUE_NO_ECPM;
  /**
   * 激励视频过期时间，开发者可自定义
   * 目前的实现是广告 load 后，30 分钟内均有效
   */
  private long expireTime;
  private RewardVideoAd rewardAd;
  private boolean hasAdShown;
  private Handler mainHandler;

  public BDRewardAdAdapter(Context context, String appId, String posID, String ext) {
    super(context, appId, posID, ext);
    BDAdManager.init(context, appId);
    this.contextReference = new WeakReference<>(context);
    this.posId = posID;
    mainHandler = new Handler(Looper.getMainLooper());
    initAd();
  }

  @Override
  public void setAdListener(ADListener listener) {
    this.listener = listener;
  }

  private void initAd() {
    Log.d(TAG, "initAd");
    rewardAd = new RewardVideoAd(contextReference.get(), posId,
        new RewardVideoAd.RewardVideoAdListener() {
      private boolean hasReward;

      @Override
      public void onVideoDownloadSuccess() {
        Log.d(TAG, "onVideoDownloadSuccess");
        fireAdEvent(AdEventType.VIDEO_CACHE);
      }

      @Override
      public void onVideoDownloadFailed() {
        Log.d(TAG, "onVideoDownloadFailed");
        // 视频缓存失败，如果想走本地播放，可以在这儿重新load下一条广告，最好限制load次数（4-5次即可）。
        onAdError(ErrorCode.VIDEO_DOWNLOAD_FAIL, ErrorCode.DEFAULT_ERROR_MESSAGE);
      }

      @Override
      public void playCompletion() {
        Log.d(TAG, "playCompletion");
        // 播放完成回调，媒体可以在这儿给用户奖励
        fireAdEvent(AdEventType.VIDEO_COMPLETE);
        onAdReward();
      }

        @Override
        public void onAdLoaded() {
          try {
            ecpm = Integer.parseInt(rewardAd.getECPMLevel());
          } catch (Exception e) {
            Log.d(TAG, "get ecpm error ", e);
          }
          Log.d(TAG, "onAdDataSuccess: ecpm = " + ecpm);
          expireTime = SystemClock.elapsedRealtime() + 30 * DateUtils.MINUTE_IN_MILLIS;
          fireAdEvent(AdEventType.AD_LOADED);
        }

        @Override
        public void onAdSkip(float v) {
          Log.d(TAG, "onAdSkip");
        }

        @Override
        public void onRewardVerify(boolean b) {
          onAdReward();
        }

        @Override
      public void onAdShow() {
        Log.d(TAG, "onAdShow");
        // 视频开始播放时候的回调
        hasAdShown = true;
        fireAdEvent(AdEventType.AD_SHOW);
        // 由于百度没有曝光回调，所以曝光和 show 一块回调
        fireAdEvent(AdEventType.AD_EXPOSED);
      }

      @Override
      public void onAdClick() {
        Log.d(TAG, "onAdClick");
        // 广告被点击的回调
        fireAdEvent(AdEventType.AD_CLICKED);
      }

      @Override
      public void onAdClose(float playScale) {
        Log.d(TAG, "onAdClose: playScale: " + playScale);
        if (listener == null) {
          return;
        }
        // 用户关闭了广告 playScale[0.0-1.0],1.0表示播放完成，媒体可以按照自己的设计给予奖励
        // 需特别注意，playCompletion 也会进行奖励，开发者自行选择此处是否奖励，如果此处也奖励则可能有两次奖励回调
        if (playScale >= 1.0f) {
          onAdReward();
        }
        fireAdEvent(AdEventType.AD_CLOSED);
      }

      @Override
      public void onAdFailed(String arg0) {
        Log.d(TAG, "onAdFailed: " + arg0);
        onAdError(ErrorCode.NO_AD_FILL, arg0);
      }


          private void onAdReward() {
            Log.d(TAG, "onAdReward");
            if (hasReward) {
              return;
            }
            hasReward = true;
            fireAdEvent(AdEventType.AD_REWARD, new Object[]{""});
          }

          /**
           * @param errorCode 错误码
           */
          private void onAdError(int errorCode, String errorMessage) {
            Log.d(TAG, "onAdError: errorCode");
            fireAdEvent(AdEventType.AD_ERROR, new Object[]{errorCode}, ErrorCode.DEFAULT_ERROR_CODE, errorMessage);
          }
        }, false);
  }

  private void fireAdEvent(int eventId, Object... params) {
    mainHandler.post(new Runnable() {
      @Override
      public void run() {
        if (listener != null) {
          listener.onADEvent(new ADEvent(eventId, params));
        }
      }
    });
  }

  @Override
  public void loadAD() {
    Log.d(TAG, "loadAD");
    rewardAd.load();
  }

  @Override
  public void showAD() {
    Log.d(TAG, "showAD");
    if (contextReference.get() != null) {
      rewardAd.show();
    }
  }

  @Override
  public long getExpireTimestamp() {
    return expireTime;
  }

  @Override
  public boolean isValid() {
    return SystemClock.elapsedRealtime() <= expireTime;
  }

  @Override
  public boolean hasShown() {
    return hasAdShown;
  }

  /**
   * 获取 ecpm，默认没有
   * @return 单位分
   */
  @Override
  public int getECPM() {
    return ecpm;
  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {
    super.sendLossNotification(price, reason, adnId);
    if (rewardAd != null) {
      rewardAd.biddingFail(String.valueOf(reason));
    }
  }

  @Override
  public void sendWinNotification(int price) {
    super.sendWinNotification(price);
    if (rewardAd != null) {
      rewardAd.biddingSuccess(String.valueOf(price));
    }
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
  public void setVolumeOn(boolean volumOn) {
    // 暂不支持
  }

  @Override
  public int getVideoDuration() {
    // 暂不支持
    return 0;
  }

  @Override
  public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
    // 暂不支持
  }
}
