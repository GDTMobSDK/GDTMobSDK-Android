package com.qq.e.union.adapter.tt.reward;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTRewardVideoAd;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.mediation.interfaces.BaseRewardAd;
import com.qq.e.union.adapter.tt.util.TTAdManagerHolder;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ContextUtils;
import com.qq.e.union.adapter.util.ErrorCode;

import java.lang.ref.WeakReference;

/**
 * 穿山甲激励视频适配器
 * 作用：封装穿山甲激励视频，适配优量汇激励视频
 */
public class TTRewardAdAdapter extends BaseRewardAd {

  private final String TAG = getClass().getSimpleName();

  private static final String KEY_APPID = "appId";
  private static final String KEY_REWARD_NAME = "rewardName";
  private static final String KEY_REWARD_AMOUNT = "rewardAmount";
  private static final String KEY_USER_ID = "userId";
  private static final String KEY_SHOW_DOWNLOAD_BAR = "isShowDownloadBar";

  private WeakReference<Activity> activityReference;
  private String posId;
  private TTAdNative mTTAdNative;
  private TTRewardVideoAd rewardAd;
  private ADListener listener;

  private String rewardName;
  private int rewardAmount;
  private String userId;
  private boolean isShownDownloadBar = true;
  private ServerSideVerificationOptions serverSideVerificationOptions;
  private int ecpm = Constant.VALUE_NO_ECPM;

  /**
   * 激励视频过期时间，开发者可自定义
   * 目前的实现是广告 load 后，30 分钟内均有效
   */
  private long expireTime;
  private boolean hasAdShown;

  public TTRewardAdAdapter(Context context, String appID, String posID, String ext) {
    super(context, appID, posID, ext);
    // step1：SDK 初始化
    TTAdManagerHolder.init(context, appID);
    // step2：创建 TTAdNative 对象
    mTTAdNative = TTAdManagerHolder.get().createAdNative(context);
    this.activityReference = new WeakReference<>(ContextUtils.getActivity(context));
    this.posId = posID;
  }

  @Override
  public void setAdListener(ADListener listener) {
    this.listener = listener;
  }

