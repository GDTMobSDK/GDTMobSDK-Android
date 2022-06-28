package com.qq.e.union.adapter.tt.unified;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTFeedAd;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.qq.e.ads.nativ.widget.ViewStatusListener;
import com.qq.e.comm.listeners.NegativeFeedbackListener;
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
import com.qq.e.comm.adevent.AdEventType;
import com.qq.e.comm.compliance.DownloadConfirmListener;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.union.adapter.util.AdapterImageLoader;
import com.qq.e.union.adapter.util.AdnLogoUtils;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.IImageLoader;
import com.qq.e.union.adapter.util.LogoImageView;
import com.qq.e.union.adapter.util.PxUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 穿山甲数据流广告数据适配器
 */
public class TTFeedAdDataAdapter implements NativeUnifiedADData, ADEventListener {

  private static final int STATUS_UNKNOWN = 0; // 未安装 且未下载
  private static final int STATUS_INSTALLED = 1;
  private static final int STATUS_DOWNLOADING = 4; // 正在下载
  private static final int STATUS_DOWNLOAD_FINISHED = 8; // 下载完成，待安装
  private static final int STATUS_DOWNLOAD_FAILED = 16; // 下载失败
  private static final int STATUS_DOWNLOAD_PAUSED = 0; // 下载暂停
  
  private static final String TAG = TTFeedAdDataAdapter.class.getSimpleName();

  private MediaView mediaView;
  private TTFeedAd data;
  private TTImage firstImg;
  private List<String> imgList;
  private ADListener listener;
  private int downloadProgress;
  private NativeAdContainer container;
  private List<View> clickViews;
  private List<View> customClickViews;
  private int apkStatus;
  private String ecpmLevel;
  private boolean hasExposed;
  /**
   * 要持有一下下载 listener，否则会被回收
   */
  private TTAppDownloadListener appDownloadListener;

  private IImageLoader imageLoader;


  public TTFeedAdDataAdapter(TTFeedAd data) {
    this.data = data;
    imgList = new ArrayList<>();
    List<TTImage> list = data.getImageList();
    if (list != null && !list.isEmpty()) {
      firstImg = list.get(0);
      for (TTImage img : list) {
        imgList.add(img.getImageUrl());
      }
    }
  }

  @Override
  public String getTitle() {
    return data.getTitle();
  }

  @Override
  public String getDesc() {
    return data.getDescription();
  }

  /**
   * 穿山甲不支持获取广告 logo url
   * 请使用 {@link #getIconBitmap()}
   */
  @Override
  public String getIconUrl() {
    return data.getIcon() != null ? data.getIcon().getImageUrl() : null;
  }

  public Bitmap getIconBitmap() {
    return data.getAdLogo();
  }

  @Override
  public String getImgUrl() {
    return firstImg == null ? null : firstImg.getImageUrl();
  }

  /**
   *  穿山甲样式无法与优量汇样式完全匹配，需要开发者注意样式的适配。
   */
  @Override
  public int getAdPatternType() {
    switch (data.getImageMode()) {
      case TTAdConstant.IMAGE_MODE_VIDEO:
      case TTAdConstant.IMAGE_MODE_VIDEO_VERTICAL:
        return AdPatternType.NATIVE_VIDEO;
      case TTAdConstant.IMAGE_MODE_GROUP_IMG:
        return AdPatternType.NATIVE_3IMAGE;
      case TTAdConstant.IMAGE_MODE_LARGE_IMG:
        return AdPatternType.NATIVE_2IMAGE_2TEXT;
      case TTAdConstant.IMAGE_MODE_SMALL_IMG:
        return AdPatternType.NATIVE_1IMAGE_2TEXT;
      default:
        return AdPatternType.NATIVE_1IMAGE_2TEXT;
    }
  }

  @Override
  public List<String> getImgList() {
    return imgList;
  }

  @Override
  public boolean isAppAd() {
    if (data != null) {
      return data.getInteractionType() == TTAdConstant.INTERACTION_TYPE_DOWNLOAD;
    } else {
      return false;
    }
  }

  @Override
  public boolean isWeChatCanvasAd() {
    return false;
  }

  @Override
  public int getAppStatus() {
    return apkStatus;
  }

  @Override
  public int getProgress() {
    return downloadProgress;
  }

  @Override
  public int getPictureWidth() {
    return firstImg == null ? 0 : firstImg.getWidth();
  }

  @Override
  public int getPictureHeight() {
    return firstImg == null ? 0 : firstImg.getHeight();
  }

  @Override
  public int getECPM() {
    try {
      return  (int) data.getMediaExtraInfo().get("price");
    } catch (Exception e) {
      Log.d(TAG, "get ecpm error ", e);
    }
    return Constant.VALUE_NO_ECPM;
  }

  @Override
  public boolean equalsAdData(NativeUnifiedADData adData) {
    if (!(adData instanceof TTFeedAdDataAdapter)) {
      return false;
    }
    TTFeedAdDataAdapter ad = (TTFeedAdDataAdapter) adData;
    if (ad.data == null || data == null) {
      return false;
    }
    return data.equals(ad.data);
  }

