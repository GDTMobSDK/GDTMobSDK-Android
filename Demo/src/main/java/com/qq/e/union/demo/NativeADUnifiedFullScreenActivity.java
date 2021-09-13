package com.qq.e.union.demo;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.androidquery.AQuery;
import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.MediaView;
import com.qq.e.ads.nativ.NativeADEventListener;
import com.qq.e.ads.nativ.NativeADMediaListener;
import com.qq.e.ads.nativ.NativeADUnifiedListener;
import com.qq.e.ads.nativ.NativeUnifiedAD;
import com.qq.e.ads.nativ.NativeUnifiedADData;
import com.qq.e.ads.nativ.widget.NativeAdContainer;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.util.DownloadConfirmHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class NativeADUnifiedFullScreenActivity extends Activity implements NativeADUnifiedListener {

  private AQuery mAQuery;
  private Button mDownloadButton;
  private RelativeLayout mADInfoContainer;
  private NativeUnifiedADData mAdData;
  private NativeADUnifiedFullScreenActivity.H mHandler = new NativeADUnifiedFullScreenActivity.H();
  private static final int MSG_INIT_AD = 0;
  private static final int MSG_VIDEO_START = 1;
  private static final int MSG_UPDATE_PROGRESS = 2;
  private static final int AD_COUNT = 1;
  private static final String TAG = NativeADUnifiedFullScreenActivity.class.getSimpleName();

  // 与广告有关的变量，用来显示广告素材的UI
  private NativeUnifiedAD mAdManager;
  private MediaView mMediaView;
  private ImageView mImagePoster;
  private NativeAdContainer mContainer;
  private TextView mTimeText;

  private long mTotalTime;
  private boolean mBindToCustomView;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_unified_ad_full_screen);
    mBindToCustomView = getIntent().getBooleanExtra(Constants.BUTTON_BIND_TO_CUSTOM_VIEW, false);
    initView();

    mAdManager = new NativeUnifiedAD(this, getPosId(), this);
    mAdManager.setMinVideoDuration(getMinVideoDuration());
    mAdManager.setMaxVideoDuration(getMaxVideoDuration());

    mAdManager.loadData(AD_COUNT);
  }

  private void initView() {
    mMediaView = findViewById(R.id.gdt_media_view);
    mImagePoster = findViewById(R.id.img_poster);
    mADInfoContainer = findViewById(R.id.ad_info_container);
    mDownloadButton = findViewById(R.id.btn_download);
    mContainer = findViewById(R.id.native_ad_container);
    mTimeText = findViewById(R.id.time_text);
    mAQuery = new AQuery(findViewById(R.id.native_ad_container));
  }

  private String getPosId() {
    return getIntent().getStringExtra(Constants.POS_ID);
  }

  private int getMinVideoDuration() {
    return getIntent().getIntExtra(Constants.MIN_VIDEO_DURATION, 0);
  }

  private int getMaxVideoDuration() {
    return getIntent().getIntExtra(Constants.MAX_VIDEO_DURATION, 0);
  }

  @Override
  public void onADLoaded(List<NativeUnifiedADData> ads) {
    if (ads != null && ads.size() > 0) {
      Message msg = Message.obtain();
      msg.what = MSG_INIT_AD;
      mAdData = ads.get(0);
      if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
        mAdData.setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
      }
      msg.obj = mAdData;
      mHandler.sendMessage(msg);
    }
  }

  private void initAd(final NativeUnifiedADData ad) {
    renderAdUi(ad);
    List<View> clickableViews = new ArrayList<>();
    List<View> customClickableViews = new ArrayList<>();
    if (mBindToCustomView) {
      customClickableViews.add(mDownloadButton);
    } else {
      clickableViews.add(mDownloadButton);
    }
    ArrayList<ImageView>imageViews = new ArrayList<>();
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
    //作为customClickableViews传入，点击不进入详情页，直接下载或进入落地页，视频和图文广告均生效，
    ad.bindAdToView(this, mContainer, null, clickableViews, customClickableViews);
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
        NativeADUnifiedSampleActivity.updateAdAction(mDownloadButton, ad);
      }
    });
    if (!imageViews.isEmpty()) {
      ad.bindImageViews(imageViews, 0);
      mADInfoContainer.setVisibility(View.VISIBLE);
      mADInfoContainer.setBackgroundColor(Color.parseColor("#999999"));
    } else if (ad.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
      mADInfoContainer.setBackgroundColor(Color.parseColor("#00000000"));
      mHandler.sendEmptyMessage(MSG_VIDEO_START);

      VideoOption videoOption = NativeADUnifiedSampleActivity.getVideoOption(getIntent());

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
          mADInfoContainer.setVisibility(View.VISIBLE);
          mTotalTime = ad.getVideoDuration();
          mTimeText.setVisibility(View.VISIBLE);
          mTimeText.setText("倒计时： " + mTotalTime / 1000 + "s");
          Message msg = mHandler.obtainMessage(MSG_UPDATE_PROGRESS, ad);
          mHandler.sendMessageDelayed(msg, 500);
        }

        @Override
        public void onVideoPause() {
          Log.d(TAG, "onVideoPause: ");
          mHandler.removeMessages(MSG_UPDATE_PROGRESS);
        }

        @Override
        public void onVideoResume() {
          Log.d(TAG, "onVideoResume: ");
          Message msg = mHandler.obtainMessage(MSG_UPDATE_PROGRESS, ad);
          mHandler.sendMessageDelayed(msg, 0);
        }

        @Override
        public void onVideoCompleted() {
          Log.d(TAG, "onVideoCompleted: ");
          removeTimeText();
        }

        @Override
        public void onVideoError(AdError error) {
          Log.d(TAG, "onVideoError: ");
          removeTimeText();
        }

        @Override
        public void onVideoStop() {
          Log.d(TAG, "onVideoStop");
          removeTimeText();
        }

        @Override
        public void onVideoClicked() {
          Log.d(TAG, "onVideoClicked");
        }

        private void removeTimeText(){
          mTimeText.setVisibility(View.GONE);
          mHandler.removeMessages(MSG_UPDATE_PROGRESS);
        }
      });
    }

    NativeADUnifiedSampleActivity.updateAdAction(mDownloadButton, ad);
  }

  @Override
  protected void onResume() {
    super.onResume();
    if (mAdData != null) {
      // 必须要在Actiivty.onResume()时通知到广告数据，以便重置广告恢复状态
      mAdData.resume();
    }
  }

  private void renderAdUi(NativeUnifiedADData ad) {
    int patternType = ad.getAdPatternType();
    if (patternType == AdPatternType.NATIVE_2IMAGE_2TEXT
        || patternType == AdPatternType.NATIVE_VIDEO) {
      mAQuery.id(R.id.img_logo).image(ad.getIconUrl(), false, true);
      mAQuery.id(R.id.text_title).text(ad.getTitle());
      mAQuery.id(R.id.text_desc).text(ad.getDesc());
    } else if (patternType == AdPatternType.NATIVE_3IMAGE) {
      mAQuery.id(R.id.native_3img_title).text(ad.getTitle());
      mAQuery.id(R.id.native_3img_desc).text(ad.getDesc());
    } else if (patternType == AdPatternType.NATIVE_1IMAGE_2TEXT) {
      mAQuery.id(R.id.img_logo).clear();
      mAQuery.id(R.id.text_title).text(ad.getTitle());
      mAQuery.id(R.id.text_desc).text(ad.getDesc());
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mAdData != null) {
      // 必须要在Actiivty.destroy()时通知到广告数据，以便释放内存
      mAdData.destroy();
    }
    mHandler.removeMessages(MSG_UPDATE_PROGRESS);
  }

  @Override
  public void onNoAD(AdError error) {
    Log.d(TAG, "onNoAd error code: " + error.getErrorCode()
        + ", error msg: " + error.getErrorMsg());
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
          Log.d(TAG, "eCPMLevel = " + ad.getECPMLevel() + " , " +
              "videoDuration = " + ad.getVideoDuration());
          break;
        case MSG_VIDEO_START:
          mImagePoster.setVisibility(View.GONE);
          mMediaView.setVisibility(View.VISIBLE);
          break;
        case MSG_UPDATE_PROGRESS:
          ad = (NativeUnifiedADData) msg.obj;
          long remainTime = (long)Math.floor((mTotalTime - ad.getVideoCurrentPosition()) / 1000);
          mTimeText.setText("倒计时： " + remainTime + " s");
          Message message = mHandler.obtainMessage(MSG_UPDATE_PROGRESS, ad);
          mHandler.sendMessageDelayed(message, 500);
          break;
      }
    }
  }
}
