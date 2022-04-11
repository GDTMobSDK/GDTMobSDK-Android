package com.qq.e.union.adapter.kuaishou.unified;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsNativeAd;
import com.kwad.sdk.api.KsScene;
import com.qq.e.ads.cfg.DownAPPConfirmPolicy;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.mediation.interfaces.BaseNativeUnifiedAd;
import com.qq.e.union.adapter.kuaishou.util.KSSDKInitUtil;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 快手原生广告管理者适配器
 */
public class KSNativeAdAdapter extends BaseNativeUnifiedAd {
  private static final String TAG = "KSNativeAdAdapter";
  private ADListener mADListener;
  private long mPosId;
  private List<KSNativeAdDataAdapter> mKSNativeAdDataAdapters;
  private int mEcpm = Constant.VALUE_NO_ECPM;

  public KSNativeAdAdapter(Context context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    mPosId = Long.parseLong(posId);
    KSSDKInitUtil.init(context, appId);
  }

  @Override
  public void setDownAPPConfirmPolicy(DownAPPConfirmPolicy policy) {

  }

  @Override
  public void loadData(int count) {
    KsScene scene = new KsScene.Builder(mPosId).build();// 此为测试posId，请联系快手平台申请正式posId
    scene.setAdNum(count); // 支持返回多条广告，默认1条，最多5条，参数范围1-5
    KsAdSDK.getLoadManager().loadNativeAd(scene, new KsLoadManager.NativeAdListener() {
      @Override
      public void onError(int code, String msg) {
        Log.d(TAG, "onError: " + code + ", msg: " + msg);
        onAdFailed(code, msg);
      }

      @Override
      public void onNativeAdLoad(@Nullable List<KsNativeAd> adList) {
        if (adList == null || adList.isEmpty()) {
          Log.d(TAG, "onNativeAdLoad: no ad");
          onAdFailed(ErrorCode.DEFAULT_ERROR_CODE, ErrorCode.DEFAULT_ERROR_MESSAGE);
        } else {
          onAdSuccess(adList);
        }
      }
    });
  }

  private void onAdSuccess(List<KsNativeAd> adList) {
    if (mADListener == null) {
      return;
    }
    int index = 0;
    List<KSNativeAdDataAdapter> result = new ArrayList<>();
    for (KsNativeAd ksNativeAd : adList) {
      if (index == 0) {
        mEcpm = ksNativeAd.getECPM();
        Log.d(TAG, "onAdSuccess: ecpm = " + mEcpm);
      }
      index++;
      result.add(new KSNativeAdDataAdapter(ksNativeAd));
    }
    mKSNativeAdDataAdapters = result;
    mADListener.onADEvent(new ADEvent(AdEventType.AD_LOADED, new Object[]{result}));
  }


  @Override
  public void setAdListener(ADListener listener) {
    mADListener = listener;
  }

  private void onAdFailed(int errorCode, String errorMessage) {
    if (mADListener == null) {
      return;
    }
    mADListener.onADEvent(new ADEvent(AdEventType.NO_AD, new Object[]{errorCode}, errorCode,
        errorMessage));
  }

  @Override
  public void setECPMLevel(String level) {
    if (mKSNativeAdDataAdapters != null) {
      for (KSNativeAdDataAdapter adapter : mKSNativeAdDataAdapters) {
        adapter.setEcpmLevel(level);
      }
    }
  }

  @Override
  public void setCategories(List<String> categories) {
    // 快手不支持此方法
  }

  @Override
  public void setMinVideoDuration(int minVideoDuration) {
    // 快手不支持此方法
  }

  @Override
  public void setMaxVideoDuration(int maxVideoDuration) {
    // 快手不支持此方法
  }

  @Override
  public int getECPM() {
    return mEcpm;
  }

  @Override
  public String getReqId() {
    return null;
  }


}
