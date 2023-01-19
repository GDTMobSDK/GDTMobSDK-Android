package com.qq.e.union.demo;

import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.VideoView;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;
import com.qq.e.ads.nativ.CustomizeVideo;
import com.qq.e.ads.nativ.NativeADEventListener;
import com.qq.e.ads.nativ.NativeADUnifiedListener;
import com.qq.e.ads.nativ.NativeUnifiedAD;
import com.qq.e.ads.nativ.NativeUnifiedADData;
import com.qq.e.ads.nativ.widget.NativeAdContainer;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.util.DownloadConfirmHelper;
import com.qq.e.union.demo.util.ToastUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.Nullable;

public class NativeADUnifiedCustomVideoActivity extends BaseActivity implements NativeADUnifiedListener {

  private static final int MSG_INIT_AD = 0;
  private static final int MSG_VIDEO_START = 1;
  private static final int MSG_VIDEO_COMPLETE = 2;
  private static final int AD_COUNT = 1;
  private static final String TAG = NativeADUnifiedCustomVideoActivity.class.getSimpleName();
  private final H mHandler = new H();
  private AQuery mAQuery;
  private NativeUnifiedADData mAdData;
  private CustomizeVideo mCustomizeVideo;
  // 与广告有关的变量，用来显示广告素材的UI
  private NativeUnifiedAD mAdManager;
  private ImageView mImagePoster;
  private VideoView mVideoView;
  private ViewGroup mCustomContainer;
  private NativeAdContainer mContainer;
  private Button mDownloadButton;

  private boolean mIsLoading;

  private boolean mBindToCustomView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_unified_ad_custom_video);
    initView();

    mBindToCustomView = getIntent().getBooleanExtra(Constants.BUTTON_BIND_TO_CUSTOM_VIEW, false);

    mAdManager = new NativeUnifiedAD(this, getPosId(), this);
    mAdManager.setMinVideoDuration(getMinVideoDuration());
    mAdManager.setMaxVideoDuration(getMaxVideoDuration());
  }

  private void initView() {
    mImagePoster = findViewById(R.id.img_poster);
    mDownloadButton = findViewById(R.id.btn_download);
    mVideoView = findViewById(R.id.gdt_media_view);
    mContainer = findViewById(R.id.native_ad_container);
    mCustomContainer = findViewById(R.id.native_ad_custom_container);
    mAQuery = new AQuery(mContainer);
  }

  public void loadAd(View view) {
    if (mIsLoading) {
      return;
    }
    mIsLoading = true;
    if (mAdData != null) {
      mAdData.destroy();
    }
    mAdManager.loadData(AD_COUNT);
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
    mIsLoading = false;
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
    //点击直接下载（App广告）或进入落地页
    List<View> clickableViews = new ArrayList<>();
    List<View> customClickableViews = new ArrayList<>();
    if (mBindToCustomView) {
      customClickableViews.add(mDownloadButton);
    } else {
      clickableViews.add(mDownloadButton);
    }
    clickableViews.add(mImagePoster);
    clickableViews.add(mVideoView);
    //作为customClickableViews传入，点击不进入详情页，直接下载或进入落地页，图文、视频广告均生效，
    if (ad.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
      ad.bindAdToCustomVideo(mCustomContainer, this, clickableViews, customClickableViews);
    } else {
      ad.bindAdToView(this, mContainer, null, clickableViews, customClickableViews);
    }
    //如果需要获得点击view的信息使用NativeADEventListenerWithClickInfo代替NativeADEventListener
    ad.setNativeAdEventListener(new NativeADEventListener() {
      @Override
      public void onADExposed() {
        Log.d(TAG, "onADExposed: ");
      }

      @Override
      public void onADClicked() {
        Log.d(TAG, "onADClicked: ");
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
    if (ad.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
      mHandler.sendEmptyMessage(MSG_VIDEO_START);
    }

    NativeADUnifiedSampleActivity.updateAdAction(mDownloadButton, ad);
  }

  @Override
  protected void onResume() {
    super.onResume();
    mVideoView.start();
    if (mCustomizeVideo != null) {
      mCustomizeVideo.reportVideoResume(mVideoView.getCurrentPosition());
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    mVideoView.pause();
    if (mCustomizeVideo != null) {
      mCustomizeVideo.reportVideoPause(mVideoView.getCurrentPosition());
    }
  }

  private void renderAdUi(NativeUnifiedADData ad) {
    int patternType = ad.getAdPatternType();
    mImagePoster.setVisibility(View.VISIBLE);
    mAQuery.id(R.id.img_logo).image(ad.getIconUrl(), false, true);
    mAQuery.id(R.id.text_title).text(ad.getTitle());
    mAQuery.id(R.id.text_desc).text(ad.getDesc());
    mAQuery.id(R.id.img_poster).image(ad.getImgUrl(), false, true, 0, 0,
        new BitmapAjaxCallback() {
          @Override
          protected void callback(String url, ImageView iv, Bitmap bm, AjaxStatus status) {
            if (iv.getVisibility() == View.VISIBLE) {
              iv.setImageBitmap(bm);
            }
          }
        });
    if (patternType == AdPatternType.NATIVE_VIDEO) {
      mVideoView.setVisibility(View.VISIBLE);
      mCustomizeVideo = ad.getCustomizeVideo();
      if (mCustomizeVideo != null) {
        Uri uri = Uri.parse(mCustomizeVideo.getVideoUrl());
        mVideoView.setVideoURI(uri);
        mVideoView.start();
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
          @Override
          public void onCompletion(MediaPlayer mp) {
            if (mCustomizeVideo != null) {
              mCustomizeVideo.reportVideoCompleted();
              mHandler.sendEmptyMessage(MSG_VIDEO_COMPLETE);
            }
          }
        });
        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
          @Override
          public boolean onError(MediaPlayer mp, int what, int extra) {
            if (mCustomizeVideo != null) {
              mCustomizeVideo.reportVideoError(mVideoView.getCurrentPosition(), what, extra);
            }
            return false;
          }
        });
        mCustomizeVideo.reportVideoStart();

      }
    } else {
      mVideoView.setVisibility(View.GONE);
    }
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (mAdData != null) {
      // 必须要在Actiivty.destroy()时通知到广告数据，以便释放内存
      mAdData.destroy();
    }
  }

  @Override
  public void onNoAD(AdError error) {
    mIsLoading = false;
    Log.d(TAG, "onNoAd error code: " + error.getErrorCode()
        + ", error msg: " + error.getErrorMsg());
    ToastUtil.s("未拉取到广告！");
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
          ToastUtil.s("拉取广告成功");
          Log.d(TAG, "eCPMLevel = " + ad.getECPMLevel() + " , " +
              "videoDuration = " + ad.getVideoDuration());
          break;
        case MSG_VIDEO_START:
          mImagePoster.setVisibility(View.GONE);
          break;
        case MSG_VIDEO_COMPLETE:
          mImagePoster.setVisibility(View.VISIBLE);
          mVideoView.setVisibility(View.GONE);
          break;
      }
    }
  }
}