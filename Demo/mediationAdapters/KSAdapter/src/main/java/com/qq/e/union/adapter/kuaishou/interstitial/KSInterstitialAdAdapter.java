package com.qq.e.union.adapter.kuaishou.interstitial;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsFullScreenVideoAd;
import com.kwad.sdk.api.KsInterstitialAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.kwad.sdk.api.KsVideoPlayConfig;
import com.kwad.sdk.api.model.MaterialType;
import com.qq.e.ads.cfg.DownAPPConfirmPolicy;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.mediation.interfaces.BaseInterstitialAd;
import com.qq.e.union.adapter.kuaishou.util.KSSDKInitUtil;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 快手插屏适配器
 */
public class KSInterstitialAdAdapter extends BaseInterstitialAd {
  private static final String TAG = KSInterstitialAdAdapter.class.getSimpleName();
  private final String mPosId;
  private KsFullScreenVideoAd mFullScreenVideoAd;
  private ADListener mUnifiedInterstitialADListener;
  private KsInterstitialAd mKsInterstitialAd;
  private Activity mActivity;
  private boolean mIsMute;

  public KSInterstitialAdAdapter(Activity context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    KSSDKInitUtil.init(context, appId);
    mPosId = posId;
    mActivity = context;
    Log.d(TAG, "KSInterstitialAdAdapter: appId = " + appId + ". posId = " + mPosId);
  }

  @Override
  public void show() {
    show(mActivity);
  }

  @Override
  public void showAsPopupWindow() {
    show();
  }

  @Override
  public void close() {/* 快手不支持此接口 */}