  @Override
  public void loadAD() {
    Log.d(TAG, "loadAD: ");
    if (mTTAdNative == null) {
      Log.i(TAG, "穿山甲 SDK 初始化错误，无法加载广告");
      return;
    }
    // step3：创建广告请求参数AdSlot,具体参数含义参考文档
    AdSlot  adSlot = setAdSlotParams(new AdSlot.Builder()).build();

    // step4：请求广告
    mTTAdNative.loadRewardVideoAd(adSlot, new TTAdNative.RewardVideoAdListener() {
      @Override
      public void onError(int code, String message) {
        Log.d(TAG, "onError: code: " + code + "message: " + message);
        onAdError(ErrorCode.NO_AD_FILL);
      }

      //视频广告加载后，视频资源缓存到本地的回调，在此回调后，播放本地视频，流畅不阻塞。
      @Override
      public void onRewardVideoCached() {
        Log.d(TAG, "onRewardVideoCached: ");
        if (listener != null) {
          listener.onADEvent(new ADEvent(EVENT_TYPE_ON_VIDEO_CACHED));
        }
      }

      @Override
      public void onRewardVideoCached(TTRewardVideoAd ttRewardVideoAd) {
        onRewardVideoCached();
      }

      //视频广告的素材加载完毕，比如视频url等，在此回调后，可以播放在线视频，网络不好可能出现加载缓冲，影响体验。
      @Override
      public void onRewardVideoAdLoad(TTRewardVideoAd ad) {
        Log.d(TAG, "onRewardVideoAdLoad: " + getAdType(ad.getRewardVideoAdType()));
        rewardAd = ad;
        try {
          ecpm = (int) ad.getMediaExtraInfo().get("price");
        } catch (Exception e) {
          Log.e(TAG, "get ecpm error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: ecpm = " + ecpm);
        if (listener != null) {
          listener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_LOADED));
        }
        expireTime = SystemClock.elapsedRealtime() + 30 * DateUtils.MINUTE_IN_MILLIS;
      }
    });
  }

  @Override
  public void showAD() {
    Log.d(TAG, "showAD: ");
    if (rewardAd == null || activityReference.get()  == null) {
      return;
    }
    rewardAd.setShowDownLoadBar(isShownDownloadBar);
    rewardAd.setRewardAdInteractionListener(new TTRewardVideoAd.RewardAdInteractionListener() {

      @Override
      public void onAdShow() {
        Log.d(TAG, "onAdShow: ");
        hasAdShown = true;
        if (listener != null) {
          listener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_SHOW));
          // 由于穿山甲没有曝光回调，所以曝光和 show 一块回调
          listener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_EXPOSE));
        }
      }

      @Override
      public void onAdVideoBarClick() {
        Log.d(TAG, "onAdVideoBarClick: ");
        if (listener != null) {
          listener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_CLICK, new Object[]{""}));
        }
      }

      @Override

      public void onAdClose() {
        Log.d(TAG, "onAdClose: ");
        if (listener != null) {
          listener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_CLOSE));
        }
      }

      //视频播放完成回调
      @Override
      public void onVideoComplete() {
        Log.d(TAG, "onVideoComplete: ");
        if (listener != null) {
          listener.onADEvent(new ADEvent(EVENT_TYPE_ON_VIDEO_COMPLETE));
        }
      }

      @Override
      public void onVideoError() {
        Log.d(TAG, "onVideoError: ");
        onAdError(ErrorCode.VIDEO_PLAY_ERROR);
      }

      //视频播放完成后，奖励验证回调，rewardVerify：是否有效，rewardAmount：奖励梳理，rewardName：奖励名称
      @Override
      public void onRewardVerify(boolean rewardVerify, int rewardAmount, String rewardName, int errorCode, String errorMsg) {
        Log.d(TAG, "onRewardVerify: ");
        if (rewardVerify && listener != null) {
          listener.onADEvent(new ADEvent(EVENT_TYPE_ON_REWARD, new Object[]{""}));
        }
      }

      @Override
      public void onSkippedVideo() {
        Log.d(TAG, "onSkippedVideo: ");
      }
    });
    // step5：展示广告
    rewardAd.showRewardVideoAd(activityReference.get());
    rewardAd = null;
  }

  @Override
  public long getExpireTimestamp() {
    return expireTime;
  }

  @Override
  public boolean hasShown() {
    return hasAdShown;
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
  public void setVolumeOn(boolean volumOn) {
    // 暂不支持
  }

  @Override
  public int getVideoDuration() {
    // 暂不支持
    return 0;
  }

  /**
   * 激励视频 Server to server 校验
   * @param options
   */
  @Override
  public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
    serverSideVerificationOptions = options;
  }

  private int getScreenOrientation() {
    int orientation = TTAdConstant.VERTICAL;
    if (activityReference.get()  != null
            && activityReference.get() .getResources() != null
            && activityReference.get() .getResources().getConfiguration() != null
            && activityReference.get() .getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      orientation = TTAdConstant.HORIZONTAL;
    }
    return orientation;
  }

  /**
   * @param errorCode 错误码
   */
  private void onAdError(int errorCode) {
    if (listener != null) {
      listener.onADEvent(new ADEvent(EVENT_TYPE_ON_ERROR, new Object[]{errorCode}));
    }
  }

  private String getAdType(int type) {
    switch (type) {
      case TTAdConstant.AD_TYPE_COMMON_VIDEO:
        return "普通激励视频, type=" + type;
      case TTAdConstant.AD_TYPE_PLAYABLE_VIDEO:
        return "Playable激励视频, type=" + type;
      case TTAdConstant.AD_TYPE_PLAYABLE:
        return "纯Playable, type=" + type;
    }

    return "未知类型, type=" + type;
  }

  protected AdSlot.Builder setAdSlotParams(AdSlot.Builder builder) {
    return builder
        .setCodeId(posId)
        .setImageAcceptedSize(1080, 1920)
        .setRewardName(rewardName) //奖励的名称
        .setRewardAmount(rewardAmount)  //奖励的数量
        .setUserID(userId)// 用户id,必传参数
        .setOrientation(getScreenOrientation()); //必填参数，期望视频的播放方向：TTAdConstant.HORIZONTAL 或 TTAdConstant.VERTICAL
  }
}
