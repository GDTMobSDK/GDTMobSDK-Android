package com.qq.e.union.adapter.bd.unified;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.XNativeView;
import com.qq.e.ads.nativ.CustomizeVideo;
import com.qq.e.ads.nativ.widget.ViewStatusListener;
import com.qq.e.comm.listeners.NegativeFeedbackListener;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.MediaView;
import com.qq.e.ads.nativ.NativeADEventListener;
import com.qq.e.ads.nativ.NativeADMediaListener;
import com.qq.e.ads.nativ.NativeUnifiedADData;
import com.qq.e.ads.nativ.NativeUnifiedADAppMiitInfo;
import com.qq.e.ads.nativ.widget.NativeAdContainer;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADEventListener;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.union.adapter.util.AdapterImageLoader;
import com.qq.e.union.adapter.util.AdnLogoUtils;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.IImageLoader;
import com.qq.e.union.adapter.util.LogoImageView;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 百度自渲染广告数据适配器
 */
public class BDNativeResponseAdapter implements NativeUnifiedADData, ADEventListener {

  private NativeResponse data;
  private ADListener adListener;
  private XNativeView videoView;
  private Handler mainHandler = new Handler(Looper.getMainLooper());
  private IImageLoader imageLoader;
  private String ecpmLevel;
  private NativeAdContainer container;
  private List clickViews;
  private List customClickViews;

  private static final String TAG = BDNativeResponseAdapter.class.getSimpleName();

  public BDNativeResponseAdapter(NativeResponse data) {
    this.data = data;
  }

  @Override
  public String getTitle() {
    return data.getTitle();
  }

  @Override
  public String getDesc() {
    return data.getDesc();
  }

  @Override
  public String getIconUrl() {
    return data.getIconUrl();
  }

  @Override
  public String getImgUrl() {
    return data.getImageUrl();
  }

  @Override
  public int getAdPatternType() {
    if (isVideo()) {
      return AdPatternType.NATIVE_VIDEO;
    }
    return AdPatternType.NATIVE_2IMAGE_2TEXT;
  }

  @Override
  public List<String> getImgList() {
    return data.getMultiPicUrls();
  }

  @Override
  public int getVideoDuration() {
    return data.getDuration();
  }

  @Override
  public int getPictureWidth() {
    return data.getMainPicWidth();
  }

  @Override
  public int getPictureHeight() {
    return data.getMainPicHeight();
  }

  @Override
  public int getECPM() {
    int ecpm = Constant.VALUE_NO_ECPM;
    try {
      ecpm = Integer.parseInt(data.getECPMLevel());
    } catch (Exception e) {
      Log.d(TAG, "get ecpm error ", e);
    }
    Log.d(TAG, "getECPM: " + ecpm);
    return ecpm;
  }

  @Override
  public boolean equalsAdData(NativeUnifiedADData nativeUnifiedADData) {
    if (!(nativeUnifiedADData instanceof BDNativeResponseAdapter)) {
      return false;
    }
    BDNativeResponseAdapter ad = (BDNativeResponseAdapter) nativeUnifiedADData;
    if (data == null || ad.data == null) {
      return false;
    }
    return data.equals(ad.data);
  }

  @Override
  public void bindAdToView(Context context, NativeAdContainer nativeAdContainer, FrameLayout.LayoutParams layoutParams, List<View> clickViews) {
    bindAdToView(context, nativeAdContainer, layoutParams, clickViews, null);
  }

  @Override
  public void bindAdToView(Context context, NativeAdContainer container,
                           FrameLayout.LayoutParams adLogoParams, List<View> clickViews,
                           List<View> customClickViews) {
    this.container = container;
    // 视频广告应该通过 bindMediaView 进行绑定
    if(!isVideo()){
      registerViewForInteraction(container, clickViews, customClickViews, null);
    } else {
      this.container = container;
      this.clickViews = clickViews;
      this.customClickViews = customClickViews;
    }
    imageLoader = new AdapterImageLoader(context);
    AdnLogoUtils.initAdLogo(context, imageLoader, adLogoParams,
        28, 15, container, data.getAdLogoUrl()); // 参照百度 demo 设置尺寸
    container.setViewStatusListener(new ViewStatusListener() {
      @Override
      public void onAttachToWindow() {
        LogoImageView logoImageView = AdnLogoUtils.getAddedLogo(container);
        if (logoImageView != null) {
          logoImageView.setVisibility(View.VISIBLE);
        }
      }

      @Override
      public void onDetachFromWindow() {
        LogoImageView logoImageView = AdnLogoUtils.getAddedLogo(container);
        if (logoImageView != null) {
          logoImageView.setVisibility(View.INVISIBLE);
        }
      }

      @Override
      public void onWindowFocusChanged(boolean hasWindowFocus) {

      }

      @Override
      public void onWindowVisibilityChanged(int visibility) {

      }

      @Override
      public void onDispatchTouchEvent(MotionEvent event) {

      }
    });
  }

