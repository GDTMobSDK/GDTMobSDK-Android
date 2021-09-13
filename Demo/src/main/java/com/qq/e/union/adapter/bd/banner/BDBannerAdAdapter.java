package com.qq.e.union.adapter.bd.banner;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;

import com.baidu.mobads.sdk.api.AdView;
import com.baidu.mobads.sdk.api.AdViewListener;
import com.qq.e.ads.banner2.UnifiedBannerADListener;
import com.qq.e.comm.util.AdError;
import com.qq.e.mediation.interfaces.BaseBannerAd;
import com.qq.e.union.adapter.bd.util.BDAdManager;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;

import org.json.JSONObject;

/**
 * 百度 Banner Adapter
 * 宽高比 20 : 3 测试广告位为 2015351, appid e866cfb0
 */
public class BDBannerAdAdapter extends BaseBannerAd {

  private static final String TAG = "BDBannerAdAdapter";

  private final String mPosId;
  private final Activity mContext;
  private final AdView mAdView;
  private UnifiedBannerADListener mBannerADListener;
  private final Handler mUiHandler;

  public BDBannerAdAdapter(Activity context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    BDAdManager.init(context, appId);
    mPosId = posId;
    mContext = context;
    mAdView = new AdView(context, posId);
    mUiHandler = new Handler(Looper.getMainLooper());
    mAdView.setListener(new AdViewListener() {
      @Override
      public void onAdReady(AdView adView) {
        Log.d(TAG, "onAdReady: " + adView);
        mUiHandler.post(() -> {
          if (mBannerADListener != null) {
            mBannerADListener.onADReceive();
          }
        });
      }

      @Override
      public void onAdShow(JSONObject jsonObject) {
        Log.d(TAG, "onAdShow: " + jsonObject);
        mUiHandler.post(() -> {
          if (mBannerADListener != null) {
            mBannerADListener.onADExposure();
          }
        });
      }

      @Override
      public void onAdClick(JSONObject jsonObject) {
        Log.d(TAG, "onAdClick: " + jsonObject);
        mUiHandler.post(() -> {
          if (mBannerADListener != null) {
            mBannerADListener.onADClicked();
          }
        });
      }

      @Override
      public void onAdFailed(String s) {
        Log.d(TAG, "onAdFailed: " + s);
        mUiHandler.post(() -> {
          if (mBannerADListener != null) {
            mBannerADListener.onNoAD(new AdError(ErrorCode.NO_AD_FILL, s));
          }
        });
      }

      @Override
      public void onAdSwitch() {
        Log.d(TAG, "onAdSwitch: ");
      }

      @Override
      public void onAdClose(JSONObject jsonObject) {
        Log.d(TAG, "onAdClose: " + jsonObject);
        mUiHandler.post(() -> {
          if (mBannerADListener != null) {
            mBannerADListener.onADClosed();
          }
        });
      }
    });
  }

  @Override
  public void loadAD() {
    // 百度拉广告是将 banner view 加载到 view tree 中
  }

  @Override
  public void destroy() {
    Log.d(TAG, "destroy: ");
    mBannerADListener = null;
    mAdView.destroy();
  }

  @Override
  public int getECPM() {
    return Constant.VALUE_NO_ECPM;
  }

  @Override
  public void setAdListener(UnifiedBannerADListener adListener) {
    mBannerADListener = adListener;
  }

  @Override
  public void setAdSize(int widthPx, int heightPx) {
    // 不需处理
  }

  @Override
  public View getAdView() {
    return mAdView;
  }
}
