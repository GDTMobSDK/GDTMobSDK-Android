package com.qq.e.union.adapter.tt.interstitial;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.union.adapter.util.ErrorCode;

import java.util.List;

/**
 * 穿山甲插屏全屏-模板和插屏半屏-模板广告视频适配器
 * 作用：封装穿山甲，适配优量汇插屏全屏-模板和插屏半屏-模板广告
 */
public class TTExpressInterstitialAdAdapter extends TTInterstitialAdAdapter{

  private final String TAG = getClass().getSimpleName();
  private TTNativeExpressAd ttInteractionExpressAd;
  private float expressViewWidth = 500f;// 具体根据穿山甲广告位申请数值设置
  private float expressViewHeight = 500f; // 具体根据穿山甲广告位申请数值设置
  private boolean hasShowDownloadActive = false;

  public TTExpressInterstitialAdAdapter(Activity context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
  }

  // ExpressInterstitial的加载、展示逻辑和NativeInterstitial不同
  @Override
  public void loadAd() {
    Log.d(TAG, "loadAD");
    if (ttAdNative == null) {
      Log.i(TAG, "穿山甲 SDK 初始化错误，无法加载广告");
      return;
    }
    // 设置广告参数
    AdSlot adSlot = setAdSlotParams(new AdSlot.Builder()).build();

    ttAdNative.loadInteractionExpressAd(adSlot, new TTAdNative.NativeExpressAdListener() {
      @Override
      public void onError(int code, String message) {
        Log.d(TAG, "load error : " + code + ", " + message);
        if (unifiedInterstitialADListener != null) {
          unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.NO_AD,ErrorCode.NO_AD_FILL));
        }
      }

      @Override
      public void onNativeExpressAdLoad(List<TTNativeExpressAd> list) {
        if (list == null || list.size() == 0) {
          return;
        }
        ttInteractionExpressAd = list.get(0);
        try {
          Object object = ttInteractionExpressAd.getMediaExtraInfo().get("price");
          if (object != null) {
            ecpm = (int) object;
          }
        } catch (Exception e) {
          Log.d(TAG, "get ecpm error ", e);
        }
        try {
          Object o = ttInteractionExpressAd.getMediaExtraInfo().get("request_id");
          if (o != null) {
            mRequestId = o.toString();
          }
        } catch (Exception e) {
          Log.d(TAG, "get request_id error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: ecpm = " + ecpm);
        Log.d(TAG, "onAdDataSuccess: mRequestId = " + mRequestId);
        ttInteractionExpressAd.setExpressInteractionListener(new TTNativeExpressAd.AdInteractionListener() {
          @Override
          public void onAdClicked(View view, int type) {
            Log.d(TAG, "onAdClicked");
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
            }
          }

          @Override
          public void onAdShow(View view, int type) {
            Log.d(TAG, "onAdShow");
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_SHOW));
              // 由于穿山甲没有曝光回调，所以曝光和 open 一块回调
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED));
            }
          }

          @Override
          public void onRenderFail(View view, String msg, int code) {
            Log.d(TAG, "onRenderFail");
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.NO_AD,ErrorCode.NO_AD_FILL));
            }
          }

          @Override
          public void onAdDismiss() {
            Log.d(TAG, "onAdDismiss");
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_CLOSED));
            }
          }

          @Override
          public void onRenderSuccess(View view, float width, float height) {
            Log.d(TAG, "onRenderSuccess");
            if (unifiedInterstitialADListener != null) {
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_LOADED));
              unifiedInterstitialADListener.onADEvent(new ADEvent(AdEventType.AD_RENDER_SUCCESS));
            }
            mIsValid = true;
          }
        });
        ttInteractionExpressAd.render();
        bindAdDownloadListener();
      }
    });
    mIsValid = false;
  }

  private void bindAdDownloadListener() {
    if (ttInteractionExpressAd.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD) {
      ttInteractionExpressAd.setDownloadListener(new TTAppDownloadListener() {
        @Override
        public void onIdle() {
          Log.d(TAG, "点击开始下载");
        }

        @Override
        public void onDownloadActive(long totalBytes, long currBytes, String fileName, String appName) {
          if (!hasShowDownloadActive) {
            hasShowDownloadActive = true;
            Log.d(TAG, "下载中，点击暂停");
          }
        }

        @Override
        public void onDownloadPaused(long totalBytes, long currBytes, String fileName, String appName) {
          Log.d(TAG, "下载暂停，点击继续");
        }

        @Override
        public void onDownloadFailed(long totalBytes, long currBytes, String fileName, String appName) {
          Log.d(TAG, "下载失败，点击重新下载");
        }

        @Override
        public void onDownloadFinished(long totalBytes, String fileName, String appName) {
          Log.d(TAG, "点击安装");
        }

        @Override
        public void onInstalled(String fileName, String appName) {
          Log.d(TAG, "安装完成，点击图片打开");
        }
      });
    }
  }

  @Override
  public void show() {
    if (ttInteractionExpressAd != null && activityReference.get() != null) {
      ttInteractionExpressAd.showInteractionExpressAd(activityReference.get());
    }
    mIsValid = false;
  }

  @Override
  public void show(Activity act) {
    if (ttInteractionExpressAd != null && act != null) {
      ttInteractionExpressAd.showInteractionExpressAd(act);
    }
    mIsValid = false;
  }

  @Override
  public void destory() {
    if (ttInteractionExpressAd != null) {
      Log.d(TAG, "on destory");
      ttInteractionExpressAd.destroy();
    }
  }

  @Override
  protected AdSlot.Builder setAdSlotParams(AdSlot.Builder builder) {
    return builder
        // 广告位id
        .setCodeId(posId)
        // 请求广告数量为1到3条
        .setAdCount(1)
        // 期望模板广告view的size, 单位dp, 根据需求手动设置
        .setExpressViewAcceptedSize(expressViewWidth, expressViewHeight);
  }

  @Override
  protected AdSlot.Builder setFullScreenAdSlotParams(AdSlot.Builder builder) {
    return super.setAdSlotParams(builder)
        // 期望模板广告view的size, 单位dp, 根据需求手动设置
        .setExpressViewAcceptedSize(expressViewWidth, expressViewHeight);
  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {
    super.sendLossNotification(price, reason, adnId);
    if (ttInteractionExpressAd != null) {
      ttInteractionExpressAd.loss((double) price, String.valueOf(reason), adnId);
    }
  }

  @Override
  public void sendWinNotification(int price) {
    super.sendWinNotification(price);
    if (ttInteractionExpressAd != null) {
      ttInteractionExpressAd.win((double) price);
    }
  }

  @Override
  public void setBidECPM(int price) {
    super.setBidECPM(price);
    if (ttInteractionExpressAd != null) {
      ttInteractionExpressAd.setPrice((double) price);
    }
  }
}
