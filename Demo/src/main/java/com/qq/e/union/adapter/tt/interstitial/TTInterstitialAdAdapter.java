package com.qq.e.union.adapter.tt.interstitial;

import android.app.Activity;
import android.app.Dialog;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bytedance.sdk.openadsdk.AdSlot;
import com.bytedance.sdk.openadsdk.TTAdConstant;
import com.bytedance.sdk.openadsdk.TTAdNative;
import com.bytedance.sdk.openadsdk.TTAppDownloadListener;
import com.bytedance.sdk.openadsdk.TTFullScreenVideoAd;
import com.bytedance.sdk.openadsdk.TTImage;
import com.bytedance.sdk.openadsdk.TTNativeAd;
import com.qq.e.ads.cfg.DownAPPConfirmPolicy;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.interstitial2.ADRewardListener;
import com.qq.e.ads.interstitial2.UnifiedInterstitialADListener;
import com.qq.e.ads.interstitial2.UnifiedInterstitialMediaListener;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.util.AdError;
import com.qq.e.mediation.interfaces.BaseInterstitialAd;
import com.qq.e.union.adapter.tt.util.TTAdManagerHolder;
import com.qq.e.union.adapter.util.AdapterImageLoader;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ContextUtils;
import com.qq.e.union.adapter.util.ErrorCode;
import com.qq.e.union.demo.R;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

/**
 * 穿山甲插屏全屏和插屏半屏广告视频适配器
 * 作用：封装穿山甲，适配优量汇插屏全屏和插屏半屏广告
 */
public class TTInterstitialAdAdapter extends BaseInterstitialAd {

  private final String TAG = getClass().getSimpleName();
  protected final String posId;

  protected TTAdNative ttAdNative;
  private TTNativeAd ttNativeInteraction;
  private TTFullScreenVideoAd ttFullVideoAd;
  /**
   * 因为，在穿山甲插屏广告的回调中，不支持 离开当前 app 时的回调，所以没有实现 {@link UnifiedInterstitialADListener#onADLeftApplication}
   */
  protected UnifiedInterstitialADListener unifiedInterstitialADListener;
  protected WeakReference<Activity> activityReference;
  private boolean mHasShowDownloadActive = false;
  private Dialog mAdDialog;
  private ImageView mAdImageView;
  private ImageView mCloseImageView;
  private TextView mDislikeView;
  private ViewGroup mRootView;
  private Activity mContext;
  private final AdapterImageLoader mAdImageLoader;
  protected boolean mIsValid = false;
  private int ecpm = Constant.VALUE_NO_ECPM;

  public TTInterstitialAdAdapter(Activity context, String appId, String posId, String ext) {
    super(context, appId, posId, ext);
    TTAdManagerHolder.init(context, appId);
    ttAdNative = TTAdManagerHolder.get().createAdNative(context);
    mAdImageLoader = new AdapterImageLoader(mContext);
    mContext = context;
    TTAdManagerHolder.get().requestPermissionIfNecessary(context);
    this.posId = posId;
    this.activityReference = new WeakReference<>(ContextUtils.getActivity(context));
  }

  // show() 有遮罩, showAsPopupWindow() 无遮罩，在这里，一样的调用是为了兼容 demo 中 有无遮罩样式的展示
  @Override
  public void show() {
    // 自行原生渲染并展示
    if (mContext == null) {
      Log.d(TAG, "show Ad : no context passed in");
      return;
    }

    mAdDialog = new Dialog(mContext, R.style.native_insert_dialog);
    mAdDialog.setCancelable(false);
    mAdDialog.setContentView(R.layout.tt_native_insert_ad_layout);
    mRootView = mAdDialog.findViewById(R.id.tt_native_insert_ad_root);
    mAdImageView = (ImageView) mAdDialog.findViewById(R.id.tt_native_insert_ad_img);
    // 限制dialog 的最大宽度不能超过屏幕，宽高最小为屏幕宽的 1/3
    DisplayMetrics dm = mContext.getResources().getDisplayMetrics();
    int maxWidth = (dm == null) ? 0 : dm.widthPixels;
    int minWidth = maxWidth / 3;
    mAdImageView.setMaxWidth(maxWidth);
    mAdImageView.setMinimumWidth(minWidth);
    // noinspection SuspiciousNameCombination
    mAdImageView.setMinimumHeight(minWidth);
    mCloseImageView = (ImageView) mAdDialog.findViewById(R.id.tt_native_insert_close_icon_img);
    // 暂未绑定网盟dislike逻辑
    mDislikeView = null;

    ImageView iv = mAdDialog.findViewById(R.id.tt_native_insert_ad_logo);

    // 绑定关闭按钮
    iv.setImageBitmap(ttNativeInteraction.getAdLogo());

    bindCloseAction();
    // 绑定广告view事件交互
    bindViewInteraction();
    // 加载Ad 图片资源
    loadAdImage();
  }

