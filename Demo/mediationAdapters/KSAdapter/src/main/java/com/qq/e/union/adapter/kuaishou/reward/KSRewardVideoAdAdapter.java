package com.qq.e.union.adapter.kuaishou.reward;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;


import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsRewardVideoAd;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsVideoPlayConfig;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.mediation.interfaces.BaseRewardAd;
import com.qq.e.union.adapter.kuaishou.util.KSSDKInitUtil;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ContextUtils;
import com.qq.e.union.adapter.util.ErrorCode;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 快手激励视频Adapter实现demo
 */
public class KSRewardVideoAdAdapter extends BaseRewardAd {

  private KsRewardVideoAd mRewardVideoAd;
  private WeakReference<Activity> mActivityReference;
  private ADListener mListener;
  private boolean mIsShown;
  private long mExpireTime;
  private long mPosId;
  private boolean mIsLoadOvertime; // 快手SDK拉广告很慢，这里加一个超时限制，超过10s后就不触发回调了，因为默认配置超时时机是10s。开发者可自行调整，
  private boolean mIsShowLandscape;

  private static final String TAG = KSRewardVideoAdAdapter.class.getSimpleName();
  private static final int LOAD_COST_TIME = 10 * 1000;

  public KSRewardVideoAdAdapter(Context context, String appId, String posID, String ext) {
    super(context, appId, posID, ext);
    mPosId = Long.parseLong(posID);
    KSSDKInitUtil.init(context, appId);
    mActivityReference = new WeakReference<>(ContextUtils.getActivity(context));
  }

  @Override
  public void setAdListener(ADListener listener) {
    mListener = listener;
  }

  @Override
  public void loadAD() {
    requestRewardVideoAd();
  }

  @Override
  public void showAD() {
    showRewardVideoAd(buildConfigHPShowScene());
    mIsShown = true;
  }

  @Override
  public long getExpireTimestamp() {
    return mExpireTime;
  }

  @Override
  public boolean isValid() {
    return true;
  }

  @Override
  public boolean hasShown() {
    return mIsShown;
  }

  /**
   * 需要统一单位为分
   */
    @Override
    public int getECPM() {
        return mRewardVideoAd != null ? mRewardVideoAd.getECPM() : Constant.VALUE_NO_ECPM;
    }

  @Override
  public String getECPMLevel() {
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
  public int getVideoDuration() {
    // 暂不支持
    return 0;
  }

  @Override
  public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
    // 暂不支持
  }

  @Override
  public void setVolumeOn(boolean volumOn) {
   // 暂不支持
  }

  // 1.请求激励视频广告，获取广告对象，KsRewardVideoAd
  private void requestRewardVideoAd() {
    mRewardVideoAd = null;
    KsScene scene = new KsScene.Builder(mPosId).build(); // 90009001 此为测试posId，请联系快手平台申请正式posId
    KsAdSDK.getLoadManager().loadRewardVideoAd(scene, new KsLoadManager.RewardVideoAdListener() {
      @Override
      public void onError(int code, String msg) {
        if (mIsLoadOvertime) {
          return;
        }
        Log.e(TAG, "onError: code : " + code + "  msg: " + msg);
        onAdError(ErrorCode.NO_AD_FILL, code, msg);
      }

      @Override
      public void onRewardVideoResult(@Nullable List<KsRewardVideoAd> adList) {
        if (adList != null) {
          Log.d(TAG, "激励视频广告请求填充个数: " + adList.size());
        }
      }

      @Override
      public void onRewardVideoAdLoad(@Nullable List<KsRewardVideoAd> adList) {
        if (mIsLoadOvertime) {
          return;
        }
        if (adList != null && adList.size() > 0) {
          mRewardVideoAd = adList.get(0);
          mExpireTime = SystemClock.elapsedRealtime() + 30 * DateUtils.MINUTE_IN_MILLIS;
          if (mListener != null) {
            mListener.onADEvent(new ADEvent(AdEventType.AD_LOADED));
            // 快手没有缓存回调，这里一同回调
            mListener.onADEvent(new ADEvent(AdEventType.VIDEO_CACHE));
          }
        }
      }
    });
    new Handler(Looper.getMainLooper()).postDelayed(() -> mIsLoadOvertime = true, LOAD_COST_TIME);
  }

  // 2.展示激励视频广告，通过步骤1获取的KsRewardVideoAd对象，判断缓存有效，则设置监听并展示
  private void showRewardVideoAd(KsVideoPlayConfig videoPlayConfig) {
    if (mRewardVideoAd != null && mActivityReference.get() != null) {
      mRewardVideoAd
          .setRewardAdInteractionListener(new KsRewardVideoAd.RewardAdInteractionListener() {
            @Override
            public void onAdClicked() {
              if (mListener != null) {
                mListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
              }
            }

            @Override
            public void onPageDismiss() {
              if (mListener != null) {
                mListener.onADEvent(new ADEvent(AdEventType.AD_CLOSED));
              }
            }

            @Override
            public void onVideoPlayError(int code, int extra) {
              Log.d(TAG, "code = "+ code + "  extra = " + extra);
              onAdError(ErrorCode.VIDEO_PLAY_ERROR, code, ErrorCode.DEFAULT_ERROR_MESSAGE);
            }

            @Override
            public void onVideoPlayEnd() {
              if (mListener != null) {
                mListener.onADEvent(new ADEvent(AdEventType.VIDEO_COMPLETE));
              }
            }

            @Override
            public void onVideoSkipToEnd(long l) {
              Log.d(TAG, "onVideoSkipToEnd:  l = " + l);
            }

            @Override
            public void onVideoPlayStart() {
              if (mListener != null) {
                mListener.onADEvent(new ADEvent(AdEventType.AD_SHOW));
                // 由于快手没有曝光回调，所以曝光和 show 一块回调
                mListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED));
              }
            }

            @Override
            public void onRewardVerify() {
              if (mListener != null) {
                mListener.onADEvent(new ADEvent(AdEventType.AD_REWARD, new Object[]{""}));
              }
            }

            @Override
            public void onRewardStepVerify(int i, int i1) {
              Log.d(TAG, "onRewardStepVerify:  i = " + i + "  i1 = " + i1);
            }
          });
      mRewardVideoAd.showRewardVideoAd(mActivityReference.get(), videoPlayConfig);
    } else {
      Log.d(TAG, "showRewardVideoAd: 暂无可用激励视频广告，请等待缓存加载或者重新刷新");
    }
  }

  // 此处需要开发者自行配置，相关参数可以写在本地，或是通过构造函数中ext参数进行解析
  private KsVideoPlayConfig buildConfigHPShowScene() {
    mIsShowLandscape = isLandscape();
    KsVideoPlayConfig videoPlayConfig = new KsVideoPlayConfig.Builder()
        .showLandscape(mIsShowLandscape) // 横屏播放
        .build();
    return videoPlayConfig;
  }

  /**
   * 判断当前屏幕方向是否是横屏
   */
  private boolean isLandscape() {
    return mActivityReference.get() != null && mActivityReference.get().getResources() != null &&
        mActivityReference.get().getResources().getConfiguration() != null &&
        mActivityReference.get().getResources().getConfiguration().orientation ==
            Configuration.ORIENTATION_LANDSCAPE;
  }

  /**
   * @param errorCode 错误码
   */
  private void onAdError(int errorCode, Integer onlineErrorCode, String errorMessage) {
    if (mListener != null) {
      mListener.onADEvent(new ADEvent(AdEventType.AD_ERROR, new Object[]{errorCode}, onlineErrorCode, errorMessage));
    }
  }
}
