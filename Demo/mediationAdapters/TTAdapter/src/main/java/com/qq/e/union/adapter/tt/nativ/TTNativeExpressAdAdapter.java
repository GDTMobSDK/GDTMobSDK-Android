package com.qq.e.union.adapter.tt.nativ;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTNativeExpressAd;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.mediation.interfaces.BaseNativeExpressAd;
import com.qq.e.union.adapter.tt.util.TTAdManagerHolder;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;

import java.util.ArrayList;
import java.util.List;

public class TTNativeExpressAdAdapter extends BaseNativeExpressAd {

  private static final String TAG = TTNativeExpressAdAdapter.class.getSimpleName();

  private ADListener mListener;
  private final TTAdNative mTTAdNative;
  private final String mPosId;
  private final Context mContext;
  private final int mAdWidth;
  private final int mAdHeight;
  private List<TTNativeExpressAdDataAdapter> mTTNativeExpressAdDataAdapters;
  private int mEcpm = Constant.VALUE_NO_ECPM;
  private String mRequestId;

  public TTNativeExpressAdAdapter(Context context, ADSize adSize, String appId, String posId,
                                  String ext) {
    super(context, adSize, appId, posId, ext);
    TTAdManagerHolder.init(context, appId);
    mTTAdNative = TTAdManagerHolder.get().createAdNative(context);
    mPosId = posId;
    mContext = context;
    mAdWidth = adSize.getWidth();
    mAdHeight = Math.max(adSize.getHeight(), 0);
  }

  @Override
  public void setAdListener(ADListener adListener) {
    mListener = adListener;
  }

  @Override
  public void loadAD(int count) {
    loadAD(count, null);
  }

  @Override
  public void loadAD(int count, LoadAdParams params) {
    loadExpressAd(count);
  }

  private void loadExpressAd(int count) {
    Log.d(TAG, "loadData: ");
    if (mTTAdNative == null) {
      Log.i(TAG, "穿山甲 SDK 初始化错误，无法加载广告");
      return;
    }
    // 创建广告请求参数AdSlot,具体参数含义参考文档
    AdSlot adSlot = new AdSlot.Builder()
        .setCodeId(mPosId) // 广告位id
        .setAdCount(Math.min(count, 3)) // 请求广告数量为1到3条
        .setExpressViewAcceptedSize(mAdWidth, mAdHeight) // 期望模板广告view的size, 单位dp, 若高度设置为0, 则高度会自适应
        .build();

    // 请求广告
    mTTAdNative.loadNativeExpressAd(adSlot, new TTAdNative.NativeExpressAdListener() {
      @Override
      public void onError(int code, String message) {
        Log.d(TAG, "onError: code: " + code + ", message: " + message);
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.NO_AD, new Object[]{ErrorCode.NO_AD_FILL}));
      }

      @Override
      public void onNativeExpressAdLoad(List<TTNativeExpressAd> ads) {
        onAdDataSuccess(ads);
      }
    });
  }

  /**
   * 加载广告成功回调
   *
   * @param ads 传入参数要非空且 notEmpty
   */
  private void onAdDataSuccess(@NonNull List<TTNativeExpressAd> ads) {
    if (mListener == null) {
      return;
    }
    if (ads == null || ads.size() == 0) {
      mListener.onADEvent(new ADEvent(AdEventType.NO_AD, new Object[]{ErrorCode.NO_AD_FILL}));
    }

    mTTNativeExpressAdDataAdapters = new ArrayList<>();
    int index = 0;
    for (TTNativeExpressAd ad : ads) {
      TTNativeExpressAdDataAdapter adDataAdapter = new TTNativeExpressAdDataAdapter(mContext, ad);
      adDataAdapter.setAdListener(mListener);
      mTTNativeExpressAdDataAdapters.add(adDataAdapter);
      if (index == 0) {
        try {
          mEcpm = (int) ad.getMediaExtraInfo().get("price");
        } catch (Exception e) {
          Log.d(TAG, "get ecpm error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: ecpm = " + mEcpm);
        try {
          Object o = ad.getMediaExtraInfo().get("request_id");
          if (o != null) {
            mRequestId = o.toString();
          }
        } catch (Exception e) {
          Log.d(TAG, "get request_id error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: mRequestId = " + mRequestId);
      }
      index++;
    }
    mListener.onADEvent(new ADEvent(AdEventType.AD_LOADED, new Object[]{mTTNativeExpressAdDataAdapters}));
  }

  @Override
  public void setECPMLevel(String ecpmLevel) {
    if (mTTNativeExpressAdDataAdapters != null) {
      for (TTNativeExpressAdDataAdapter adDataAdapter : mTTNativeExpressAdDataAdapters) {
        adDataAdapter.getBoundData().setECPMLevel(ecpmLevel);
      }
    }
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
    if (mTTNativeExpressAdDataAdapters == null || mTTNativeExpressAdDataAdapters.size() == 0) {
      return;
    }
    for (TTNativeExpressAdDataAdapter adapter: mTTNativeExpressAdDataAdapters) {
      if (adapter != null) {
        adapter.sendLossNotification(price, reason, adnId);
      }
    }
  }

  @Override
  public void sendWinNotification(int price) {
    super.sendWinNotification(price);
    if (mTTNativeExpressAdDataAdapters == null || mTTNativeExpressAdDataAdapters.size() == 0) {
      return;
    }
    for (TTNativeExpressAdDataAdapter adapter: mTTNativeExpressAdDataAdapters) {
      if (adapter != null) {
        adapter.sendWinNotification(price);
      }
    }
  }

  @Override
  public void setBidECPM(int price) {
    super.setBidECPM(price);
    if (mTTNativeExpressAdDataAdapters == null || mTTNativeExpressAdDataAdapters.size() == 0) {
      return;
    }
    for (TTNativeExpressAdDataAdapter adapter: mTTNativeExpressAdDataAdapters) {
      if (adapter != null) {
        adapter.setBidECPM(price);
      }
    }
  }


  /**
   * ======================================================================
   * 以下方法暂不支持
   */

  @Override
  public void setVideoOption(VideoOption videoOption) {

  }

  @Override
  public void setMinVideoDuration(int minVideoDuration) {

  }

  @Override
  public void setMaxVideoDuration(int maxVideoDuration) {

  }

}