  @Override
  public void loadAd() {
    mKsInterstitialAd = null;
    Long posId = getPosId();
    if(posId == null){
      return;
    }
    KsScene scene =
        new KsScene.Builder(posId).build(); // 此为测试posId，请联系快手平台申请正式posId
    KsAdSDK.getLoadManager().loadInterstitialAd(scene,
        new KsLoadManager.InterstitialAdListener() {
          @Override
          public void onError(int code, String msg) {
            Log.d(TAG, "插屏广告请求失败" + code + msg);
            if (mUnifiedInterstitialADListener != null) {
              mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.NO_AD, ErrorCode.NO_AD_FILL, code, msg));
            }
          }

          @Override
          public void onRequestResult(int adNumber) {
            Log.d(TAG, "插屏广告请求填充个数 " + adNumber);
          }


          @Override
          public void onInterstitialAdLoad(@Nullable List<KsInterstitialAd> adList) {
            if (adList != null && adList.size() > 0) {
              mKsInterstitialAd = adList.get(0);
              Log.d(TAG, "插屏广告请求成功");
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_LOADED));
              }
            }
          }
        });
  }

  @Override
  public void showAsPopupWindow(Activity act) {
    show(act);
  }

  @Override
  public void show(Activity act) {
    KsVideoPlayConfig videoPlayConfig = new KsVideoPlayConfig.Builder()
        .videoSoundEnable(!mIsMute)
        .showLandscape(act.getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        .build();
    if (mKsInterstitialAd != null) {
      mKsInterstitialAd
          .setAdInteractionListener(new KsInterstitialAd.AdInteractionListener() {
            @Override
            public void onAdClicked() {
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
              }
              Log.d(TAG, "插屏广告点击");
            }

            @Override
            public void onAdShow() {
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_SHOW));
              }
              Log.d(TAG, "插屏广告曝光");
            }

            @Override
            public void onAdClosed() {
              Log.d(TAG, "用户点击插屏关闭按钮");
            }

            @Override
            public void onPageDismiss() {
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLOSED));
              }
              Log.i(TAG, "插屏广告关闭");
            }

            @Override
            public void onVideoPlayError(int code, int extra) {
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.VIDEO_ERROR));
              }
              Log.d(TAG, "插屏广告播放出错");
            }

            @Override
            public void onVideoPlayEnd() {
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.VIDEO_COMPLETE));
              }
              Log.d(TAG, "插屏广告播放完成");
            }

            @Override
            public void onVideoPlayStart() {
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.VIDEO_START));
              }
              Log.d(TAG, "插屏广告播放开始");
            }

            @Override
            public void onSkippedAd() {
              Log.d(TAG, "插屏广告播放跳过");
            }

          });
      mKsInterstitialAd.showInterstitialAd(act, videoPlayConfig);
    } else {
      Log.d(TAG, "暂无可用插屏广告，请等待缓存加载或者重新刷新");
    }
  }

  @Override
  public void setDownAPPConfirmPolicy(DownAPPConfirmPolicy policy) {/* 快手不支持此接口 */}

  @Override
  public void destory() {/* 快手不支持此接口 */}

  /**
   * 需要统一单位为分
   */
  @Override
  public int getECPM() {
    if (mFullScreenVideoAd != null) {
      return mFullScreenVideoAd.getECPM();
    }
    if (mKsInterstitialAd != null) {
      return mKsInterstitialAd.getECPM();
    }
    return Constant.VALUE_NO_ECPM;
  }

  @Override
  public String getECPMLevel() {
    return null; /* 快手不支持此接口 */
  }

  @Override
  public String getReqId() {
    return null;
  }

  @Override
  public Map<String, Object> getExtraInfo() {
    /* 快手不支持此接口 */
    return new HashMap<>();
  }

  @Override
  public void setVideoOption(VideoOption videoOption) {
    mIsMute = videoOption.getAutoPlayMuted();
  }

  @Override
  public void setMinVideoDuration(int minVideoDuration) {/* 快手不支持此接口 */}

  @Override
  public void setMaxVideoDuration(int maxVideoDuration) {/* 快手不支持此接口 */}

  @Override
  public int getAdPatternType() {
    if (mKsInterstitialAd == null && mFullScreenVideoAd == null) {
      return MaterialType.UNKNOWN;
    }
    int type = mKsInterstitialAd == null ? mFullScreenVideoAd.getMaterialType() :
        mKsInterstitialAd.getMaterialType();
    switch (type) {
      case MaterialType.VIDEO:
        return AdPatternType.NATIVE_VIDEO;
      case MaterialType.SINGLE_IMG:
        return AdPatternType.NATIVE_2IMAGE_2TEXT;
      case MaterialType.GROUP_IMG:
        return AdPatternType.NATIVE_3IMAGE;
      default:
        return MaterialType.UNKNOWN;
    }
  }

  @Override
  public int getVideoDuration() {
    return 0;/* 快手不支持此接口 */
  }

  @Override
  public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
    /* 快手不支持此接口 */
  }



  @Override
  public void setAdListener(ADListener listener) {
    mUnifiedInterstitialADListener = listener;
  }

  @Override
  public boolean isValid() {
    return (mFullScreenVideoAd != null && mFullScreenVideoAd.isAdEnable()) || mKsInterstitialAd != null;
  }

  @Override
  public void loadFullScreenAD() {
    mFullScreenVideoAd = null;
    Long posId = getPosId();
    if(posId == null){
      return;
    }
    KsScene scene =
        new KsScene.Builder(posId).build(); // 此为测试posId，请联系快手平台申请正式posId
    KsAdSDK.getLoadManager()
        .loadFullScreenVideoAd(scene, new KsLoadManager.FullScreenVideoAdListener() {
          @Override
          public void onError(int code, String msg) {
            Log.d(TAG, "全屏视频广告请求失败" + code + msg);
            if (mUnifiedInterstitialADListener != null) {
              mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.NO_AD, ErrorCode.NO_AD_FILL, code, msg));
            }
          }

          @Override
          public void onFullScreenVideoResult(@Nullable List<KsFullScreenVideoAd> adList) {
            if (adList != null) {
              Log.d(TAG, "全屏视频广告请求填充个数: " + adList.size());
            }
          }

          @Override
          public void onFullScreenVideoAdLoad(@Nullable List<KsFullScreenVideoAd> adList) {
            if (adList != null && adList.size() > 0) {
              mFullScreenVideoAd = adList.get(0);
              Log.d(TAG, "全屏视频广告请求成功");
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_LOADED));
              }
            }
          }
        });
  }

  @Override
  public void showFullScreenAD(Activity activity) {
    if (mFullScreenVideoAd != null && mFullScreenVideoAd.isAdEnable()) {
      mFullScreenVideoAd.setFullScreenVideoAdInteractionListener(
          new KsFullScreenVideoAd.FullScreenVideoAdInteractionListener() {
            @Override
            public void onAdClicked() {
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
              }
              Log.d(TAG, "全屏视频广告点击");
            }

            @Override
            public void onPageDismiss() {
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLOSED));
              }
              Log.d(TAG, "全屏视频广告关闭");
            }

            @Override
            public void onVideoPlayError(int code, int extra) {
              Log.d(TAG, "全屏视频广告播放出错");
            }

            @Override
            public void onVideoPlayEnd() {
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.VIDEO_COMPLETE));
              }
              Log.d(TAG, "全屏视频广告播放完成");
            }

            @Override
            public void onVideoPlayStart() {
              if (mUnifiedInterstitialADListener != null) {
                mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.VIDEO_START));
              }
              Log.d(TAG, "全屏视频广告播放开始");
            }

            @Override
            public void onSkippedVideo() {
              Log.d(TAG, "全屏视频广告播放跳过");
            }
          });
      mFullScreenVideoAd.showFullScreenVideoAd(activity, null);
      if (mUnifiedInterstitialADListener != null) {
        mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED));
      }
    } else {
      Log.d(TAG, "暂无可用全屏视频广告，请等待缓存加载或者重新刷新");
    }
  }

  private Long getPosId(){
    Long posId = null;
    try {
      posId = Long.parseLong(mPosId);
    } catch (Exception e) {
      e.printStackTrace();
      if (mUnifiedInterstitialADListener != null) {
        mUnifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.NO_AD, ErrorCode.NO_AD_FILL));
      }
    }
    return posId;
  }


  @Override
  public void setBidECPM(int price) {
    if(mKsInterstitialAd != null){
      mKsInterstitialAd.setBidEcpm(price);
    }
    if(mFullScreenVideoAd != null){
      mFullScreenVideoAd.setBidEcpm(price);
    }
  }

}