  private void loadAdImage() {
    if (ttNativeInteraction.getImageList() != null && !ttNativeInteraction.getImageList().isEmpty()) {
      TTImage image = ttNativeInteraction.getImageList().get(0);
      if (image != null && image.isValid()) {
        String url = image.getImageUrl();
        if (mAdImageLoader != null)
        {
          mAdImageLoader.displayImage(mAdImageView, url);
        }
      }
    }

    showAd();
  }

  private void bindCloseAction() {
    mCloseImageView.setOnClickListener(new View.OnClickListener(){
      @Override
      public void onClick(View v) {
        close();
      }
    });
  }

  private void bindViewInteraction() {
    // 可以被点击的view, 比如标题、icon等,点击后尝试打开落地页，也可以把nativeView放进来意味整个广告区域可被点击
    List<View> clickViewList = new ArrayList<>();
    clickViewList.add(mAdImageView);

    // 触发创意广告的view（点击下载或拨打电话），比如可以设置为一个按钮，按钮上文案根据广告类型设定提示信息
    List<View> creativeViewList = new ArrayList<>();
    // 如果需要点击图文区域也能进行下载或者拨打电话动作，请将图文区域的view传入
    // creativeViewList.add(nativeView);
    creativeViewList.add(mAdImageView);
    List<View> imageViewList = new ArrayList<>();
    imageViewList.add(mAdImageView);
    // 重要! 这个涉及到广告计费，必须正确调用。convertView必须使用ViewGroup。
    ttNativeInteraction.registerViewForInteraction(mRootView, imageViewList, clickViewList, creativeViewList, mDislikeView, new TTNativeAd.AdInteractionListener() {
      @Override
      public void onAdClicked(View view, TTNativeAd ad) {
        if (ad != null && unifiedInterstitialADListener != null) {
          Log.d(TAG, "Ad: " + ad.getTitle() + " was clicked");
          unifiedInterstitialADListener.onADClicked();
        }
      }

      @Override
      public void onAdCreativeClick(View view, TTNativeAd ad) {
        if (ad != null && unifiedInterstitialADListener != null) {
          Log.d(TAG, "Creative Ad: " + ad.getTitle() + " was clicked");
          unifiedInterstitialADListener.onADClicked();
        }
      }

      @Override
      public void onAdShow(TTNativeAd ad) {
        if (ad != null && unifiedInterstitialADListener != null) {
          Log.d(TAG, "Ad: " + ad.getTitle() + " showed");
          unifiedInterstitialADListener.onADExposure();
          unifiedInterstitialADListener.onADOpened();
        }
      }
    });

    ttNativeInteraction.setDownloadListener(new TTAppDownloadListener() {
      @Override
      public void onIdle() {
        mHasShowDownloadActive = false;
      }

      @Override
      public void onDownloadActive(long totalBytes, long currBytes, String fileName, String appName) {
        Log.d(TAG, "onDownloadActive==totalBytes=" + totalBytes + ",currBytes=" + currBytes + ",fileName=" + fileName + ",appName=" + appName);

        if (!mHasShowDownloadActive) {
          mHasShowDownloadActive = true;
        }
      }

      @Override
      public void onDownloadPaused(long totalBytes, long currBytes, String fileName, String appName) {
        Log.d(TAG, "onDownloadPaused===totalBytes=" + totalBytes + ",currBytes=" + currBytes + ",fileName=" + fileName + ",appName=" + appName);
      }

      @Override
      public void onDownloadFailed(long totalBytes, long currBytes, String fileName, String appName) {
        Log.d(TAG, "onDownloadFailed==totalBytes=" + totalBytes + ",currBytes=" + currBytes + ",fileName=" + fileName + ",appName=" + appName);
      }

      @Override
      public void onDownloadFinished(long totalBytes, String fileName, String appName) {
        Log.d(TAG, "onDownloadFinished==totalBytes=" + totalBytes + ",fileName=" + fileName + ",appName=" + appName);
      }

      @Override
      public void onInstalled(String fileName, String appName) {
        Log.d(TAG, "onInstalled==" + ",fileName=" + fileName + ",appName=" + appName);
      }
    });
  }

  private void showAd() {
    if (mContext.isFinishing()) {
      return;
    }
    if (mAdDialog != null && !mAdDialog.isShowing()) {
      mAdDialog.show();
    }
    mIsValid = false;
  }

  @Override
  public void show(Activity act) {
    mContext = act;
    show();
  }

  @Override
  public void showAsPopupWindow() {
    show();
  }

