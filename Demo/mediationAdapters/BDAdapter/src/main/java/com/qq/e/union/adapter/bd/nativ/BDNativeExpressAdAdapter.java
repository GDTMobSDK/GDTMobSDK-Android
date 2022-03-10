package com.qq.e.union.adapter.bd.nativ;

import android.content.Context;
import android.support.annotation.NonNull;
import android.util.Log;

import com.baidu.mobads.sdk.api.ArticleInfo;
import com.baidu.mobads.sdk.api.BaiduNativeManager;
import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.RequestParameters;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.mediation.interfaces.BaseNativeExpressAd;
import com.qq.e.union.adapter.bd.util.BDAdManager;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;

import java.util.ArrayList;
import java.util.List;

/**
 * 百度 模版信息流 Adapter
 * 测试广告位为 6481012
 */
public class BDNativeExpressAdAdapter extends BaseNativeExpressAd {

  private static final String TAG = BDNativeExpressAdAdapter.class.getSimpleName();

  private ADListener mListener;
  private final Context mContext;
  private BaiduNativeManager mBaiduNativeManager;
  private List<BDNativeExpressAdDataAdapter> mBDNativeExpressAdDataAdapters;
  private int mEcpm = Constant.VALUE_NO_ECPM;

  public BDNativeExpressAdAdapter(Context context, ADSize adSize, String appId, String posId,
                                  String ext) {
    super(context, adSize, appId, posId, ext);
    BDAdManager.init(context, appId);
    mBaiduNativeManager = new BaiduNativeManager(context, posId);
    mContext = context;
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
    if (mBaiduNativeManager == null) {
      Log.i(TAG, "穿山甲 SDK 初始化错误，无法加载广告");
      return;
    }

    // 构建请求参数
    RequestParameters requestParameters = new RequestParameters.Builder()
        .downloadAppConfirmPolicy(RequestParameters.DOWNLOAD_APP_CONFIRM_ONLY_MOBILE)
        /**
         * 【信息流传参】传参功能支持的参数见ArticleInfo类，各个参数字段的描述和取值可以参考如下注释
         * 注意：所有参数的总长度(不包含key值)建议控制在150字符内，避免因超长发生截断，影响信息的上报
         * 注意：【高】【中】【低】代表参数的优先级，请尽量提供更多高优先级参数
         */
        // 【高】通用信息：用户性别，取值：0-unknown，1-male，2-female
        .addExtra(ArticleInfo.USER_SEX, "1")
        // 【高】最近阅读：小说、文章的名称
        .addExtra(ArticleInfo.PAGE_TITLE, "测试书名")
        // 【高】最近阅读：小说、文章的ID
        .addExtra(ArticleInfo.PAGE_ID, "10930484090")
        // 【高】书籍信息：小说分类，取值：一级分类和二级分类用'/'分隔
        .addExtra(ArticleInfo.CONTENT_CATEGORY, "一级分类/二级分类")
        // 【高】书籍信息：小说、文章的标签，取值：最多10个，且不同标签用'/分隔'
        .addExtra(ArticleInfo.CONTENT_LABEL, "标签1/标签2/标签3")
        // 【中】通用信息：收藏的小说ID，取值：最多五个ID，且不同ID用'/分隔'
        .addExtra(ArticleInfo.FAVORITE_BOOK, "这是小说的名称1/这是小说的名称2/这是小说的名称3")
        // 【中】最近阅读：一级目录，格式：章节名，章节编号
        .addExtra(ArticleInfo.FIRST_LEVEL_CONTENTS, "测试一级目录，001")
        // 【低】书籍信息：章节数，取值：32位整数，默认值0
        .addExtra(ArticleInfo.CHAPTER_NUM, "12345")
        // 【低】书籍信息：连载状态，取值：0 表示连载，1 表示完结，默认值0
        .addExtra(ArticleInfo.PAGE_SERIAL_STATUS, "0")
        // 【低】书籍信息：作者ID/名称
        .addExtra(ArticleInfo.PAGE_AUTHOR_ID, "123456")
        // 【低】最近阅读：二级目录，格式：章节名，章节编号
        .addExtra(ArticleInfo.SECOND_LEVEL_CONTENTS, "测试二级目录，2000")
        .build();

    // 请求广告
    mBaiduNativeManager.loadFeedAd(requestParameters, new BaiduNativeManager.FeedAdListener() {
      @Override
      public void onNativeLoad(List<NativeResponse> nativeResponses) {
        onAdDataSuccess(nativeResponses);
        Log.i(TAG, "onNativeLoad:" +
            (nativeResponses != null ? nativeResponses.size() : null));
      }

      @Override
      public void onNoAd(int code, String msg) {
        Log.d(TAG, "onError: code: " + code + ", message: " + msg);
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.NO_AD, new Object[]{ErrorCode.NO_AD_FILL}));
      }

      @Override
      public void onNativeFail(int errorCode, String message) {
        Log.d(TAG, "onError: code: " + errorCode + ", message: " + message);
        if (mListener == null) {
          return;
        }
        mListener.onADEvent(new ADEvent(AdEventType.NO_AD, new Object[]{ErrorCode.NO_AD_FILL}));
      }

      @Override
      public void onVideoDownloadSuccess() {

      }

      @Override
      public void onVideoDownloadFailed() {

      }

      @Override
      public void onLpClosed() {
        Log.i(TAG, "onLpClosed.");
      }
    });
  }

  /**
   * 加载广告成功回调
   *
   * @param ads 传入参数要非空且 notEmpty
   */
  private void onAdDataSuccess(@NonNull List<NativeResponse> ads) {
    if (mListener == null) {
      return;
    }
    if (ads == null || ads.size() == 0) {
      mListener.onADEvent(new ADEvent(AdEventType.NO_AD, new Object[]{ErrorCode.NO_AD_FILL}));
    }
    mBDNativeExpressAdDataAdapters = new ArrayList<>();
    int index = 0;
    for (NativeResponse ad : ads) {
      BDNativeExpressAdDataAdapter adDataAdapter = new BDNativeExpressAdDataAdapter(mContext, ad);
      adDataAdapter.setAdListener(mListener);
      mBDNativeExpressAdDataAdapters.add(adDataAdapter);
      if (index == 0) {
        try {
          mEcpm = Integer.parseInt(ad.getECPMLevel());
        } catch (Exception e) {
          Log.d(TAG, "get ecpm error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: ecpm = " + mEcpm);
      }
      index++;
    }
    mListener.onADEvent(new ADEvent(AdEventType.AD_LOADED, new Object[]{mBDNativeExpressAdDataAdapters}));
  }

  @Override
  public void setECPMLevel(String ecpmLevel) {
    if (mBDNativeExpressAdDataAdapters != null) {
      for (BDNativeExpressAdDataAdapter adDataAdapter : mBDNativeExpressAdDataAdapters) {
        adDataAdapter.getBoundData().setECPMLevel(ecpmLevel);
      }
    }
  }

  /**
   * ======================================================================
   * 以下方法暂不支持
   */

  @Override
  public int getECPM() {
    return mEcpm;
  }

  @Override
  public String getReqId() {
    return null;
  }

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
