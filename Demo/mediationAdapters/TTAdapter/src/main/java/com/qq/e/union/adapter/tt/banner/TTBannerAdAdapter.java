package com.qq.e.union.adapter.tt.banner;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdDislike;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.mediation.interfaces.BaseBannerAd;
import com.qq.e.union.adapter.tt.util.LoadAdUtil;
import com.qq.e.union.adapter.tt.util.TTAdManagerHolder;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;
import com.qq.e.union.adapter.util.PxUtils;

import java.util.List;

/**
 * 穿山甲 Banner Adapter
 * 宽高比 6.4 : 1 测试广告位为 901121223
 */

public class TTBannerAdAdapter extends BaseBannerAd implements TTAdManagerHolder.InitCallBack {

  private static final String TAG = "TTBannerAdAdapter";
  private final String mPosId;
  private final Activity mContext;
  private TTAdNative mTTAdNative;
  private TTNativeExpressAd mTTAd;
  private View mAdView;
  private int mHeight = 320;
  private int mWidth = 50;
  private ADListener mBannerADListener;
  private int mEcpm = Constant.VALUE_NO_ECPM;

  public TTBannerAdAdapter(Activity context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    TTAdManagerHolder.init(context, appId);
    // 创建TTAdNative对象，createAdNative(Context context) banner广告context需要传入Activity对象
    mTTAdNative = TTAdManagerHolder.get().createAdNative(context);
    mPosId = posId;
    mContext = context;
  }

  @Override
  public void loadAD() {
    LoadAdUtil.load(this);
  }

  private void loadADAfterInitSuccess() {
    //step:创建广告请求参数AdSlot,具体参数含义参考文档
    AdSlot adSlot = new AdSlot.Builder()
        .setCodeId(mPosId) //广告位id
        .setAdCount(1) //请求广告数量为1到3条
        .setExpressViewAcceptedSize(mWidth, mHeight) //期望模板广告view的size,单位dp
        .build();
    // 请求广告，对请求回调的广告作渲染处理
    mTTAdNative.loadBannerExpressAd(adSlot, new TTAdNative.NativeExpressAdListener() {
      @Override
      public void onError(int code, String message) {
        Log.w(TAG, "onError: " + code + ", " + message);
        if (mBannerADListener != null) {
          mBannerADListener.onADEvent(new ADEvent(AdEventType.NO_AD,ErrorCode.NO_AD_FILL, code, message));
        }
      }

      @Override
      public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
        if (ads == null || ads.size() == 0) {
          Log.d(TAG, "loadAd onNativeAdLoad FAILED : no ads");
          if (mBannerADListener != null) {
            mBannerADListener.onADEvent(new ADEvent(AdEventType.NO_AD,ErrorCode.NO_AD_FILL));
          }
          return;
        }
        mTTAd = ads.get(0);
        try {
          mEcpm = (int) mTTAd.getMediaExtraInfo().get("price");
        } catch (Exception e) {
          Log.d(TAG, "get ecpm error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: ecpm = " + mEcpm);
        mTTAd.render();
        bindAdListener(mTTAd);
      }
    });
  }

  @Override
  public boolean isValid() {
    return true;
  }

  private void bindAdListener(TTNativeExpressAd ad) {
    ad.setExpressInteractionListener(new TTNativeExpressAd.ExpressAdInteractionListener() {
      @Override
      public void onAdClicked(View view, int type) {
        Log.d(TAG, "onAdClicked: ");
        if (mBannerADListener != null) {
          mBannerADListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
        }
      }

      @Override
      public void onAdShow(View view, int type) {
        Log.d(TAG, "onAdShow: ");
        if (mBannerADListener != null) {
          mBannerADListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED));
        }
      }

      @Override
      public void onRenderFail(View view, String msg, int code) {
        Log.w(TAG, "onRenderFail: " + msg + " code:" + code);
      }

      @Override
      public void onRenderSuccess(View view, float width, float height) {
        //返回view的宽高 单位 dp
        Log.d(TAG, "onRenderSuccess: ");
        mAdView = view;
        if (mBannerADListener != null) {
          mBannerADListener.onADEvent(new ADEvent(AdEventType.AD_LOADED));;
        }
      }
    });
    //dislike设置
    bindDislike(ad);
  }
  @Override
  public int getECPM() {
    return mEcpm;
  }

  @Override
  public String getReqId() {
    String reqId = null;
    try {
      reqId = (String )mTTAd.getMediaExtraInfo().get("request_id");
    } catch (Exception e) {
      Log.d(TAG, "getReqId: " + e.toString());
    }
    return reqId;
  }

  /**
   * 设置广告的不喜欢, 注意：强烈建议设置该逻辑，如果不设置dislike处理逻辑，则模板广告中的 dislike区域不响应dislike事件。
   */
  private void bindDislike(TTNativeExpressAd ad) {
    //使用默认模板中默认dislike弹出样式
    ad.setDislikeCallback(mContext, new TTAdDislike.DislikeInteractionCallback() {
      @Override
      public void onShow() {

      }

      @Override
      public void onSelected(int position, String value, boolean enforce) {

        if (enforce) {
          Log.w(TAG, "dislick onSelected: 模板Banner 穿山甲sdk强制将view关闭了");
        }
        if (mBannerADListener != null) {
          mBannerADListener.onADEvent(new ADEvent(AdEventType.AD_CLOSED));;
        }
      }

      @Override
      public void onCancel() {
        Log.d(TAG, "dislike onCancel: ");
      }

    });
  }

  @Override
  public void destroy() {
    if (mTTAd != null) {
      mTTAd.destroy();
    }
  }

  @Override
  public void setAdListener(ADListener adListener) {
    mBannerADListener = adListener;
  }

  @Override
  public void setAdSize(int widthPx, int heightPx) {
    mHeight = PxUtils.pxToDp(mContext, heightPx);
    mWidth = PxUtils.pxToDp(mContext, widthPx);
    Log.d(TAG, "setAdSize: mHeight = " + mHeight + ", mWidth = " + mWidth);
  }

  @Override
  public View getAdView() {
    return mAdView;
  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {
    super.sendLossNotification(price, reason, adnId);
    if (mTTAd != null) {
      mTTAd.loss((double) price, String.valueOf(reason), adnId);
    }
  }

  @Override
  public void sendWinNotification(int price) {
    super.sendWinNotification(price);
    if (mTTAd != null) {
      mTTAd.win((double) price);
    }
  }

  @Override
  public void setBidECPM(int price) {
    super.setBidECPM(price);
    if (mTTAd != null) {
      mTTAd.setPrice((double) price);
    }
  }

  @Override
  public void onInitSuccess() {
    loadADAfterInitSuccess();
  }

  @Override
  public void onInitFail() {
    Log.i(TAG, "穿山甲 SDK 初始化失败，无法加载广告");
    if (mBannerADListener != null) {
      mBannerADListener.onADEvent(new ADEvent(AdEventType.NO_AD, ErrorCode.NO_AD_FILL,
          ErrorCode.DEFAULT_ERROR_CODE, ErrorCode.DEFAULT_ERROR_MESSAGE));
    }
  }
}