  @Override
  public void bindAdToCustomVideo(ViewGroup container, Context context, List<View> clickViews,
                                  List<View> customClickViews) {

  }

  @Override
  public void bindImageViews(List<ImageView> imageViews, byte[] defaultImageData) {
    displayImage(imageViews);
  }

  @Override
  public void bindImageViews(List<ImageView> imageViews, int defaultImageRes) {
    displayImage(imageViews);
  }

  private void displayImage(List<ImageView> imageViews) {
    if (imageLoader == null || imageViews == null || imageViews.isEmpty()) {
      return;
    }
    List<String> imgList = data.getMultiPicUrls();
    if (imgList != null && !imgList.isEmpty()) {
      int size = Math.min(imageViews.size(), imgList.size());
      for (int i = 0; i < size; i++) {
        imageLoader.displayImage(imageViews.get(i), imgList.get(i));
      }
    } else {
      imageLoader.displayImage(imageViews.get(0), data.getImageUrl());
    }
  }

  /**
   * @param nativeADMediaListener 这个参数传入为 null
   */
  @Override
  public void bindMediaView(MediaView mediaView, VideoOption videoOption, NativeADMediaListener nativeADMediaListener) {
    if (!isVideo() || mediaView == null) {
      return;
    }
    videoView = new XNativeView(mediaView.getContext());
    videoView.setLayoutParams(new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
    videoView.setNativeViewClickListener(xNativeView -> {
      Log.d(TAG, "AD_CLICKED: ");
      if (adListener != null) {
        adListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
      }
    });
    mediaView.addView(videoView);
    registerViewForInteraction(container, clickViews, customClickViews, mediaView);
    videoView.setNativeItem(data);
    videoView.setVideoMute(videoOption.getAutoPlayMuted());
    videoView.render();
  }

  private boolean isVideo() {
    return data.getMaterialType() == NativeResponse.MaterialType.VIDEO;
  }

  // 获取安装状态、下载进度所对应的按钮文案
  private String getBtnText(NativeResponse nrAd) {
    if (nrAd == null) {
      return "";
    }
    String actButtonString = nrAd.getActButtonString();
    if (nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_APP_DOWNLOAD
        || nrAd.getAdActionType() == NativeResponse.ACTION_TYPE_DEEP_LINK) {
      int status = nrAd.getDownloadStatus();
      if (status >= 0 && status <= 100) {
        return "下载中：" + status + "%";
      } else if (status == 101) {
        return "点击安装";
      } else if (status == 102) {
        return "继续下载";
      } else if (status == 103) {
        return "点击启动";
      } else if (status == 104) {
        return "重新下载";
      } else {
        if (!TextUtils.isEmpty(actButtonString)) {
          return actButtonString;
        }
        return "点击下载";
      }
    }
    if (!TextUtils.isEmpty(actButtonString)) {
      return actButtonString;
    }
    return "查看详情";
  }

  private void registerViewForInteraction(NativeAdContainer container, List<View> clickViews,
                                          List<View> customClickViews, MediaView mediaView) {
    if (mediaView != null) {
      clickViews.add(mediaView);
    }
    /**
     * 注册可点击的View，点击和曝光会在内部完成
     * @Param view 广告容器或广告View
     * @Param clickViews 可点击的View，默认展示下载整改弹框
     * @Param creativeViews 带有广告文案之类的View，点击不会触发下载整改弹框
     * @Param interactionListener 点击、曝光回调
     */
    data.registerViewForInteraction(container, clickViews, customClickViews,
        new NativeResponse.AdInteractionListener() {
          @Override
          public void onAdClick() {
            Log.i(TAG, "onAdClick:" + data.getTitle());
            if (adListener != null) {
              adListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
            }
          }

          @Override
          public void onADExposed() {
            Log.i(TAG,
                "onADExposed:" + data.getTitle() + ", actionType = " + data.getAdActionType());
            data.recordImpression(container);
            if (adListener != null) {
              adListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED));
            }
          }

          @Override
          public void onADExposureFailed(int reason) {
            Log.i(TAG, "onADExposureFailed: " + reason);
          }

          @Override
          public void onADStatusChanged() {
            Log.i(TAG, "onADStatusChanged:" + getBtnText(data));
          }

          @Override
          public void onAdUnionClick() {
            Log.i(TAG, "onADUnionClick");
          }
        });
  }

  @Override
  public void setAdListener(ADListener adListener) {
    this.adListener = adListener;
  }

  @Override
  public void startVideo() {
    if (videoView != null) {
      videoView.render();
    }
  }

  @Override
  public void pauseVideo() {
    if (videoView != null) {
      videoView.pause();
    }
  }

  @Override
  public void resumeVideo() {
    if (videoView != null) {
      videoView.resume();
    }
  }

  @Override
  public void stopVideo() {
    if (videoView != null) {
      videoView.stop();
    }
  }

  @Override
  public String getCTAText() {
    return data.getActButtonString();
  }

  @Override
  public String getECPMLevel() {
    return ecpmLevel;
  }

  public void setEcpmLevel(String level) {
    ecpmLevel = level;
  }

  @Override
  public void destroy() {
    if (videoView != null) {
      videoView.stop();
      videoView = null;
    }
    AdnLogoUtils.clearPreviousLogoView(container);
  }

  @Override
  public void sendWinNotification(int price) {
    if (data != null) {
      data.biddingSuccess(String.valueOf(price));
    }
  }

  @Override
  public void sendWinNotification(Map<String, Object> map) {

  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {
    if (data != null) {
      data.biddingFail(String.valueOf(reason));
    }
  }

  @Override
  public void sendLossNotification(Map<String, Object> map) {

  }

  /* ==================================以下方法暂不支持==========================================*/
  @Override
  public void setBidECPM(int price) {

  }

  @Override
  public Map<String, Object> getExtraInfo() {
    return new HashMap<>();
  }

  @Override
  public void resume() {
  }

  @Override
  public void setVideoMute(boolean mute) {
    videoView.setVideoMute(mute);
  }

  @Override
  public boolean isAppAd() {
    return false;
  }

  @Override
  public boolean isWeChatCanvasAd() {
    return false;
  }

  @Override
  public int getAppStatus() {
    return 0;
  }

  @Override
  public int getProgress() {
    return 0;
  }

  @Override
  public long getDownloadCount() {
    return 0;
  }

  @Override
  public int getAppScore() {
    return 0;
  }

  @Override
  public double getAppPrice() {
    return 0;
  }

  @Override
  public int getVideoCurrentPosition() {
    return 0;
  }

  @Override
  public void setNativeAdEventListener(NativeADEventListener nativeADEventListener) {}

  @Override
  public void negativeFeedback() {}

  @Override
  public void bindCTAViews(List<View> CTAViews) { }



  @Override
  public void pauseAppDownload() {

  }

  @Override
  public void resumeAppDownload() {

  }
  
  @Override
  public String getApkInfoUrl() {
    //工信部需求，百度不支持
    return null;
  }

  @Override
  public void setDownloadConfirmListener(DownloadConfirmListener listener) {
    //工信部需求，百度不支持
  }

  @Override
  public NativeUnifiedADAppMiitInfo getAppMiitInfo() {
    //工信部需求，百度不支持
    return null;
  }

  @Override
  public String getButtonText() {
    return getBtnText(data);
  }

  @Override
  public CustomizeVideo getCustomizeVideo() {
    //工信部需求，百度不支持
    return null;
  }

  @Override
  public void setNegativeFeedbackListener(NegativeFeedbackListener listener) {

  }

  @Override
  public boolean isValid() {
    return true;
  }
}