  @Override
  public void bindAdToView(Context context, NativeAdContainer container,
                           FrameLayout.LayoutParams adLogoParams, List<View> clickViews) {
    bindAdToView(context, container, adLogoParams, clickViews, null);
  }

  @Override
  public void bindAdToView(Context context, NativeAdContainer container,
                           FrameLayout.LayoutParams adLogoParams, List<View> clickViews,
                           List<View> customClickViews) {
    imageLoader = new AdapterImageLoader(context);
    if (container == null || (clickViews == null)) {
      return;
    }
    this.container = container;
    this.clickViews = clickViews;
    this.customClickViews = customClickViews;
    // 视频广告应该通过 bindMediaView 进行绑定
    if (!isVideo()) {
      data.registerViewForInteraction(container, clickViews, customClickViews, getInteractionListener());
      tryBindDownloadListener();
    }
    AdnLogoUtils.initAdLogo(context, imageLoader, adLogoParams,
        FrameLayout.LayoutParams.WRAP_CONTENT,
        FrameLayout.LayoutParams.WRAP_CONTENT, container, data.getAdLogo());
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
    int size = Math.min(imageViews.size(), imgList.size());
    for (int i = 0; i < size; i++) {
      imageLoader.displayImage(imageViews.get(i), imgList.get(i));
    }
  }

  /**
   * @param mediaListener 这个参数传入为 null
   */
  @Override
  public void bindMediaView(MediaView view, VideoOption videoOption, NativeADMediaListener mediaListener) {
    if (container == null || clickViews == null || view == null || !isVideo()) {
      return;
    }
    mediaView = view;
    data.setVideoAdListener(new TTFeedAd.VideoAdListener() {
      @Override
      public void onVideoLoad(TTFeedAd ttFeedAd) {
        Log.d(TAG, "onVideoLoad: ");
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.VIDEO_CACHE, new Object[]{getVideoDuration()}));
        }
      }

