package com.qq.e.union.adapter.tt.unified;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.qq.e.ads.cfg.DownAPPConfirmPolicy;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.mediation.interfaces.BaseNativeUnifiedAd;
import com.qq.e.union.adapter.tt.util.LoadAdUtil;
import com.qq.e.union.adapter.tt.util.TTAdManagerHolder;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 穿山甲数据流广告适配器
 */
public class TTNativeUnifiedAdAdapter extends BaseNativeUnifiedAd implements TTAdManagerHolder.InitCallBack {

  private static final String TAG = TTNativeUnifiedAdAdapter.class.getSimpleName();

  private String posId;
  private TTAdNative mTTAdNative;
  private ADListener listener;
  private int width;
  private int height;
  private boolean isSupportDeepLink;
  private List<TTFeedAdDataAdapter> data;
  private String ecpmLevel;
  private int mCount;

  /**
   * @param ext 开发者自定义字段，是一个 json
   */
  public TTNativeUnifiedAdAdapter(Context context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    width = 640; // 开发者自行设置
    height = 320; // 开发者自行设置
    isSupportDeepLink = true;
    // step1：SDK 初始化
    TTAdManagerHolder.init(context, appId);
    // step2：创建 TTAdNative 对象
    mTTAdNative = TTAdManagerHolder.get().createAdNative(context);
    this.posId = posId;
  }

  @Override
  public void loadData(int count) {
    mCount = count;
    LoadAdUtil.load(this);
  }

  private void loadDataAfterInitSuccess(int count) {
    Log.d(TAG, "loadData: ");
    if (mTTAdNative == null) {
      Log.i(TAG, "穿山甲 SDK 初始化错误，无法加载广告");
      return;
    }
    data = null;
    // step3：创建广告请求参数AdSlot
    final AdSlot adSlot = new AdSlot.Builder()
            .setCodeId(posId)
            .setSupportDeepLink(isSupportDeepLink)
            .setImageAcceptedSize(width, height)
            .setAdCount(count)
            .build();

    // step4：请求广告，对请求回调的广告作渲染处理
    mTTAdNative.loadFeedAd(adSlot, new TTAdNative.FeedAdListener() {
      @Override
      public void onError(int code, String message) {
        Log.d(TAG, "onError: code: " + code + ", message: " +message);
        onAdFailed(ErrorCode.NO_AD_FILL, code, message);
      }

      @Override
      public void onFeedAdLoad(List<TTFeedAd> ads) {
        Log.d(TAG, "onFeedAdLoad: ads" + ads);
        if (ads == null || ads.isEmpty()) {
          onAdFailed(ErrorCode.NO_AD_FILL, ErrorCode.DEFAULT_ERROR_CODE, ErrorCode.DEFAULT_ERROR_MESSAGE);
          return;
        }
        onAdDataSuccess(ads);
      }
    });
  }

  @Override
  public void setAdListener(ADListener listener) {
    this.listener = listener;
  }

  /**
   * 加载广告成功回调
   *
   * @param ads 传入参数要非空且 notEmpty
   */
  private void onAdDataSuccess(@NonNull List<TTFeedAd> ads) {
    if (listener == null) {
      return;
    }
    int index = 0;
    List<TTFeedAdDataAdapter> result = new ArrayList<>();
    for (TTFeedAd ad : ads) {
      result.add(new TTFeedAdDataAdapter(ad));
    }
    data = result;
    listener.onADEvent(new ADEvent(AdEventType.AD_LOADED, new Object[]{result}));
  }

  private void onAdFailed(int errorCode, Integer onlineErrorCode, String errorMessage) {
    if (listener == null) {
      return;
    }
    listener.onADEvent(new ADEvent(AdEventType.NO_AD, new Object[]{errorCode}, onlineErrorCode, errorMessage));
  }

  @Override
  public int getECPM() {
    if (data == null || data.isEmpty()) {
      return Constant.VALUE_NO_ECPM;
    }
    return data.get(0).getECPM();
  }

  @Override
  public String getReqId() {
    if (data == null || data.isEmpty()) {
      return null;
    }
    Object o = data.get(0).getExtraInfo().get("request_id");
    if (o != null) {
      return o.toString();
    } else {
      return null;
    }
  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {
    super.sendLossNotification(price, reason, adnId);
    if (data == null || data.size() == 0) {
      return;
    }
    for (TTFeedAdDataAdapter adapter: data) {
      if (adapter != null) {
        adapter.sendLossNotification(price, reason, adnId);
      }
    }
  }

  @Override
  public void sendWinNotification(int price) {
    super.sendWinNotification(price);
    if (data == null || data.size() == 0) {
      return;
    }
    for (TTFeedAdDataAdapter adapter: data) {
      if (adapter != null) {
        adapter.sendWinNotification(price);
      }
    }
  }

  @Override
  public void setBidECPM(int price) {
    super.setBidECPM(price);
    if (data == null || data.size() == 0) {
      return;
    }
    for (TTFeedAdDataAdapter adapter: data) {
      if (adapter != null) {
        adapter.setBidECPM(price);
      }
    }
  }

  @Override
  public void setECPMLevel(String level) {
    if (data != null) {
      for (TTFeedAdDataAdapter adapter : data) {
        adapter.setEcpmLevel(level);
      }
    }
  }

  @Override
  public Map<String, Object> getExtraInfo() {
    Map<String, Object> map = new HashMap<>();
    map.put("request_id", getReqId());
    return map;
  }

  @Override
  public void onInitSuccess() {
    loadDataAfterInitSuccess(mCount);
  }

  @Override
  public void onInitFail() {
    Log.i(TAG, "穿山甲 SDK 初始化失败，无法加载广告");
    onAdFailed(ErrorCode.NO_AD_FILL, ErrorCode.DEFAULT_ERROR_CODE, ErrorCode.DEFAULT_ERROR_MESSAGE);
  }

  /**
   * ======================================================================
   * 以下方法暂不支持
   */

  @Override
  public void setDownAPPConfirmPolicy(DownAPPConfirmPolicy policy) { }


  @Override
  public void setCategories(List<String> categories) { }

  @Override
  public void setMinVideoDuration(int minVideoDuration) {
  }

  @Override
  public void setMaxVideoDuration(int maxVideoDuration) { }

}