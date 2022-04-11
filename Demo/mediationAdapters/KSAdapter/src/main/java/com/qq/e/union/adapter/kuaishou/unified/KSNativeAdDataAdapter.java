package com.qq.e.union.adapter.kuaishou.unified;

import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.kwad.sdk.api.KsAdVideoPlayConfig;
import com.kwad.sdk.api.KsAppDownloadListener;
import com.kwad.sdk.api.KsImage;
import com.kwad.sdk.api.KsNativeAd;
import com.kwad.sdk.api.model.AdSourceLogoType;
import com.kwad.sdk.api.model.InteractionType;
import com.kwad.sdk.api.model.MaterialType;
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
import com.qq.e.union.adapter.util.IImageLoader;
import com.qq.e.union.adapter.util.LogoImageView;
import com.qq.e.union.adapter.util.PxUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 快手原生广告数据适配器
 */
public class KSNativeAdDataAdapter implements NativeUnifiedADData, ADEventListener {
  private static final int STATUS_UNKNOWN = 0; // 未安装 且未下载
  private static final int STATUS_INSTALLED = 1;
  private static final int STATUS_DOWNLOADING = 4; // 正在下载
  private static final int STATUS_DOWNLOAD_FINISHED = 8; // 下载完成，待安装
  private static final int STATUS_DOWNLOAD_FAILED = 16; // 下载失败
  private static final int STATUS_DOWNLOAD_PAUSED = 0; // 下载暂停
  private static final String TAG = KSNativeAdDataAdapter.class.getSimpleName();
  private KsNativeAd mKsNativeAd;
  private boolean mVideoMute;
  private ADListener mADListener;
  private int mApkStatus;
  private int mDownloadProgress;
  private IImageLoader imageLoader;
  private String mEcpmLevel;
  private NativeAdContainer container;

  public KSNativeAdDataAdapter(KsNativeAd ksNativeAd) {
    mKsNativeAd = ksNativeAd;
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
    if (clickViews == null || clickViews.size() == 0) {
      clickViews = customClickViews;
    }
    imageLoader = new AdapterImageLoader(context);
    this.container = container;
    mKsNativeAd
        .registerViewForInteraction(container, clickViews, new KsNativeAd.AdInteractionListener() {
          @Override
          public void onAdClicked(View view, KsNativeAd ksNativeAd) {
            if (mADListener != null) {
              mADListener.onADEvent(new ADEvent(AdEventType.AD_CLICKED));
            }
          }

          @Override
          public void onAdShow(KsNativeAd ksNativeAd) {
            if (mADListener != null) {
              mADListener.onADEvent(new ADEvent(AdEventType.AD_EXPOSED, new Object[]{""}));
            }
          }

          @Override
          public boolean handleDownloadDialog(DialogInterface.OnClickListener onClickListener) {
            return false;
          }

          @Override
          public void onDownloadTipsDialogShow() {
            Log.d(TAG, "AdInteractionListener: onDownloadTipsDialogShow");
          }

          @Override
          public void onDownloadTipsDialogDismiss() {
            Log.d(TAG, "AdInteractionListener: onDownloadTipsDialogDismiss");
          }
        });
    KsAppDownloadListener ksAppDownloadListener = new KsAppDownloadListener() {

      @Override
      public void onIdle() {
        Log.d(TAG, "onIdle: KsAppDownload");
        onAdStatusChanged(STATUS_UNKNOWN);
      }

      @Override
      public void onDownloadStarted() {
        Log.d(TAG, "onDownloadStarted: KsAppDownload");
        onAdStatusChanged(STATUS_DOWNLOADING);
      }

      @Override
      public void onProgressUpdate(int progress) {
        mDownloadProgress = progress;
        onAdStatusChanged(STATUS_DOWNLOADING);
      }

      @Override
      public void onDownloadFinished() {
        Log.d(TAG, "onDownloadFinished: KsAppDownload");
        mDownloadProgress = 100;
        onAdStatusChanged(STATUS_DOWNLOAD_FINISHED);
      }

      @Override
      public void onDownloadFailed() {
        Log.d(TAG, "onDownloadFailed: KsAppDownload");
        onAdStatusChanged(STATUS_DOWNLOAD_FAILED);
      }

      @Override
      public void onInstalled() {
        Log.d(TAG, "onInstalled: KsAppDownload");
        onAdStatusChanged(STATUS_INSTALLED);
      }

      private void onAdStatusChanged(int status) {
        Log.d(TAG, "onAdStatusChanged: ");
        mApkStatus = status;
        if (mADListener != null) {
          mADListener.onADEvent(new ADEvent(AdEventType.AD_STATUS_CHANGED));
        }
      }

    };
    // 注册下载监听器
    mKsNativeAd.setDownloadListener(ksAppDownloadListener);
    // 参照快手 demo 设置尺寸
    AdnLogoUtils.initAdLogo(
        context,
        imageLoader,
        adLogoParams,
        35,
        12,
        container,
        mKsNativeAd.getAdSourceLogoUrl(AdSourceLogoType.NORMAL));
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
    List<KsImage> imgList = mKsNativeAd.getImageList();
    int size = Math.min(imageViews.size(), imgList.size());
    for (int i = 0; i < size; i++) {
      imageLoader.displayImage(imageViews.get(i), imgList.get(i).getImageUrl());
    }
  }

