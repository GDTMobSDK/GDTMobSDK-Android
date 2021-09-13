package com.qq.e.union.demo;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.MediaView;
import com.qq.e.ads.nativ.NativeADEventListener;
import com.qq.e.ads.nativ.NativeADMediaListener;
import com.qq.e.ads.nativ.NativeADUnifiedListener;
import com.qq.e.ads.nativ.NativeUnifiedAD;
import com.qq.e.ads.nativ.NativeUnifiedADData;
import com.qq.e.ads.nativ.NativeUnifiedADAppMiitInfo;
import com.qq.e.ads.nativ.VideoPreloadListener;
import com.qq.e.ads.nativ.widget.NativeAdContainer;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.constants.AppDownloadStatus;
import com.qq.e.comm.constants.BiddingLossReason;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.util.DownloadConfirmHelper;
import com.qq.e.union.demo.view.ViewUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NativeADUnifiedSampleActivity extends Activity implements NativeADUnifiedListener {

  private AQuery mAQuery;
  private Button mDownloadButton;
  private Button mCTAButton;
  private NativeUnifiedADData mAdData;
  private H mHandler = new H();
  private static final int MSG_INIT_AD = 0;
  private static final int MSG_VIDEO_START = 1;
  private static final int AD_COUNT = 1;
  private static final String TAG = NativeADUnifiedSampleActivity.class.getSimpleName();


  // 与广告有关的变量，用来显示广告素材的UI
  private NativeUnifiedAD mAdManager;
  private MediaView mMediaView;
  private ImageView mImagePoster;
  private NativeAdContainer mContainer;

  private View mButtonsContainer;
  private Button mPlayButton;
  private Button mPauseButton;
  private Button mStopButton;
  private CheckBox mMuteButton;
  private Button mAppStatusButton; // 暂停或继续App下载的按钮

  private boolean mPlayMute = true;

  private boolean mPreloadVideo = false;
  private boolean mLoadingAd = false;
  private boolean mBindToCustomView;
  private FrameLayout mCustomContainer;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_unified_ad_simple);
    initView();

    boolean nonOption = getIntent().getBooleanExtra(Constants.NONE_OPTION, false);
    if(!nonOption){
      mPlayMute = getIntent().getBooleanExtra(Constants.PLAY_MUTE,true);
    }
    mBindToCustomView = getIntent().getBooleanExtra(Constants.BUTTON_BIND_TO_CUSTOM_VIEW, false);

    String token = getToken();
    Log.d(TAG, "onCreate: token = " + token);
    if (!TextUtils.isEmpty(token)) {
      mAdManager = new NativeUnifiedAD(this, getPosId(), this, token);
    } else {
      mAdManager = new NativeUnifiedAD(this, getPosId(), this);
    }
    mAdManager.setMinVideoDuration(getMinVideoDuration());
    mAdManager.setMaxVideoDuration(getMaxVideoDuration());
    // 下面设置项为海外流量使用，国内暂不支持
    mAdManager.setVastClassName("com.qq.e.union.demo.adapter.vast.unified.ImaNativeDataAdapter");
    layoutWithOrientation();
  }

  private void initView() {
    mMediaView = findViewById(R.id.gdt_media_view);
    mAppStatusButton = findViewById(R.id.app_download_button);
    mImagePoster = findViewById(R.id.img_poster);
    mDownloadButton = findViewById(R.id.btn_download);
    mCTAButton = findViewById(R.id.btn_cta);
    mContainer = findViewById(R.id.native_ad_container);
    mAQuery = new AQuery(findViewById(R.id.root));

    mButtonsContainer = findViewById(R.id.video_btns_container);
    mPlayButton = findViewById(R.id.btn_play);
    mPauseButton = findViewById(R.id.btn_pause);
    mStopButton = findViewById(R.id.btn_stop);
    mMuteButton = findViewById(R.id.btn_mute);
    mCustomContainer = findViewById(R.id.custom_container);
  }

  private String getPosId() {
    return getIntent().getStringExtra(Constants.POS_ID);
  }

  private String getToken() {
    return getIntent().getStringExtra(Constants.TOKEN);
  }

  private int getMinVideoDuration() {
    return getIntent().getIntExtra(Constants.MIN_VIDEO_DURATION, 0);
  }

  private int getMaxVideoDuration() {
    return getIntent().getIntExtra(Constants.MAX_VIDEO_DURATION, 0);
  }

  @Override
  public void onADLoaded(List<NativeUnifiedADData> ads) {
    Log.d(TAG, "onADLoaded");

    mLoadingAd = false;
    if (ads != null && ads.size() > 0) {
      Message msg = Message.obtain();
      msg.what = MSG_INIT_AD;
      mAdData = ads.get(0);
      reportBiddingResult(mAdData);
      if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
        mAdData.setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
      }

      //获取下载类广告的应用信息，可以用来展示工信部合规内容
      NativeUnifiedADAppMiitInfo miitInfo = mAdData.getAppMiitInfo();
      String miitStr;
      if (miitInfo != null) {
        miitStr = "miit info appName ='" + miitInfo.getAppName() + '\'' +
            ", authorName='" + miitInfo.getAuthorName() + '\'' +
            ", packageSizeBytes=" + miitInfo.getPackageSizeBytes() +
            ", permissionsUrl='" + miitInfo.getPermissionsUrl() + '\'' +
            ", privacyAgreement='" + miitInfo.getPrivacyAgreement() + '\'' +
            ", versionName='" + miitInfo.getVersionName() + '\'' +
            '}';
      } else {
        miitStr = "miit info is null";
      }
      Log.d(TAG, miitStr);
      msg.obj = mAdData;
      mHandler.sendMessage(msg);
    }
  }

  /**
   * 上报给优量汇服务端在开发者客户端竞价中优量汇的竞价结果，以便于优量汇服务端调整策略提供给开发者更合理的报价
   *
   * 优量汇竞价失败调用 sendLossNotification，并填入优量汇竞败原因（必填）、竞胜ADN ID（选填）、竞胜ADN报价（选填）
   * 优量汇竞价胜出调用 sendWinNotification，并填入开发者期望扣费价格（单位分）
   * 请开发者如实上报相关参数，以保证优量汇服务端能根据相关参数调整策略，使开发者收益最大化
   */
  private void reportBiddingResult(NativeUnifiedADData adData) {
    if (DemoUtil.isReportBiddingLoss() == DemoUtil.REPORT_BIDDING_LOSS) {
      adData.sendLossNotification(100, BiddingLossReason.LOW_PRICE, "WinAdnID");
    } else if (DemoUtil.isReportBiddingLoss() == DemoUtil.REPORT_BIDDING_WIN) {
      adData.sendWinNotification(200);
    }
    if (DemoUtil.isNeedSetBidECPM()) {
      adData.setBidECPM(300);
    }
  }

  public void onPreloadVideoClicked(View view) {
    loadAd(true);
  }

  public void onShowAdClicked(View view) {
    loadAd(false);
  }

  public void onReBindMediaView(View view) {
    if (mAdData != null) {
      ViewUtils.removeFromParent(mMediaView);
      mMediaView = new MediaView(this);
      mCustomContainer.addView(mMediaView, 0);
      bindMediaView(mAdData);
    }
  }

  private void loadAd(boolean preloadVideo) {
    if(mLoadingAd) {
      return;
    }
    mLoadingAd = true;
    resetAdViews();
    if(mAdData != null) {
      mAdData.destroy();
      mAdData = null;
    }
    mPreloadVideo = preloadVideo;
    if(mAdManager != null) {
      mAdManager.loadData(AD_COUNT, DemoUtil.getLoadAdParams("native_unified"));
    }
  }

  private void initAd(final NativeUnifiedADData ad) {
    if (ad.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
      if(mPreloadVideo) {
        // 如果是视频广告，可以调用preloadVideo预加载视频素材
        Toast.makeText(this, "正在加载视频素材", Toast.LENGTH_SHORT).show();
        ad.preloadVideo(new VideoPreloadListener() {
          @Override
          public void onVideoCached() {
            Log.d(TAG, "onVideoCached");
            // 视频素材加载完成，此时展示广告不会有进度条。
            showAd(ad);
          }

          @Override
          public void onVideoCacheFailed(int errorNo, String msg) {
            Log.d(TAG, "onVideoCacheFailed : " + msg);
          }
        });
      } else {
        showAd(ad);
      }
    } else {
      showAd(ad);
    }
  }

  private void showAd(final NativeUnifiedADData ad) {
    renderAdUi(ad);

    List<View> clickableViews = new ArrayList<>();
    List<View> customClickableViews = new ArrayList<>();
    // 所有广告类型，注册mDownloadButton的点击事件
    if (mBindToCustomView) {
      customClickableViews.add(mDownloadButton);
    } else {
      clickableViews.add(mDownloadButton);
    }

    List<ImageView> imageViews = new ArrayList<>();
    if(ad.getAdPatternType() == AdPatternType.NATIVE_2IMAGE_2TEXT ||
        ad.getAdPatternType() == AdPatternType.NATIVE_1IMAGE_2TEXT){
      // 双图双文、单图双文：注册mImagePoster的点击事件
      clickableViews.add(mImagePoster);
      imageViews.add(mImagePoster);
    } else if(ad.getAdPatternType() == AdPatternType.NATIVE_3IMAGE){
      // 三小图广告：注册native_3img_ad_container的点击事件
      clickableViews.add(findViewById(R.id.native_3img_ad_container));
      imageViews.add(findViewById(R.id.img_1));
      imageViews.add(findViewById(R.id.img_2));
      imageViews.add(findViewById(R.id.img_3));
    }
    //作为customClickableViews传入，点击不进入详情页，直接下载或进入落地页，视频和图文广告均生效
    ad.bindAdToView(this, mContainer, null, clickableViews, customClickableViews);
    if (!imageViews.isEmpty()) {
      ad.bindImageViews(imageViews, 0);
    } else if (ad.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
      // 视频广告，注册mMediaView的点击事件
      mHandler.sendEmptyMessage(MSG_VIDEO_START);

      bindMediaView(ad);

      mButtonsContainer.setVisibility(View.VISIBLE);

      View.OnClickListener listener = new View.OnClickListener(){
        @Override
        public void onClick(View v) {
          if(v == mPlayButton){
            ad.startVideo();
          }else if(v == mPauseButton){
            ad.pauseVideo();
          }else if(v == mStopButton){
            ad.stopVideo();
          }
        }
      };
      mPlayButton.setOnClickListener(listener);
      mPauseButton.setOnClickListener(listener);
      mStopButton.setOnClickListener(listener);

      mMuteButton.setChecked(mPlayMute);
      mMuteButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
          ad.setVideoMute(isChecked);
        }
      });
    }else if(ad.getAdPatternType() == AdPatternType.NATIVE_2IMAGE_2TEXT ||
            ad.getAdPatternType() == AdPatternType.NATIVE_1IMAGE_2TEXT){
      // 双图双文、单图双文：注册mImagePoster的点击事件
      clickableViews.add(mImagePoster);
    }else {
      // 三小图广告：注册native_3img_ad_container的点击事件
      clickableViews.add(findViewById(R.id.native_3img_ad_container));
    }
    //如果需要获得点击view的信息使用NativeADEventListenerWithClickInfo代替NativeADEventListener
    ad.setNativeAdEventListener(new NativeADEventListener() {
      @Override
      public void onADExposed() {
        Log.d(TAG, "onADExposed: ");
      }

      @Override
      public void onADClicked() {
        Log.d(TAG, "onADClicked: " + " clickUrl: " + ad.ext.get("clickUrl"));
      }

      @Override
      public void onADError(AdError error) {
        Log.d(TAG, "onADError error code :" + error.getErrorCode()
                + "  error msg: " + error.getErrorMsg());
      }

      @Override
      public void onADStatusChanged() {
        Log.d(TAG, "onADStatusChanged: ");
        updateAdAction(mDownloadButton, ad);
        updateAppStatusButton(ad);
      }
    });
    mAppStatusButton.setOnClickListener(new View.OnClickListener(){

      @Override
      public void onClick(View v) {
        if (ad.getAppStatus() == AppDownloadStatus.STATUS_DOWNLOADING) { // 正在下载
          ad.pauseAppDownload();
        } else if (ad.getAppStatus() == AppDownloadStatus.STATUS_DOWNLOAD_PAUSED) { // 下载已暂停
          ad.resumeAppDownload();
        }
      }
    });
    updateAdAction(mDownloadButton, ad);
    updateAppStatusButton(ad);
        /**
         * 营销组件
         * 支持项目：智能电话（点击跳转拨号盘），外显表单
         *  bindCTAViews 绑定营销组件监听视图，注意：bindCTAViews的视图不可调用setOnClickListener，否则SDK功能可能受到影响
         *  ad.getCTAText 判断拉取广告是否包含营销组件，如果包含组件，展示组件按钮，否则展示download按钮
         */
        List<View> CTAViews = new ArrayList<>();
        CTAViews.add(mCTAButton);
        ad.bindCTAViews(CTAViews);
        String ctaText = ad.getCTAText(); //获取组件文案
        if(!TextUtils.isEmpty(ctaText)){
            //如果拉取广告包含CTA组件，则渲染该组件
            //当广告中有营销组件时，隐藏下载按钮，仅为demo示例所用，开发者可自行决定mDownloadButton按钮是否显示
            mCTAButton.setText(ctaText);
            mCTAButton.setVisibility(View.VISIBLE);
            mDownloadButton.setVisibility(View.INVISIBLE);
        } else {
          mCTAButton.setVisibility(View.INVISIBLE);
          mDownloadButton.setVisibility(View.VISIBLE);
        }
    }
  private void bindMediaView(NativeUnifiedADData ad) {
    VideoOption videoOption = getVideoOption(getIntent());
    ad.bindMediaView(mMediaView, videoOption, new NativeADMediaListener() {
      @Override
      public void onVideoInit() {
        Log.d(TAG, "onVideoInit: ");
      }

      @Override
      public void onVideoLoading() {
        Log.d(TAG, "onVideoLoading: ");
      }

      @Override
      public void onVideoReady() {
        Log.d(TAG, "onVideoReady");
      }

      @Override
      public void onVideoLoaded(int videoDuration) {
        Log.d(TAG, "onVideoLoaded: ");

      }

      @Override
      public void onVideoStart() {
        Log.d(TAG, "onVideoStart");
      }

      @Override
      public void onVideoPause() {
        Log.d(TAG, "onVideoPause: ");
      }

      @Override
      public void onVideoResume() {
        Log.d(TAG, "onVideoResume: ");
      }

      @Override
      public void onVideoCompleted() {
        Log.d(TAG, "onVideoCompleted: ");
      }

      @Override
      public void onVideoError(AdError error) {
        Log.d(TAG, "onVideoError: ");
      }

      @Override
      public void onVideoStop() {
        Log.d(TAG, "onVideoStop");
      }

      @Override
      public void onVideoClicked() {
        Log.d(TAG, "onVideoClicked");
      }
    });
  }

  @Nullable
  public static VideoOption getVideoOption(Intent intent) {
    if(intent == null){
      return null;
    }

    VideoOption videoOption = null;
    boolean noneOption = intent.getBooleanExtra(Constants.NONE_OPTION, false);
    if (!noneOption) {
      VideoOption.Builder builder = new VideoOption.Builder();

      builder.setAutoPlayPolicy(intent.getIntExtra(Constants.PLAY_NETWORK, VideoOption.AutoPlayPolicy.ALWAYS));
      builder.setAutoPlayMuted(intent.getBooleanExtra(Constants.PLAY_MUTE, true));
      builder.setDetailPageMuted(intent.getBooleanExtra(Constants.DETAIL_PAGE_MUTED,false));
      builder.setNeedCoverImage(intent.getBooleanExtra(Constants.NEED_COVER, true));
      builder.setNeedProgressBar(intent.getBooleanExtra(Constants.NEED_PROGRESS, true));
      builder.setEnableDetailPage(intent.getBooleanExtra(Constants.ENABLE_DETAIL_PAGE, true));
      builder.setEnableUserControl(intent.getBooleanExtra(Constants.ENABLE_USER_CONTROL, false));

      videoOption = builder.build();
    }
    return videoOption;
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mAdData != null) {
      // 必须要在Activity.onResume()时通知到广告数据，以便重置广告恢复状态
      mAdData.resume();
    }
  }

  private void renderAdUi(NativeUnifiedADData ad) {
    int patternType = ad.getAdPatternType();
    if (patternType == AdPatternType.NATIVE_2IMAGE_2TEXT
        || patternType == AdPatternType.NATIVE_VIDEO) {
      mImagePoster.setVisibility(View.VISIBLE);
      mAQuery.id(R.id.img_logo).image(ad.getIconUrl(), false, true);
      mAQuery.id(R.id.text_title).text(ad.getTitle());
      mAQuery.id(R.id.text_desc).text(ad.getDesc());
    } else if (patternType == AdPatternType.NATIVE_3IMAGE) {
      mAQuery.id(R.id.native_3img_title).text(ad.getTitle());
      mAQuery.id(R.id.native_3img_desc).text(ad.getDesc());
    } else if (patternType == AdPatternType.NATIVE_1IMAGE_2TEXT) {
      mAQuery.id(R.id.img_poster).clear();
      mAQuery.id(R.id.text_title).text(ad.getTitle());
      mAQuery.id(R.id.text_desc).text(ad.getDesc());
    }
  }

  private void resetAdViews() {
    if(mAdData == null) {
      return;
    }
    int patternType = mAdData.getAdPatternType();
    if (patternType == AdPatternType.NATIVE_2IMAGE_2TEXT
            || patternType == AdPatternType.NATIVE_VIDEO) {
      mAQuery.id(R.id.img_logo).clear();
      mAQuery.id(R.id.img_poster).clear();
      mAQuery.id(R.id.text_title).clear();
      mAQuery.id(R.id.text_desc).clear();
    } else if (patternType == AdPatternType.NATIVE_3IMAGE) {
      mAQuery.id(R.id.img_1).clear();
      mAQuery.id(R.id.img_2).clear();
      mAQuery.id(R.id.img_3).clear();
      mAQuery.id(R.id.native_3img_title).clear();
      mAQuery.id(R.id.native_3img_desc).clear();
    } else if (patternType == AdPatternType.NATIVE_1IMAGE_2TEXT) {
      mAQuery.id(R.id.img_logo).clear();
      mAQuery.id(R.id.img_poster).clear();
      mAQuery.id(R.id.text_title).clear();
      mAQuery.id(R.id.text_desc).clear();
    }

    mButtonsContainer.setVisibility(View.GONE);
    mAppStatusButton.setVisibility(View.GONE);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mAdData != null) {
      // 必须要在Actiivty.destroy()时通知到广告数据，以便释放内存
      mAdData.destroy();
    }
  }

  private void updateAppStatusButton(NativeUnifiedADData ad) {
    if (!ad.isAppAd()) {
      return;
    }
    int status = ad.getAppStatus();
    if (status == AppDownloadStatus.STATUS_DOWNLOADING || status == AppDownloadStatus.STATUS_DOWNLOAD_PAUSED) { // 下载中或暂停中显示，其它状态不展示该按钮
      mAppStatusButton.setText(status == AppDownloadStatus.STATUS_DOWNLOADING ? R.string.pause_app_download :
          R.string.resume_app_download);
      if (!mAppStatusButton.isShown()) {
        mAppStatusButton.setVisibility(View.VISIBLE);
      }
    } else if (mAppStatusButton.isShown()) {
      mAppStatusButton.setVisibility(View.GONE);
    }
  }

  static void updateAdAction(Button button, NativeUnifiedADData ad) {
    String buttonText = ad.getButtonText();
    if (ad.isWeChatCanvasAd()) {
      button.setText(TextUtils.isEmpty(buttonText) ? "去微信看看" : buttonText);
      return;
    }
    if (!ad.isAppAd()) {
      button.setText(TextUtils.isEmpty(buttonText) ? "查看详情" : buttonText);
      return;
    }
    switch (ad.getAppStatus()) {
      case AppDownloadStatus.STATUS_INSTALLED: // 已安装
        button.setText("启动");
        break;
      case AppDownloadStatus.STATUS_DOWNLOADING: // 下载中
        button.setText(ad.getProgress() + "%");
        break;
      case AppDownloadStatus.STATUS_DOWNLOAD_FINISHED: // 已下载
        button.setText("安装");
        break;
      case AppDownloadStatus.STATUS_DOWNLOAD_FAILED: //下载失败
        button.setText("下载失败，重新下载");
        break;
      default: // 其它状态，未下载、暂停等
        button.setText(TextUtils.isEmpty(buttonText) ? "立即下载" : buttonText);
        break;
    }
  }

  @Override
  public void onNoAD(AdError error) {
    Log.d(TAG, "onNoAd error code: " + error.getErrorCode()
        + ", error msg: " + error.getErrorMsg());
    mLoadingAd = false;
    Toast.makeText(getApplicationContext(),error.getErrorCode()
            + ", error msg: " + error.getErrorMsg(),Toast.LENGTH_LONG).show();
  }

  private class H extends Handler {
    public H() {
      super();
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_INIT_AD:
          NativeUnifiedADData ad = (NativeUnifiedADData) msg.obj;
          Log.d(TAG, String.format(Locale.getDefault(), "(pic_width,pic_height) = (%d , %d)", ad
                  .getPictureWidth(),
              ad.getPictureHeight()));
          initAd(ad);
          Log.d(TAG,
              "eCPMLevel = " + ad.getECPMLevel() + "， ECPM: " + ad.getECPM()
                  + " ,videoDuration = " + ad.getVideoDuration()
                  + ", testExtraInfo:" + ad.getExtraInfo().get("mp"));
          break;
        case MSG_VIDEO_START:
          mImagePoster.setVisibility(View.GONE);
          mMediaView.setVisibility(View.VISIBLE);
          break;
      }
    }
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    layoutWithOrientation();
  }

  private void layoutWithOrientation() {
    mCustomContainer = findViewById(R.id.custom_container);
    int height = Math.min(Resources.getSystem().getDisplayMetrics().widthPixels,
        Resources.getSystem().getDisplayMetrics().heightPixels);
    mCustomContainer.post(new Runnable() {
      @Override
      public void run() {
        Configuration configuration = getResources().getConfiguration();
        ViewGroup.LayoutParams layoutParams = mCustomContainer.getLayoutParams();
        if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
          layoutParams.height = height;
        } else if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
          layoutParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        }
        mCustomContainer.setLayoutParams(layoutParams);
      }
    });
  }
}