  @Override
  public void showAsPopupWindow(Activity act) {
    show(act);
  }

  @Override
  public void setAdListener(UnifiedInterstitialADListener listener) {
    unifiedInterstitialADListener = listener;
  }

  @Override
  public boolean isValid() {
    return mIsValid;
  }

  @Override
  public void loadAd() {
    mIsValid = false;
    if (ttAdNative == null) {
      Log.i(TAG, "穿山甲 SDK 初始化错误，无法加载广告");
      return;
    }
    // 设置广告参数
    AdSlot adSlot = setAdSlotParams(new AdSlot.Builder()).build();

    ttAdNative.loadNativeAd(adSlot, new TTAdNative.NativeAdListener() {
      @Override
      public void onError(int code, String message) {
        Log.d(TAG, "loadAd error : " + code + ", " + message);
        if (unifiedInterstitialADListener != null) {
          unifiedInterstitialADListener.onNoAD(new AdError(ErrorCode.NO_AD_FILL, message));
        }
      }

      @Override
      public void onNativeAdLoad(List<TTNativeAd> ads){
        if (ads.get(0) == null) {
          Log.d(TAG, "loadAd onNativeAdLoad FAILED : no ads");
          if (unifiedInterstitialADListener != null) {
            unifiedInterstitialADListener.onNoAD(new AdError(ErrorCode.NO_AD_FILL, "onNativeAdLoad FAILED: no ads"));
          }
          return ;
        }
        ttNativeInteraction = ads.get(0);
        try {
          ecpm = (int) ttNativeInteraction.getMediaExtraInfo().get("price");
        } catch (Exception e) {
          Log.e(TAG, "get ecpm error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: ecpm = " + ecpm);
        Log.d(TAG, "loadAd onNativeAdLoad SUCCESS : ");
        if (unifiedInterstitialADListener != null) {
          unifiedInterstitialADListener.onADReceive();
        }
        mIsValid = true;
      }
    });
  }

  @Override
  public void loadFullScreenAD() {
    AdSlot adSlot = setFullScreenAdSlotParams(new AdSlot.Builder()).build();
    mIsValid = false;
    ttAdNative.loadFullScreenVideoAd(adSlot, new TTAdNative.FullScreenVideoAdListener() {
      @Override
      public void onError(int code, String message) {
        Log.e(TAG, "Callback --> onError: " + code + ", " + message);
        if (unifiedInterstitialADListener != null) {
          unifiedInterstitialADListener.onNoAD(new AdError(ErrorCode.NO_AD_FILL, message));
        }
      }

      @Override
      public void onFullScreenVideoAdLoad(TTFullScreenVideoAd ad) {
        Log.d(TAG, "Callback --> onFullScreenVideoAdLoad");
        mIsValid = true;
        ttFullVideoAd = ad;
        try {
          ecpm = (int) ad.getMediaExtraInfo().get("price");
        } catch (Exception e) {
          Log.e(TAG, "get ecpm error ", e);
        }
        Log.d(TAG, "onAdDataSuccess: ecpm = " + ecpm);
        ttFullVideoAd.setFullScreenVideoAdInteractionListener(
            new TTFullScreenVideoAd.FullScreenVideoAdInteractionListener() {

              @Override
              public void onAdShow() {
                if (unifiedInterstitialADListener != null) {
                  unifiedInterstitialADListener.onADExposure();
                  unifiedInterstitialADListener.onADOpened();
                }
                Log.d(TAG, "Callback --> FullVideoAd show");
              }

              @Override
              public void onAdVideoBarClick() {
                if (unifiedInterstitialADListener != null) {
                  unifiedInterstitialADListener.onADClicked();
                }
                Log.d(TAG, "Callback --> FullVideoAd bar click");
              }

              @Override
              public void onAdClose() {
                if (unifiedInterstitialADListener != null) {
                  unifiedInterstitialADListener.onADClosed();
                }
                Log.d(TAG, "Callback --> FullVideoAd close");
              }

              @Override
              public void onVideoComplete() {
                Log.d(TAG, "Callback --> FullVideoAd complete");
              }

              @Override
              public void onSkippedVideo() {
                Log.d(TAG, "Callback --> FullVideoAd skipped");
              }

            });

        ad.setDownloadListener(new TTAppDownloadListener() {
          @Override
          public void onIdle() {
            mHasShowDownloadActive = false;
          }

          @Override
          public void onDownloadActive(long totalBytes, long currBytes, String fileName,
                                       String appName) {
            Log.d(TAG, "onDownloadActive==totalBytes=" + totalBytes + ",currBytes=" + currBytes +
                ",fileName=" + fileName + ",appName=" + appName);

            if (!mHasShowDownloadActive) {
              mHasShowDownloadActive = true;
              Log.d(TAG, "下载中，点击下载区域暂停");
            }
          }

          @Override
          public void onDownloadPaused(long totalBytes, long currBytes, String fileName,
                                       String appName) {
            Log.d(TAG, "onDownloadPaused===totalBytes=" + totalBytes + ",currBytes=" + currBytes +
                ",fileName=" + fileName + ",appName=" + appName);
            Log.d(TAG, "下载暂停，点击下载区域继续");
          }

          @Override
          public void onDownloadFailed(long totalBytes, long currBytes, String fileName,
                                       String appName) {
            Log.d(TAG, "onDownloadFailed==totalBytes=" + totalBytes + ",currBytes=" + currBytes +
                ",fileName=" + fileName + ",appName=" + appName);
            Log.d(TAG, "下载失败，点击下载区域重新下载");
          }

          @Override
          public void onDownloadFinished(long totalBytes, String fileName, String appName) {
            Log.d(TAG, "onDownloadFinished==totalBytes=" + totalBytes + ",fileName=" + fileName +
                ",appName=" + appName);
            Log.d(TAG, "下载完成，点击下载区域重新下载");
          }

          @Override
          public void onInstalled(String fileName, String appName) {
            Log.d(TAG, "onInstalled==" + ",fileName=" + fileName + ",appName=" + appName);
            Log.d(TAG, "安装完成，点击下载区域打开");
          }
        });
        Log.d(TAG, "Callback --> loadFullScreenAD");
        if (unifiedInterstitialADListener != null) {
          unifiedInterstitialADListener.onADReceive();
        }
      }

      @Override
      public void onFullScreenVideoCached() {
        Log.d(TAG, "Callback --> onFullScreenVideoCached");
        // 视频缓存
        // mIsLoaded = true;
        if (unifiedInterstitialADListener != null) {
          unifiedInterstitialADListener.onVideoCached();
        }
      }

      @Override
      public void onFullScreenVideoCached(TTFullScreenVideoAd ttFullScreenVideoAd) {
        onFullScreenVideoCached();
      }
    });

  }

  @Override
  public void showFullScreenAD(Activity activity) {
    if (ttFullVideoAd != null/*&&mIsLoaded*/) {
      Log.d(TAG, "ttFullVideoAd not null");
      // 直接展示广告
      // ttFullVideoAd.showFullScreenVideoAd(FullScreenVideoActivity.this);
      // 展示广告，并传入广告展示的场景
      ttFullVideoAd.showFullScreenVideoAd(activity, TTAdConstant.RitScenes.GAME_GIFT_BONUS, null);
      ttFullVideoAd = null;
    } else {
      Log.e(TAG, "FullScreenVideo 请先加载广告");
    }
    mIsValid = false;
  }

  protected AdSlot.Builder setAdSlotParams(AdSlot.Builder builder) {
    return builder
        .setCodeId(posId)
        .setImageAcceptedSize(1080, 1920)
        // 请求原生广告时候，请务必调用该方法，设置参数为TYPE_BANNER或TYPE_INTERACTION_AD
        .setNativeAdType(AdSlot.TYPE_INTERACTION_AD);
  }

  protected AdSlot.Builder setFullScreenAdSlotParams(AdSlot.Builder builder) {
    return builder
        .setCodeId(posId)
        .setOrientation(TTAdConstant.VERTICAL); // 全屏广告默认设置为竖屏播放，可根据需要修改
  }

  @Override
  public void destory() {
    Log.d(TAG, "Callback --> destory");
  }

  @Override
  public void close() {
    Log.d(TAG, "Callback --> close");
    if (mAdDialog != null && mAdDialog.isShowing()) {
      mAdDialog.dismiss();
      if (unifiedInterstitialADListener != null) {
        unifiedInterstitialADListener.onADClosed();
      }
    }
  }
  @Override
  public int getECPM() {
    return ecpm;
  }

  /******************************以下方法暂未使用*****************************/


  @Override
  public String getECPMLevel() {
    return null;
  }

  @Override
  public void setVideoOption(VideoOption videoOption) {}

  @Override
  public void setMinVideoDuration(int minVideoDuration) {}

  @Override
  public void setMaxVideoDuration(int maxVideoDuration) {}

  @Override
  public void setVideoPlayPolicy(int videoPlayPolicy) {}

  @Override
  public int getAdPatternType() {
    return 0;
  }

  @Override
  public void setMediaListener(UnifiedInterstitialMediaListener listener) {}

  @Override
  public void setRewardListener(ADRewardListener listener) {}

  @Override
  public void setDownAPPConfirmPolicy(DownAPPConfirmPolicy policy) {}

  @Override
  public int getVideoDuration() {
    return 0;
  }

  @Override
  public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {}
}
