package com.qq.e.union.adapter.bd.unified;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.util.Log;


import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.qq.e.ads.cfg.DownAPPConfirmPolicy;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.mediation.interfaces.BaseNativeUnifiedAd;
import com.qq.e.union.adapter.bd.util.BDAdManager;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import static com.qq.e.comm.adevent.AdEventType.AD_CLICKED;
import static com.qq.e.comm.adevent.AdEventType.VIDEO_ERROR;
import static com.qq.e.comm.adevent.AdEventType.VIDEO_CACHE;

/**
 * 百度自渲染广告适配器
 */
public class BDNativeUnifiedAdAdapter extends BaseNativeUnifiedAd implements BaiduNativeManager.FeedAdListener {

  private static final String TAG = BDNativeUnifiedAdAdapter.class.getSimpleName();
  private final Handler mainHandler;
  private Context context;
  // 广告位id
  private String posId;
  private ADListener listener;
  private List<BDNativeResponseAdapter> data;
  private boolean isVideoAd;

  /**
   * @param ext 开发者自定义字段，是一个 json
   */
  public BDNativeUnifiedAdAdapter(Context context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    BDAdManager.init(context, appId);
    this.context = context;
    this.posId = posId;
    try {
      // ext 需要在msdk平台中配置。
      JSONObject object = new JSONObject(ext);
      this.isVideoAd = object.optBoolean("isVideo");
    } catch (JSONException e) {
      e.printStackTrace();
    }
    mainHandler = new Handler(Looper.getMainLooper());
  }

  @Override
  public void loadData(int count) {
    Log.d(TAG, "loadData.");
    data = null;

    // 若与百度进行相关合作，可使用如下接口上报广告的上下文
    RequestParameters requestParameters = new RequestParameters.Builder()
        .downloadAppConfirmPolicy(RequestParameters.DOWNLOAD_APP_CONFIRM_ONLY_MOBILE)
        // 用户维度：用户性别，取值：0-unknown，1-male，2-female
//        .addExtra(ArticleInfo.USER_SEX, "1")
        // 用户维度：收藏的小说ID，最多五个ID，且不同ID用'/分隔'
//        .addExtra(ArticleInfo.FAVORITE_BOOK, "这是小说的名称1/这是小说的名称2/这是小说的名称3")
        // 内容维度：小说、文章的名称
//        .addExtra(ArticleInfo.PAGE_TITLE, "测试书名")
        // 内容维度：小说、文章的ID
//        .addExtra(ArticleInfo.PAGE_ID, "10930484090")
        // 内容维度：小说分类，一级分类和二级分类用'/'分隔
//        .addExtra(ArticleInfo.CONTENT_CATEGORY, "一级分类/二级分类")
        // 内容维度：小说、文章的标签，最多10个，且不同标签用'/分隔'
//        .addExtra(ArticleInfo.CONTENT_LABEL, "标签1/标签2/标签3")
        .build();

    BaiduNativeManager nativeManager = new BaiduNativeManager(context, posId);

    nativeManager.loadFeedAd(requestParameters, this);
  }

  public void setAdListener(ADListener listener) {
    this.listener = listener;
  }


  @Override
  public void onNativeFail(int errorCode, String message) {
    Log.d(TAG, "onNativeFail: " + errorCode + ", message:" + message);
    onAdFailed(ErrorCode.NO_AD_FILL, errorCode, message);
  }

  @Override
  public void onNoAd(int i, String s) {
    Log.w(TAG, "onLoadFail reason:" + i + "errorCode:" + s);
    onAdFailed(ErrorCode.NO_AD_FILL, i, s);
  }

  @Override
  public void onNativeLoad(List<NativeResponse> arg0) {
    Log.d(TAG, "onNativeLoad: " + arg0);
    if (arg0 == null || arg0.isEmpty()) {
      onAdFailed(ErrorCode.NO_AD_FILL, ErrorCode.DEFAULT_ERROR_CODE, ErrorCode.DEFAULT_ERROR_MESSAGE);
      return;
    }
    onAdDataSuccess(arg0);
  }


  @Override
  public void onVideoDownloadSuccess() {
    Log.d(TAG, "onVideoDownloadSuccess.");
    fireAdEvent(VIDEO_CACHE, new Object[]{0});
  }

  @Override
  public void onVideoDownloadFailed() {
    Log.d(TAG, "onVideoDownloadFailed.");
    fireAdEvent(VIDEO_ERROR);
  }

  @Override
  public void onLpClosed() {
    Log.i(TAG, "onLpClosed.");
  }

  /**
   * 加载广告成功回调
   *
   * @param ads 传入的参数要非空且 notEmpty
   */
  private void onAdDataSuccess(@NonNull List<NativeResponse> ads) {
    if (listener == null) {
      return;
    }
    List<BDNativeResponseAdapter> result = new ArrayList<>();
    for (NativeResponse ad : ads) {
      result.add(new BDNativeResponseAdapter(ad));
    }
    data = result;
    fireAdEvent(AdEventType.AD_LOADED, new Object[]{result});
  }

  /**
   * @param errorCode 错误码
   */
  private void onAdFailed(int errorCode, Integer onlineErrorCode, String errorMessage) {
    fireAdEvent(AdEventType.NO_AD, new Object[]{errorCode}, onlineErrorCode, errorMessage);
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
    return null;
  }

  private void fireAdEvent(int eventId, Object... params) {
    mainHandler.post(new Runnable() {
      @Override
      public void run() {
        if (listener != null) {
          listener.onADEvent(new ADEvent(eventId, params));
        }
      }
    });
  }

  @Override
  public void setECPMLevel(String level) {
    if (data != null) {
      for (BDNativeResponseAdapter adapter : data) {
        adapter.setEcpmLevel(level);
      }
    }
  }

  /**
   * ======================================================================
   * 以下方法暂不支持
   */

  @Override
  public void setDownAPPConfirmPolicy(DownAPPConfirmPolicy policy) {
  }

  @Override
  public void setCategories(List<String> categories) {
  }

  @Override
  public void setMinVideoDuration(int minVideoDuration) {
  }

  @Override
  public void setMaxVideoDuration(int maxVideoDuration) {
  }
}