      @Override
      public void onVideoError(int errorCode, int extraCode) {
        Log.d(TAG, "onVideoError: errorCode: " + errorCode + "  extraCode: " + extraCode );
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.VIDEO_ERROR));
        }
      }

      @Override
      public void onVideoAdStartPlay(TTFeedAd ttFeedAd) {
        Log.d(TAG, "onVideoAdStartPlay: ");
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.VIDEO_START));
        }
      }

      @Override
      public void onVideoAdPaused(TTFeedAd ttFeedAd) {
        Log.d(TAG, "onVideoAdPaused: ");
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.VIDEO_PAUSE));
        }
      }

      @Override
      public void onVideoAdContinuePlay(TTFeedAd ttFeedAd) {
        Log.d(TAG, "onVideoAdContinuePlay: ");
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.VIDEO_RESUME));
        }
      }

      @Override
      public void onProgressUpdate(long current, long duration) {
//        Log.d(TAG, "onProgressUpdate: " + current);
      }

      @Override
      public void onVideoAdComplete(TTFeedAd ttFeedAd) {
        Log.d(TAG, "onVideoAdComplete: ");
      }
    });
    // 将视频播放器加入 MediaView，此处需要注意，穿山甲 Demo 中 xml 布局将高度设置为 200 dp，开发者需要自行设置
    View video = data.getAdView();
    if (video != null && video.getParent() == null) {
      mediaView.removeAllViews();
      mediaView.addView(video, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT,
          PxUtils.dpToPx(mediaView.getContext(), 200)));
    }
    tryBindDownloadListener();
    if (clickViews == null || clickViews.size() == 0) {
      clickViews = customClickViews;
    }
    data.registerViewForInteraction(container, clickViews, customClickViews, getInteractionListener());
    if (listener != null) {
      listener.onADEvent(new ADEvent(AdEventType.VIDEO_INIT));
    }
  }

  @Override
  public void setAdListener(ADListener adListener) {
    this.listener = adListener;
  }

  private TTNativeAd.AdInteractionListener getInteractionListener() {
    return new TTNativeAd.AdInteractionListener() {
      @Override
      public void onAdClicked(View view, TTNativeAd ttNativeAd) {
        Log.d(TAG, "onAdClicked: ");
        onClick();
      }

      @Override
      public void onAdCreativeClick(View view, TTNativeAd ttNativeAd) {
        Log.d(TAG, "onAdCreativeClick: ");
        onClick();
      }

      @Override
      public void onAdShow(TTNativeAd ttNativeAd) {
        Log.d(TAG, "onAdShow: " + hasExposed);
        if (hasExposed) {
          return;
        }
        hasExposed = true;
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED));
        }
      }

      private void onClick() {
        if (listener != null) {
          listener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
        }
      }
    };
  }

  /**
   * 为下载类型的广告设置下载监听器
   */
  private void tryBindDownloadListener() {
    if (!isAppAd()) {
      return;
    }
    if (appDownloadListener == null) {
      appDownloadListener = new TTAppDownloadListener() {
        @Override
        public void onIdle() {
          Log.d(TAG, "onIdle: ");
          onAdStatusChanged(STATUS_UNKNOWN);
        }

        @Override
        public void onDownloadActive(long totalBytes, long currBytes, String fileName, String appName) {
          Log.d(TAG, "onDownloadActive: ");
          if (totalBytes <= 0) {
            downloadProgress = 0;
          } else {
            downloadProgress = (int) (currBytes * 100 / totalBytes);
          }
          onAdStatusChanged(STATUS_DOWNLOADING);
        }

        @Override
        public void onDownloadPaused(long totalBytes, long currBytes, String fileName, String appName) {
          Log.d(TAG, "onDownloadPaused: ");
          onAdStatusChanged(STATUS_DOWNLOAD_PAUSED);
        }


        @Override
        public void onDownloadFailed(long totalBytes, long currBytes, String fileName, String appName) {
          Log.d(TAG, "onDownloadFailed: ");
          downloadProgress = 0;
          onAdStatusChanged(STATUS_DOWNLOAD_FAILED);
        }

        @Override
        public void onDownloadFinished(long totalBytes, String fileName, String appName) {
          Log.d(TAG, "onDownloadFinished: ");
          downloadProgress = 100;
          onAdStatusChanged(STATUS_DOWNLOAD_FINISHED);
        }

        @Override
        public void onInstalled(String fileName, String appName) {
          Log.d(TAG, "onInstalled: ");
          onAdStatusChanged(STATUS_INSTALLED);
        }

        private void onAdStatusChanged(int status) {
          Log.d(TAG, "onAdStatusChanged: ");
          apkStatus = status;
          if (listener != null) {
            listener.onADEvent(new ADEvent(AdEventType.AD_STATUS_CHANGED));
          }
        }
      };
    }
    data.setDownloadListener(appDownloadListener);
  }

  private boolean isVideo() {
    int mode = data.getImageMode();
    return mode == TTAdConstant.IMAGE_MODE_VIDEO || mode == TTAdConstant.IMAGE_MODE_VIDEO_VERTICAL;
  }

  @Override
  public void destroy() {
    if (mediaView != null) {
      mediaView.removeAllViews();
      mediaView = null;
    }
    AdnLogoUtils.clearPreviousLogoView(container);
    data = null;
  }

  @Override
  public String getECPMLevel() {
    return ecpmLevel;
  }

  public void setEcpmLevel(String level) {
    ecpmLevel = level;
  }

  

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {
    if (data != null) {
      data.loss((double) price, String.valueOf(reason), adnId);
    }
  }

  @Override
  public void sendWinNotification(int price) {
    if (data != null) {
      data.win((double) price);
    }
  }

  @Override
  public void setBidECPM(int price) { }

  /* ================================以下方法暂不支持=========================================== */

  @Override
  public Map<String, Object> getExtraInfo() {
    HashMap<String, Object> map = new HashMap<>();
    try {
      map.put("request_id", data.getMediaExtraInfo().get("request_id"));
    } catch (Exception e) {
      Log.d(TAG, "getExtraInfo: " + e.toString());
    }
    return map;
  }

  @Override
  public void resume() {

  }

  @Override
  public void startVideo() {

  }

  @Override
  public void pauseVideo() {

  }

  @Override
  public void resumeVideo() {

  }

  @Override
  public void stopVideo() {

  }

  @Override
  public void setVideoMute(boolean mute) {

  }

  @Override
  public int getVideoCurrentPosition() {
    return 0;
  }


  @Override
  public String getCTAText() {
    return "";
  }

  @Override
  public void bindCTAViews(List<View> CTAViews) { }

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
  public int getVideoDuration() {
    return 0;
  }

  @Override
  public void setNativeAdEventListener(NativeADEventListener l) { }

  @Override
  public void negativeFeedback() {}

  @Override
  public void preloadVideo(VideoPreloadListener listener) {

  }


  @Override
  public void pauseAppDownload() {
    //如果为下载中则暂停下载
    if (data.getDownloadStatusController() != null) {
      data.getDownloadStatusController().changeDownloadStatus();
    }
  }

  @Override
  public void resumeAppDownload() {
    //如果为下载中则继续下载
    if (data.getDownloadStatusController() != null) {
      data.getDownloadStatusController().changeDownloadStatus();
    }
  }

  @Override
  public String getApkInfoUrl() {
    //工信部需求，穿山甲不支持
    return null;
  }

  @Override
  public void setDownloadConfirmListener(DownloadConfirmListener listener) {
    //工信部需求，穿山甲不支持
  }

  @Override
  public NativeUnifiedADAppMiitInfo getAppMiitInfo() {
    //工信部需求，穿山甲不支持
    return null;
  }

  @Override
  public String getButtonText() {
    return data.getButtonText();
  }

  @Override
  public void setNegativeFeedbackListener(NegativeFeedbackListener callback) {
  }

  @Override
  public boolean isValid() {
    return true;
  }
}
