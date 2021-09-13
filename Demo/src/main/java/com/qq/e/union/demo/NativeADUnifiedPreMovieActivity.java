package com.qq.e.union.demo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.androidquery.AQuery;
import com.androidquery.callback.AjaxStatus;
import com.androidquery.callback.BitmapAjaxCallback;
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

public class NativeADUnifiedPreMovieActivity extends Activity implements NativeADUnifiedListener {

  private AQuery mAQuery;
  private Button mDownloadButton;
  private NativeUnifiedADData mAdData;
  private H mHandler = new H();
  private static final int MSG_INIT_AD = 0;
  private static final int MSG_VIDEO_START = 1;
  private static final int AD_COUNT = 1;
  private static final String TAG = NativeADUnifiedPreMovieActivity.class.getSimpleName();

  // 与广告有关的变量，用来显示广告素材的UI
  private NativeUnifiedAD mAdManager;
  private MediaView mMediaView;
  private ImageView mImagePoster;
  private NativeAdContainer mContainer;
  private LinearLayout native3imgContainer;

  private ImageButton mCloseButton;

  private boolean mIsLoading = false;

  private boolean mBindToCustomView;

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_unified_ad_premovie);
    initView();

    mBindToCustomView = getIntent().getBooleanExtra(Constants.BUTTON_BIND_TO_CUSTOM_VIEW, false);


    mAdManager = new NativeUnifiedAD(this, getPosId(), this);
    mAdManager.setMinVideoDuration(getMinVideoDuration());
    mAdManager.setMaxVideoDuration(getMaxVideoDuration());
    // 下面设置项为海外流量使用，国内暂不支持
    mAdManager.setVastClassName("com.qq.e.union.demo.adapter.vast.unified.ImaNativeDataAdapter");
  }

  private void initView() {
    mMediaView = findViewById(R.id.gdt_media_view);
    mImagePoster = findViewById(R.id.img_poster);
    mDownloadButton = findViewById(R.id.btn_download);
    mContainer = findViewById(R.id.native_ad_container);
    mAQuery = new AQuery(findViewById(R.id.native_ad_container));
    native3imgContainer = findViewById(R.id.native_3img_ad_container);
    mCloseButton = findViewById(R.id.close_button);
    mCloseButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        mContainer.setVisibility(View.GONE);
        mCloseButton.setVisibility(View.GONE);
        if(mAdData != null){
          mAdData.destroy();
        }
      }
    });

    findViewById(R.id.load_ad_button).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        if(mIsLoading){
          return;
        }
        if(mAdData != null){
          mAdData.destroy();
        }
        mIsLoading = true;
        mContainer.setVisibility(View.VISIBLE);
        if(mCloseButton!=null) {
          mCloseButton.setVisibility(View.VISIBLE);
        }

        mAdManager.loadData(AD_COUNT);
      }
    });
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
    ArrayList<ImageView>imageViews = new ArrayList<>();
    if(ad.getAdPatternType() == AdPatternType.NATIVE_2IMAGE_2TEXT ||
        ad.getAdPatternType() == AdPatternType.NATIVE_1IMAGE_2TEXT){
      // 双图双文、单图双文：注册mImagePoster的点击事件
      clickableViews.add(mImagePoster);
      imageViews.add(mImagePoster);
    } else if(ad.getAdPatternType() == AdPatternType.NATIVE_3IMAGE){
      // 三小图广告：注册native_3img_ad_container的点击事件
      clickableViews.add(native3imgContainer);
      imageViews.add(native3imgContainer.findViewById(R.id.img_1));
      imageViews.add(native3imgContainer.findViewById(R.id.img_2));
      imageViews.add(native3imgContainer.findViewById(R.id.img_3));
    }
    //作为customClickableViews传入，点击不进入详情页，直接下载或进入落地页，图文、视频广告均生效，
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
    } else if (ad.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
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

    Log.d(TAG, "isSkippable : " + mAdData.isSkippable());

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
      setVisibilityFor3Img(false);
      mMediaView.setVisibility((patternType == AdPatternType.NATIVE_VIDEO) ? View.VISIBLE :View.GONE);
      mImagePoster.setVisibility(View.VISIBLE);
      mAQuery.id(R.id.img_logo).image(ad.getIconUrl(), false, true);
      mAQuery.id(R.id.text_title).text(ad.getTitle());
      mAQuery.id(R.id.text_desc).text(ad.getDesc());
    } else if (patternType == AdPatternType.NATIVE_3IMAGE) {
      mAQuery.id(R.id.text_title).text(ad.getTitle());
      mAQuery.id(R.id.text_desc).text(ad.getDesc());
      setVisibilityFor3Img(true);
      mImagePoster.setVisibility(View.GONE);
      mMediaView.setVisibility(View.GONE);
    } else if (patternType == AdPatternType.NATIVE_1IMAGE_2TEXT) {
      mAQuery.id(R.id.img_poster).clear();
      mAQuery.id(R.id.text_title).text(ad.getTitle());
      mAQuery.id(R.id.text_desc).text(ad.getDesc());
      mImagePoster.setVisibility(View.GONE);
      setVisibilityFor3Img(false);
      mMediaView.setVisibility(View.GONE);
    }
  }

  private void setVisibilityFor3Img(boolean is3img) {
    findViewById(R.id.img_logo).setVisibility(is3img ? View.INVISIBLE : View.VISIBLE);
    findViewById(R.id.native_3img_ad_container).setVisibility(is3img ? View.VISIBLE : View.GONE);
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
    Toast.makeText(this, "未拉取到广告！", Toast.LENGTH_LONG).show();
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
          Toast.makeText(NativeADUnifiedPreMovieActivity.this, "拉取广告成功", Toast.LENGTH_LONG).show();
          Log.d(TAG, "eCPMLevel = " + ad.getECPMLevel() + " , " +
              "videoDuration = " + ad.getVideoDuration());
          break;
        case MSG_VIDEO_START:
          mImagePoster.setVisibility(View.GONE);
          mMediaView.setVisibility(View.VISIBLE);
          break;
      }
    }
  }
}
