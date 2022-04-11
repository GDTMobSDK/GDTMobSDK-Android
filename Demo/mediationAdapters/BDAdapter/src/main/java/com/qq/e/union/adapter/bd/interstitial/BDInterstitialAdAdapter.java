package com.qq.e.union.adapter.bd.interstitial;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.baidu.mobads.sdk.api.FullScreenVideoAd;
import com.baidu.mobads.sdk.api.InterstitialAd;
import com.baidu.mobads.sdk.api.InterstitialAdListener;
import com.qq.e.ads.cfg.DownAPPConfirmPolicy;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.mediation.interfaces.BaseInterstitialAd;
import com.qq.e.union.adapter.bd.util.BDAdManager;
import com.qq.e.union.adapter.util.CallbackUtil;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;

//百度插屏广告
public class BDInterstitialAdAdapter extends BaseInterstitialAd {

  private static final String KEY_PUBLISHERID = "publisherId";
  private static final String TAG = BDInterstitialAdAdapter.class.getSimpleName();

  // 插屏全屏广告
  private FullScreenVideoAd fullScreenVideoAd;
  // 插屏半屏广告
  private InterstitialAd interstitialAd;
  private ADListener unifiedInterstitialADListener;
  private Activity activity;
  private String posId; // 广告位id
  private int ecpm = Constant.VALUE_NO_ECPM;
  private final Handler mainHandler;

  public BDInterstitialAdAdapter(Activity context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    this.activity = context;
    this.posId = posId;
    BDAdManager.init(context, appId);
    mainHandler = new Handler(Looper.getMainLooper());
  }

  // show() 有遮罩, showAsPopupWindow() 无遮罩，在这里，一样的调用是为了兼容 demo 中 有无遮罩样式的展示
  @Override
  public void show() {
    if (interstitialAd != null) {
      interstitialAd.showAd();
    }
  }

  @Override
  public void showAsPopupWindow() {
    if (interstitialAd != null) {
      interstitialAd.showAd();
    }
  }

  @Override
  public void loadAd() {
    interstitialAd = new InterstitialAd(activity, posId);
    interstitialAd.setListener(new InterstitialAdListener() {

      @Override
      public void onAdReady() {//插屏广告加载完毕
        Log.i(TAG, "onAdReady.");
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_LOADED));
              if (CallbackUtil.hasRenderSuccessCallback(unifiedInterstitialADListener)) {
                unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_RENDER_SUCCESS));;
              }
            }
          }
        });
      }

      @Override
      public void onAdPresent() {//插屏广告展开时回调
        Log.i(TAG, "onAdPresent.");
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED));;
            }
          }
        });
      }

      @Override
      public void onAdClick(InterstitialAd interstitialAd) {//插屏广告点击时回调
        Log.i(TAG, "onAdClick.");
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));;
            }
          }
        });
      }

      @Override
      public void onAdDismissed() {//插屏广告关闭时回调
        Log.i(TAG, "onAdDismissed.");
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLOSED));;
            }
          }
        });
      }

      @Override
      public void onAdFailed(String reason) {//广告加载失败
        Log.i(TAG, "onAdFailed." + reason);
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.NO_AD, ErrorCode.NO_AD_FILL,
                  ErrorCode.DEFAULT_ERROR_CODE, reason));
            }
          }
        });
      }
    });
    interstitialAd.loadAd();
  }

  @Override
  public void showAsPopupWindow(Activity act) {
    if (interstitialAd != null && interstitialAd.isAdReady()) {
      interstitialAd.showAd();
    }
  }

  @Override
  public void show(Activity act) {
    if (interstitialAd != null && interstitialAd.isAdReady()) {
      interstitialAd.showAd();
    }
  }

  @Override
  public void destory() {
    if (interstitialAd != null) {
      interstitialAd.destroy();
    }
  }

  @Override
  public void setAdListener(ADListener listener) {
    this.unifiedInterstitialADListener = listener;
  }

  @Override
  public boolean isValid() {
    return (interstitialAd != null && interstitialAd.isAdReady())
        || (fullScreenVideoAd != null && fullScreenVideoAd.isReady());
  }

  @Override
  public void loadFullScreenAD() {
    fullScreenVideoAd = new FullScreenVideoAd(activity, posId,
        new FullScreenVideoAd.FullScreenVideoAdListener() {
      @Override
      public void onAdShow() {
        Log.d(TAG, "onAdShow.");
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {

              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED));
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_SHOW));
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.VIDEO_START));
            }
          }
        });
      }

      @Override
      public void onAdClick() {
        Log.d(TAG, "onAdClick.");
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));;
            }
          }
        });
      }

      @Override
      public void onAdClose(float v) {
        Log.d(TAG, "onAdClose.");
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLOSED));;
            }
          }
        });
      }

      @Override
      public void onAdFailed(String s) {
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            Log.d(TAG, "onAdFailed: " + s);
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.NO_AD, ErrorCode.NO_AD_FILL,
                  ErrorCode.DEFAULT_ERROR_CODE, s));
            }
          }
        });
      }

      @Override
      public void onVideoDownloadSuccess() {
        Log.d(TAG, "onVideoDownloadSuccess.");
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.VIDEO_CACHE));
            }
          }
        });
      }

      @Override
      public void onVideoDownloadFailed() {
        Log.d(TAG, "onVideoDownloadFailed.");
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.NO_AD, ErrorCode.VIDEO_DOWNLOAD_FAIL));
            }
          }
        });
      }

      @Override
      public void playCompletion() {
        Log.d(TAG, "playCompletion.");
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.VIDEO_COMPLETE));
            }
          }
        });
      }

      @Override
      public void onAdSkip(float v) {
        Log.d(TAG, "onAdSkip.");
      }

      @Override
      public void onAdLoaded() {
        try {
          ecpm = Integer.parseInt(fullScreenVideoAd.getECPMLevel());
        } catch (Exception e) {
          Log.d(TAG, "get ecpm error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: ecpm = " + ecpm);
        mainHandler.post(new Runnable() {
          @Override
          public void run() {
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_LOADED));
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_RENDER_SUCCESS));
            }
          }
        });
      }
    });
    fullScreenVideoAd.load();
  }

  @Override
  public void showFullScreenAD(Activity activity) {
    if (fullScreenVideoAd != null) {
      fullScreenVideoAd.show();
    }
  }

  /******************************以下方法暂不支持*****************************/

  @Override
  public void close() {
    /* 百度不支持此接口 */
  }

  @Override
  public int getECPM() {
    return ecpm;
  }

  @Override
  public void setDownAPPConfirmPolicy(DownAPPConfirmPolicy policy) {
    /* 百度不支持此接口 */
  }

  @Override
  public String getECPMLevel() {
    /* 百度不支持此接口 */
    return null;
  }

  @Override
  public String getReqId() {
    /* 百度不支持此接口 */
    return null;
  }

  @Override
  public void setVideoOption(VideoOption videoOption) {
    /* 百度不支持此接口 */
  }

  @Override
  public void setMinVideoDuration(int minVideoDuration) {
    /* 百度不支持此接口 */
  }

  @Override
  public void setMaxVideoDuration(int maxVideoDuration) {
    /* 百度不支持此接口 */
  }


  @Override
  public int getAdPatternType() {
    /* 百度不支持此接口 */
    return 0;
  }


  @Override
  public int getVideoDuration() {
    /* 百度不支持此接口 */
    return 0;
  }

  @Override
  public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
    /* 百度不支持此接口 */
  }
}
