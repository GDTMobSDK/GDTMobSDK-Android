package com.qq.e.union.adapter.bd.unified;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.baidu.mobads.sdk.api.NativeResponse;
import com.baidu.mobads.sdk.api.XNativeView;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.MediaView;
import com.qq.e.ads.nativ.NativeADEventListener;
import com.qq.e.ads.nativ.NativeADMediaListener;
import com.qq.e.ads.nativ.NativeUnifiedADData;
import com.qq.e.ads.nativ.NativeUnifiedADAppMiitInfo;
import com.qq.e.ads.nativ.VideoPreloadListener;
import com.qq.e.ads.nativ.widget.NativeAdContainer;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADEventListener;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.union.adapter.util.AdapterImageLoader;
import com.qq.e.union.adapter.util.AdnLogoUtils;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.IImageLoader;
import com.qq.e.union.adapter.util.LogoImageView;

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
      Log.e(TAG, "get ecpm error ", e);
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
    View.OnClickListener listener = v -> {
      Log.d(TAG, "AD_CLICKED: ");
      data.handleClick(v);
      if (adListener != null) {
        adListener.onADEvent(new ADEvent(AD_CLICKED, new Object[]{""}));
      }
    };
    if (clickViews != null && clickViews.size() > 0) {
      for (View v : clickViews) {
        v.setOnClickListener(listener);
      }
    }

    if (customClickViews != null && customClickViews.size() > 0) {
      for (View v : customClickViews) {
        v.setOnClickListener(listener);
      }
    }

    // 由于百度没有曝光或展示的回调，所以在这里回调
    // 使用延时，是为了防止调用 {@link NativeUnifiedADData#setNativeAdEventListener} 在当前方法之后
    mainHandler.postDelayed(() -> {
      Log.d(TAG, "AD_EXPOSED: ");
      data.recordImpression(container);
      if (adListener != null) {
        adListener.onADEvent(new ADEvent(AD_EXPOSED));
      }
    }, 100);

    imageLoader = new AdapterImageLoader(context);
    AdnLogoUtils.initAdLogo(context, imageLoader, adLogoParams,
        28, 15, container, data.getAdLogoUrl()); // 参照百度 demo 设置尺寸
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
        adListener.onADEvent(new ADEvent(AD_CLICKED, new Object[]{""}));
      }
    });
    mediaView.addView(videoView);
    videoView.setNativeItem(data);
    videoView.render();
  }

  private boolean isVideo() {
    return data.getMaterialType() == NativeResponse.MaterialType.VIDEO;
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
  }

  /* ==================================以下方法暂不支持==========================================*/

  @Override
  public void sendWinNotification(int price) {

  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {

  }

  @Override
  public void setBidECPM(int price) {

  }

  @Override
  public Map<String, Object> getExtraInfo() {
    return null;
  }

  @Override
  public void resume() {
  }

  @Override
  public void setVideoMute(boolean mute) { }

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
  public void onVideoADExposured(View view) { }

  @Override
  public boolean isSkippable() {
    return false;
  }

  @Override
  public void bindCTAViews(List<View> CTAViews) { }

  @Override
  public void preloadVideo(VideoPreloadListener listener) {

  }

  @Override
  public String getVastTag() {
    return null;
  }

  @Override
  public String getVastContent() {
    return null;
  }

  @Override
  public void reportVastEvent(ADEvent adEvent) {

  }

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
    return data.getActButtonString();
  }
}
