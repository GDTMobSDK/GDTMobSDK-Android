package com.qq.e.union.adapter.kuaishou.nativ;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import com.kwad.sdk.api.KsAdSDK;
import com.kwad.sdk.api.KsAdVideoPlayConfig;
import com.kwad.sdk.api.KsFeedAd;
import com.kwad.sdk.api.KsLoadManager;
import com.kwad.sdk.api.KsScene;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.mediation.interfaces.BaseNativeExpressAd;
import com.qq.e.union.adapter.kuaishou.util.KSSDKInitUtil;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;
import com.qq.e.union.adapter.util.PxUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 快手模板信息流广告
 * appid 90009
 * 测试广告位
 * 4000000075L // 自定义Feed测试+文字悬浮在图片
 * 4000000078L // 自定义Feed测试+左文右图
 * 4000000076L // 自定义Feed测试+左图右文
 * 4000000074L // 自定义Feed测试+上文下图/上文下视频
 * 4000000079L // 自定义Feed测试+上图下文/上视频下文
 * 4000000077L // 自定义Feed测试+上文下图 精简
 * 4000000080L // 自定义Feed测试+上图下文 精简
 */
public class KSNativeExpressAdAdapter extends BaseNativeExpressAd {

  private final static String TAG = KSNativeExpressAdAdapter.class.getSimpleName();
  private long mPosId = -1;
  private final int mWidth;
  private ADListener mListener;
  private final Context mContext;
  private List<KSNativeExpressAdDataAdapter> mKSNativeExpressAdDataAdapters;
  private int mEcpm = Constant.VALUE_NO_ECPM;

  public KSNativeExpressAdAdapter(Context context, ADSize adSize, String appId, String posId,
                                  String ext) {
    super(context, adSize, appId, posId, ext);
    KSSDKInitUtil.init(context, appId);
    mContext = context;
    mWidth = PxUtils.dpToPx(mContext, adSize.getWidth());
    try {
      mPosId = Long.parseLong(posId);
    } catch (Exception e) {
      Log.e(TAG, "posId 异常 ");
    }
  }


  @Override
  public void setAdListener(ADListener adListener) {
    mListener = adListener;
  }

  @Override
  public void loadAD(int count) {
    if (mPosId < 0) {
      Log.d(TAG, "posId 异常 ");
      onLoadError();
      return;
    }
    KsScene scene = new KsScene.Builder(mPosId)
      .width(mWidth)
      .adNum(count).build(); // 此为测试posId，请联系快手平台申请正式posId
    KsAdSDK.getLoadManager()
      .loadConfigFeedAd(scene, new KsLoadManager.FeedAdListener() {
        @Override
        public void onError(int code, String msg) {
          Log.d(TAG, "广告数据请求失败 " + code + msg);
          onLoadError();
        }

        @Override
        public void onFeedAdLoad(@Nullable List<KsFeedAd> adList) {
          if (adList == null || adList.isEmpty()) {
            Log.d(TAG, "广告数据为空 ");
            onLoadError();
            return;
          }
          mKSNativeExpressAdDataAdapters = new ArrayList<>();
          int index = 0;
          for (KsFeedAd ksFeedAd : adList) {
            if (ksFeedAd == null) {
              continue;
            }
            if (index == 0) {
              mEcpm = ksFeedAd.getECPM();
              Log.d(TAG, "onAdSuccess: ecpm = " + mEcpm);
            }
            index++;
            KsAdVideoPlayConfig videoPlayConfig = new KsAdVideoPlayConfig.Builder()
              .videoSoundEnable(false) // 是否有声播放
              .dataFlowAutoStart(true) // 是否非WiFi下自动播放
              .build();
            ksFeedAd.setVideoPlayConfig(videoPlayConfig);
            KSNativeExpressAdDataAdapter adapter = new KSNativeExpressAdDataAdapter(mContext,
              ksFeedAd, mWidth);
            adapter.setAdListener(mListener);
            mKSNativeExpressAdDataAdapters.add(adapter);
          }
          if (mListener != null) {
            mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_LOADED, new Object[]{mKSNativeExpressAdDataAdapters}));
          }
        }
      });
  }

  private void onLoadError() {
    if (mListener != null) {
      mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_NO_AD, new Object[]{ErrorCode.NO_AD_FILL}));
    }
  }

  @Override
  public void loadAD(int count, LoadAdParams params) {
    loadAD(count);
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

  @Override
  public void setVideoPlayPolicy(int videoPlayPolicy) {

  }

  @Override
  public int getECPM() {
    return mEcpm;
  }

  @Override
  public void setECPMLevel(String ecpmLevel) {
    if (mKSNativeExpressAdDataAdapters != null) {
      for (KSNativeExpressAdDataAdapter adapter : mKSNativeExpressAdDataAdapters) {
        adapter.getBoundData().setECPMLevel(ecpmLevel);
      }
    }
  }
}