  @Override
  public void bindMediaView(MediaView view, VideoOption videoOption,
                            NativeADMediaListener mediaListener) {
    if (view == null) {
      Log.d(TAG, "MediaView is null");
      return;
    }
    mKsNativeAd.setVideoPlayListener(new KsNativeAd.VideoPlayListener() {
      @Override
      public void onVideoPlayStart() {
        Log.d(TAG, "onVideoPlayStart");
        if (mADListener != null) {
          mADListener.onADEvent(new ADEvent(AdEventType.VIDEO_CACHE, new Object[]{getVideoDuration()}));
          mADListener.onADEvent(new ADEvent(AdEventType.VIDEO_START));
        }
      }

      @Override
      public void onVideoPlayComplete() {
        Log.d(TAG, "onVideoPlayComplete");
        if (mADListener != null) {
          mADListener.onADEvent(new ADEvent(AdEventType.VIDEO_COMPLETE));
        }
      }

      @Override
      public void onVideoPlayError(int what, int extra) {
        Log.d(TAG, "onVideoPlayError");
        if (mADListener != null) {
          mADListener.onADEvent(new ADEvent(AdEventType.VIDEO_ERROR));
        }
      }
    });

    // SDK默认渲染的视频view
    mVideoMute = videoOption != null && videoOption.getAutoPlayMuted();
    boolean isAlwaysPlay =
        videoOption != null && videoOption.getAutoPlayPolicy() == VideoOption.AutoPlayPolicy.ALWAYS;
    KsAdVideoPlayConfig videoPlayConfig =
        new KsAdVideoPlayConfig.Builder().videoSoundEnable(!mVideoMute) // 有声播放
            .dataFlowAutoStart(isAlwaysPlay) // 流量下自动播放
            .build();
    View videoView = mKsNativeAd.getVideoView(view.getContext(), videoPlayConfig);
    if (videoView != null && videoView.getParent() == null) {
      view.removeAllViews();
      // 快手demo中设置布局高度为200 dp，开发者可以自行设置
      view.addView(videoView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
          PxUtils.dpToPx(view.getContext(), 200)));
    }
  }

  @Override
  public String getCTAText() {
    return null;
  }

  @Override
  public String getTitle() {
    if (mKsNativeAd.getInteractionType() == InteractionType.DOWNLOAD) {
      return mKsNativeAd.getAppName();
    }
    return mKsNativeAd.getProductName();
  }

  @Override
  public String getDesc() {
    return mKsNativeAd.getAdDescription();
  }

  @Override
  public String getIconUrl() {
    if (mKsNativeAd.getInteractionType() == InteractionType.DOWNLOAD) {
      return mKsNativeAd.getAppIconUrl();
    }
    return null;
  }

  @Override
  public String getImgUrl() {
    if (mKsNativeAd.getImageList() != null && !mKsNativeAd.getImageList().isEmpty()) {
      KsImage image = mKsNativeAd.getImageList().get(0);
      if (image != null && image.isValid()) {
        return image.getImageUrl();
      }
    }
    return null;
  }

  @Override
  public int getAdPatternType() {
    switch (mKsNativeAd.getMaterialType()) {
      case MaterialType.GROUP_IMG:
        return AdPatternType.NATIVE_3IMAGE;
      case MaterialType.VIDEO:
        return AdPatternType.NATIVE_VIDEO;
      case MaterialType.SINGLE_IMG:
        // fall through
      default:
        return AdPatternType.NATIVE_2IMAGE_2TEXT;
    }
  }

  @Override
  public List<String> getImgList() {
    if (mKsNativeAd.getImageList() != null && !mKsNativeAd.getImageList().isEmpty()) {
      List<String> result = new ArrayList<>();
      for (KsImage image : mKsNativeAd.getImageList()) {
        if (image != null) {
          result.add(image.getImageUrl());
        }
      }
      return result;
    }
    return null;
  }

  @Override
  public boolean isAppAd() {
    return mKsNativeAd.getInteractionType() == InteractionType.DOWNLOAD;
  }

  @Override
  public boolean isWeChatCanvasAd() {
    return false;
  }

  @Override
  public void setAdListener(ADListener adListener) {
    mADListener = adListener;
  }

  @Override
  public int getAppStatus() {
    return mApkStatus;
  }

  @Override
  public int getProgress() {
    return mDownloadProgress;
  }

  @Override
  public long getDownloadCount() {
    return 0; // 快手不支持
  }

  @Override
  public int getAppScore() {
    return 0; // 快手不支持
  }

  @Override
  public double getAppPrice() {
    return 0; // 快手不支持
  }

  @Override
  public int getVideoDuration() {
    return mKsNativeAd.getVideoDuration();
  }

  @Override
  public int getPictureWidth() {
    if (mKsNativeAd.getVideoCoverImage() != null) {
      return mKsNativeAd.getVideoCoverImage().getWidth();
    }
    return 0;
  }

  @Override
  public int getPictureHeight() {
    if (mKsNativeAd.getVideoCoverImage() != null) {
      return mKsNativeAd.getVideoCoverImage().getHeight();
    }
    return 0;
  }

  /**
   * 需要统一单位为分
   */
  @Override
  public int getECPM() {
    return mKsNativeAd.getECPM();
  }


  @Override
  public void setNativeAdEventListener(NativeADEventListener listener) {
  }

  @Override
  public void negativeFeedback() {

  }

  @Override
  public boolean equalsAdData(NativeUnifiedADData adData) {
    return false;
  }

  @Override
  public void bindCTAViews(List<View> CTAViews) {

  }

  @Override
  public String getECPMLevel() {
    return mEcpmLevel;
  }

  public void setEcpmLevel(String level) {
    mEcpmLevel = level;
  }

  @Override
  public NativeUnifiedADAppMiitInfo getAppMiitInfo() {
    NativeUnifiedADAppMiitInfo info = new NativeUnifiedADAppMiitInfo() {
      @Override
      public String getAppName() {
        return mKsNativeAd.getAppName();
      }

      @Override
      public String getAuthorName() {
        return mKsNativeAd.getCorporationName();
      }

      @Override
      public long getPackageSizeBytes() {
        return mKsNativeAd.getAppPackageSize();
      }

      @Override
      public String getPermissionsUrl() {
        return mKsNativeAd.getPermissionInfo();
      }

      @Override
      public String getPrivacyAgreement() {
        return mKsNativeAd.getAppPrivacyUrl();
      }

      @Override
      public String getVersionName() {
        return mKsNativeAd.getAppVersion();
      }
    };
    return info;
  }

  @Override
  public void sendWinNotification(int price) {
    // 快手不支持
  }

  @Override
  public void sendLossNotification(int price, int reason, String adnId) {
    // 快手不支持
  }

  @Override
  public void setBidECPM(int price) {
    // 快手不支持
  }

  @Override
  public Map<String, Object> getExtraInfo() {
    return new HashMap<>(); // 快手不支持
  }

  @Override
  public void resume() {
    // 快手不支持
  }

  @Override
  public void destroy() {
    AdnLogoUtils.clearPreviousLogoView(container);
  }

  @Override
  public void startVideo() {
    // 快手不支持
  }

  @Override
  public void pauseVideo() {
    // 快手不支持
  }

  @Override
  public void resumeVideo() {
    // 快手不支持
  }

  @Override
  public void stopVideo() {
    // 快手不支持
  }

  @Override
  public void setVideoMute(boolean mute) {
    mVideoMute = mute;
  }

  @Override
  public int getVideoCurrentPosition() {
    return 0; // 快手不支持
  }


  @Override
  public void preloadVideo(VideoPreloadListener listener) {
    // 快手不支持
  }



  @Override
  public void pauseAppDownload() {

  }

  @Override
  public void resumeAppDownload() {

  }

  @Override
  public String getApkInfoUrl() {
    //工信部需求，快手不支持
    return null;
  }

  @Override
  public void setDownloadConfirmListener(DownloadConfirmListener listener) {
    //工信部需求，快手不支持
  }

  @Override
  public String getButtonText() {
    return mKsNativeAd.getActionDescription();
  }

  @Override
  public void setNegativeFeedbackListener(NegativeFeedbackListener callback) {

  }

  @Override
  public boolean isValid() {
    return true;
  }
}
