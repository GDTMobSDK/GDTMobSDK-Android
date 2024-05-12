package com.qq.e.union.adapter.tt.reward;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTRewardVideoAd;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.mediation.interfaces.BaseRewardAd;
import com.qq.e.union.adapter.tt.util.TTAdManagerHolder;
import com.qq.e.union.adapter.tt.util.TTLoadAdUtil;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ContextUtils;
import com.qq.e.union.adapter.util.ErrorCode;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * 穿山甲激励视频适配器
 * 作用：封装穿山甲激励视频，适配优量汇激励视频
 */
public class TTRewardAdAdapter extends BaseRewardAd implements TTAdManagerHolder.InitCallBack {

  private final String TAG = getClass().getSimpleName();

  private WeakReference<Activity> activityReference;
  private String posId;
  private TTAdNative mTTAdNative;
  private TTRewardVideoAd rewardAd;
  private ADListener listener;

  private boolean isShownDownloadBar = true;
  private ServerSideVerificationOptions serverSideVerificationOptions;
  private int ecpm = Constant.VALUE_NO_ECPM;
  private String requestId;
  private String mAppId;
  private boolean mIsStartDownload;
  private boolean mIsPaused;

  private boolean mIsEnableAdvancedReward = true; // 开发者自行决定是否开放进阶奖励功能，此处仅为示例
  private RewardAdvancedInfo mRewardAdvancedInfo;

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
    mAppId = appID;
  }

  @Override
  public void setAdListener(ADListener listener) {
    this.listener = listener;
  }

  @Override
  public void loadAD() {
    Log.d(TAG, "loadAD: ");
    TTLoadAdUtil.load(this);
  }

  private void loadAdAfterInitSuccess() {
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
        onAdError(ErrorCode.NO_AD_FILL, code, message);
      }

      //视频广告加载后，视频资源缓存到本地的回调，在此回调后，播放本地视频，流畅不阻塞。
      @Override
      public void onRewardVideoCached() {
        Log.d(TAG, "onRewardVideoCached: ");
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.VIDEO_CACHE));
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
        mRewardAdvancedInfo = new RewardAdvancedInfo();
        try {
          ecpm = (int) ad.getMediaExtraInfo().get("price");
        } catch (Exception e) {
          Log.d(TAG, "get ecpm error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: ecpm = " + ecpm);
        try {
          Object o = ad.getMediaExtraInfo().get("request_id");
          if (o != null) {
            requestId = o.toString();
          }
        } catch (Exception e) {
          Log.e(TAG, "get request_id error ", e);
        }
        Log.d(TAG, "onAdDataSuccess request_id = " + requestId);
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.AD_LOADED));
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
          listener.onADEvent(new ADEvent(AdEventType.AD_SHOW));
          // 由于穿山甲没有曝光回调，所以曝光和 show 一块回调
          listener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED));
        }
      }

      @Override
      public void onAdVideoBarClick() {
        Log.d(TAG, "onAdVideoBarClick: ");
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
          if (isAppAd()) {
            listener.onADEvent(new ADEvent(AdEventType.APP_AD_CLICKED));
          }
        }
      }

      @Override

      public void onAdClose() {
        Log.d(TAG, "onAdClose: ");
        if (mIsEnableAdvancedReward && mRewardAdvancedInfo != null) {
          Log.d(TAG, "本次奖励共发放：" + mRewardAdvancedInfo.getRewardAdvancedAmount());
        }
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.AD_CLOSED));
        }
      }

      //视频播放完成回调
      @Override
      public void onVideoComplete() {
        Log.d(TAG, "onVideoComplete: ");
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.VIDEO_COMPLETE));
        }
      }

      @Override
      public void onVideoError() {
        Log.d(TAG, "onVideoError: ");
        onAdError(ErrorCode.VIDEO_PLAY_ERROR, ErrorCode.DEFAULT_ERROR_CODE, ErrorCode.DEFAULT_ERROR_MESSAGE);
      }

      // 视频播放完成后，奖励验证回调，rewardVerify：是否有效，rewardAmount：奖励梳理，rewardName：奖励名称
      // 此接口仅支持基础奖励
      @Override
      public void onRewardVerify(boolean rewardVerify, int rewardAmount, String rewardName, int errorCode, String errorMsg) {
        Log.d(TAG, "onRewardVerify: ");
        if (rewardVerify && listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.AD_REWARD, new Object[]{""}));
        }
      }

      /**
       * 激励视频播放完毕，验证是否有效发放奖励的回调 4400版本新增
       *
       * @param isRewardValid 奖励有效
       * @param rewardType 奖励类型，0:基础奖励 >0:进阶奖励
       * @param extraInfo 奖励的额外参数
       */
      @Override
      public void onRewardArrived(boolean isRewardValid, int rewardType, Bundle extraInfo) {
        Log.d(TAG, "onRewardArrived: ");
        RewardBundleModel rewardBundleModel = new RewardBundleModel(extraInfo);
        Log.e(TAG, "Callback --> rewardVideoAd has onRewardArrived " +
            "\n奖励是否有效：" + isRewardValid +
            "\n奖励类型：" + rewardType +
            "\n奖励名称：" + rewardBundleModel.getRewardName() +
            "\n奖励数量：" + rewardBundleModel.getRewardAmount() +
            "\n建议奖励百分比：" + rewardBundleModel.getRewardPropose());
        if (!isRewardValid) {
          Log.d(TAG, "发送奖励失败 code：" + rewardBundleModel.getServerErrorCode() +
              "\n msg：" + rewardBundleModel.getServerErrorMsg());
          return;
        }

        if (!mIsEnableAdvancedReward) {
          // 未使用进阶奖励功能
          if (rewardType == TTRewardVideoAd.REWARD_TYPE_DEFAULT) {
            Log.d(TAG, "普通奖励发放，name:" + rewardBundleModel.getRewardName() +
                "\namount:" + rewardBundleModel.getRewardAmount());
          }
        } else {
          // 使用了进阶奖励功能
          if (mRewardAdvancedInfo != null) {
            mRewardAdvancedInfo.proxyRewardModel(rewardBundleModel, false);
          }
        }
      }

      @Override
      public void onSkippedVideo() {
        Log.d(TAG, "onSkippedVideo: ");
      }
    });
    if (isAppAd()) {
      rewardAd.setDownloadListener(new TTAppDownloadListener() {
        @Override
        public void onIdle() {
          mIsStartDownload = false;
        }

        @Override
        public void onDownloadActive(long totalBytes, long currBytes, String fileName,
                                     String appName) {
          Log.d(TAG, "onDownloadActive==totalBytes=" + totalBytes + ",currBytes=" + currBytes +
              ",fileName=" + fileName + ",appName=" + appName);

          if (!mIsStartDownload) {
            mIsStartDownload = true;
            fireAdEvent(AdEventType.ADAPTER_APK_DOWNLOAD_START, appName);
          }

          if (mIsPaused) {
            mIsPaused = false;
            fireAdEvent(AdEventType.ADAPTER_APK_DOWNLOAD_RESUME, appName);
          }
        }

        @Override
        public void onDownloadPaused(long totalBytes, long currBytes, String fileName,
                                     String appName) {
          Log.d(TAG, "onDownloadPaused===totalBytes=" + totalBytes + ",currBytes=" + currBytes +
              ",fileName=" + fileName + ",appName=" + appName);
          mIsPaused = true;
          fireAdEvent(AdEventType.ADAPTER_APK_DOWNLOAD_PAUSE, appName);
        }

        @Override
        public void onDownloadFailed(long totalBytes, long currBytes, String fileName,
                                     String appName) {
          Log.d(TAG, "onDownloadFailed==totalBytes=" + totalBytes + ",currBytes=" + currBytes +
              ",fileName=" + fileName + ",appName=" + appName);
          fireAdEvent(AdEventType.ADAPTER_APK_DOWNLOAD_FAIL, appName);
        }

        @Override
        public void onDownloadFinished(long totalBytes, String fileName, String appName) {
          Log.d(TAG, "onDownloadFinished==totalBytes=" + totalBytes + ",fileName=" + fileName +
              ",appName=" + appName);
          fireAdEvent(AdEventType.ADAPTER_APK_DOWNLOAD_FINISH, appName);
        }

        @Override
        public void onInstalled(String fileName, String appName) {
          Log.d(TAG, "onInstalled==" + ",fileName=" + fileName + ",appName=" + appName);
          fireAdEvent(AdEventType.ADAPTER_APK_INSTALLED, appName);
        }
      });

    }
    // step5：展示广告
    rewardAd.showRewardVideoAd(activityReference.get());
    rewardAd = null;
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

  @Override
  public int getECPM() {
    return ecpm;
  }

  @Override
  public String getECPMLevel() {
    return null;
  }

  @Override
  public String getReqId() {
    return requestId;
  }

  @Override
  public Map<String, Object> getExtraInfo() {
    Map<String, Object> map = new HashMap<>();
    map.put("request_id", getReqId());
    return map;
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
  private void onAdError(int errorCode, Integer onlineErrorCode, String errorMessage) {
    if (listener != null) {
      listener.onADEvent(new ADEvent(AdEventType.AD_ERROR, new Object[]{errorCode}, onlineErrorCode, errorMessage));
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
        .setUserID(serverSideVerificationOptions != null ? serverSideVerificationOptions.getUserId() : "")// 用户id,必传参数
        .setOrientation(getScreenOrientation()); //必填参数，期望视频的播放方向：TTAdConstant.HORIZONTAL 或 TTAdConstant.VERTICAL
  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {
    super.sendLossNotification(price, reason, adnId);
    if (rewardAd != null) {
      rewardAd.loss((double) price, String.valueOf(reason), adnId);
    }
  }

  @Override
  public void sendWinNotification(int price) {
    super.sendWinNotification(price);
    if (rewardAd != null) {
      rewardAd.win((double) price);
    }
  }


  @Override
  public void setBidECPM(int price) {
    super.setBidECPM(price);
  }

  @Override
  public void onInitSuccess() {
    loadAdAfterInitSuccess();
  }

  @Override
  public void onInitFail() {
    Log.i(TAG, "穿山甲 SDK 初始化失败，无法加载广告");
    onAdError(ErrorCode.NO_AD_FILL, ErrorCode.DEFAULT_ERROR_CODE, ErrorCode.DEFAULT_ERROR_MESSAGE);
  }

  private void fireAdEvent(int adEventType, String appName) {
    if (listener != null) {
      listener.onADEvent(new ADEvent(adEventType, posId, mAppId, getReqId(), appName));
    }
  }

  private boolean isAppAd() {
    if (rewardAd != null && rewardAd.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD) {
      return true;
    }
    return false;
  }
}
