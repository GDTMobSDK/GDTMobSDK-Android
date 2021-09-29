package com.qq.e.union.demo;

import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Toast;

import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.nativ.ADSize;
import com.qq.e.ads.nativ.NativeExpressAD;
import com.qq.e.ads.nativ.NativeExpressADView;
import com.qq.e.ads.nativ.NativeExpressMediaListener;
import com.qq.e.comm.constants.AdPatternType;
import com.qq.e.comm.constants.BiddingLossReason;
import com.qq.e.comm.pi.AdData;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.util.DownloadConfirmHelper;

import java.util.List;

/**
 * 原生模板广告基本接入示例，演示了基本的原生模板广告功能，包括广告尺寸的ADSize.FULL_WIDTH，ADSize.AUTO_HEIGHT功能
 *
 * Created by noughtchen on 2017/4/17.
 */

public class NativeExpressDemoActivity extends Activity implements View.OnClickListener,
        NativeExpressAD.NativeExpressADListener, CompoundButton.OnCheckedChangeListener {

  private static final String TAG = "ExpressDemoActivity";
  private ViewGroup container;
  private NativeExpressAD nativeExpressAD;
  private NativeExpressADView nativeExpressADView;
  private Button buttonRefresh,buttonPreloadVideo, buttonResize;
  private EditText editTextWidth, editTextHeight; // 编辑框输入的宽高
  private int adWidth, adHeight; // 广告宽高
  private CheckBox checkBoxFullWidth, checkBoxAutoHeight;
  private boolean isAdFullWidth, isAdAutoHeight; // 是否采用了ADSize.FULL_WIDTH，ADSize.AUTO_HEIGHT
  private boolean isPreloadVideo;
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_native_express_demo);
    container = (ViewGroup) findViewById(R.id.container);
    editTextWidth = (EditText) findViewById(R.id.editWidth);
    editTextHeight = (EditText) findViewById(R.id.editHeight);
    buttonRefresh = (Button) findViewById(R.id.buttonRefresh);
    buttonPreloadVideo = (Button) findViewById(R.id.buttonPreloadVideo);
    buttonResize = (Button) findViewById(R.id.buttonDestroy);
    buttonRefresh.setOnClickListener(this);
    buttonPreloadVideo.setOnClickListener(this);
    buttonResize.setOnClickListener(this);
    checkBoxFullWidth = (CheckBox) findViewById(R.id.checkboxFullWidth);
    checkBoxAutoHeight =  (CheckBox) findViewById(R.id.checkboxAutoHeight);
    checkBoxFullWidth.setOnCheckedChangeListener(this);
    checkBoxAutoHeight.setOnCheckedChangeListener(this);
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    // 使用完了每一个NativeExpressADView之后都要释放掉资源
    if (nativeExpressADView != null) {
      nativeExpressADView.destroy();
    }
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
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.buttonRefresh:
        isPreloadVideo = false;
        refreshAd();
        break;
      case R.id.buttonPreloadVideo:
        isPreloadVideo = true;
        refreshAd();
        break;
      case R.id.buttonDestroy:
        resizeAd();
        break;
    }
  }

  private void refreshAd() {
    try {
      if (checkEditTextEmpty()) {
        return;
      }

      adWidth = Integer.valueOf(editTextWidth.getText().toString());
      adHeight = Integer.valueOf(editTextHeight.getText().toString());
      hideSoftInput();
      /**
       *  如果选择支持视频的模板样式，请使用{@link PositionId#NATIVE_EXPRESS_SUPPORT_VIDEO_POS_ID}
       */
      String token = getToken();
      Log.d(TAG, "refreshAd: BiddingToken " + token);
      if (!TextUtils.isEmpty(token)) {
        nativeExpressAD = new NativeExpressAD(this, getMyADSize(), getPosId(), this, token);
      } else {
        nativeExpressAD = new NativeExpressAD(this, getMyADSize(), getPosId(), this); // 这里的Context必须为Activity
      }
      VideoOption option = NativeExpressADActivity.getVideoOption(getIntent());
      if(option != null){
        // setVideoOption是可选的，开发者可根据需要选择是否配置
        nativeExpressAD.setVideoOption(option);
      }
      nativeExpressAD.setMinVideoDuration(getMinVideoDuration());
      nativeExpressAD.setMaxVideoDuration(getMaxVideoDuration());
      nativeExpressAD.loadAD(1, DemoUtil.getLoadAdParams("native_express"));
    } catch (NumberFormatException e) {
      Log.w(TAG, "ad size invalid.");
      Toast.makeText(this, "请输入合法的宽高数值", Toast.LENGTH_SHORT).show();
    }
  }

  private ADSize getMyADSize() {
    int w = isAdFullWidth ? ADSize.FULL_WIDTH : adWidth;
    int h = isAdAutoHeight ? ADSize.AUTO_HEIGHT : adHeight;
    return new ADSize(w, h);
  }

  /**
   *
   * 如何设置广告的尺寸：
   *
   * 方法一：
   * 在接入、调试模板广告的过程中，可以利用NativeExpressADView.setAdSize这个方法调整广告View的大小，找到适合自己的广告位尺寸。
   * 发布时，把这个ADSize固定下来，并在构造NativeExpressAD的时候传入，给一个固定的广告位ID去使用。
   *
   * 方法二：
   * 根据App需要，可以选择一个固定的宽度（宽度也可以设置为ADSize.FULL_WIDTH让广告宽度铺满父控件，但是不能过小，否则将展示不完整），
   * 然后把广告的高度设置为ADSize.AUTO_HEIGHT，让广告的高度根据宽度去自适应。（我们建议开发者选择这种方法，但是目前AUTO_HEIGHT还不支持双图双文模板）
   *
   * 注意：setAdSize是NativeExpressADView的方法，而不是NativeExpressAD的方法，
   * 调用setAdSize只会对当前的NativeExpressADView尺寸进行改变，而不会影响NativeExpressADView加载出来的其他广告尺寸。
   */
  private void resizeAd() {
    if (nativeExpressADView == null) {
      return;
    }

    try {
      if (checkEditTextEmpty()) {
        return;
      }
      if (checkEditTextChanged()) {
        adWidth = Integer.valueOf(editTextWidth.getText().toString());
        adHeight = Integer.valueOf(editTextHeight.getText().toString());
        nativeExpressADView.setAdSize(getMyADSize());
        hideSoftInput();
      }
    } catch (NumberFormatException e) {
      Log.w(TAG, "ad size invalid.");
      Toast.makeText(this, "请输入合法的宽高数值", Toast.LENGTH_SHORT).show();
    }
  }

  /**
   * 获取广告数据
   *
   * @param nativeExpressADView
   * @return
   */
  private String getAdInfo(NativeExpressADView nativeExpressADView) {
    AdData adData = nativeExpressADView.getBoundData();
    if (adData != null) {
      StringBuilder infoBuilder = new StringBuilder();
      infoBuilder.append("title:").append(adData.getTitle()).append(",")
          .append("desc:").append(adData.getDesc()).append(",")
          .append("patternType:").append(adData.getAdPatternType());
      if (adData.getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
        infoBuilder.append(", video info: ").append(getVideoInfo(adData.getProperty(AdData.VideoPlayer.class)));
      }
      Log.d(TAG, "eCPMLevel = " + adData.getECPMLevel()+ ", ECPM: " + adData.getECPM()
          + ", videoDuration = " + adData.getVideoDuration()
          + ", testExtraInfo:" + adData.getExtraInfo().get("mp"));
      return infoBuilder.toString();
    }
    return null;
  }

  /**
   * 获取播放器实例
   *
   * 仅当视频回调{@link NativeExpressMediaListener#onVideoInit(NativeExpressADView)}调用后才会有返回值
   *
   * @param videoPlayer
   * @return
   */
  private String getVideoInfo(AdData.VideoPlayer videoPlayer) {
    if (videoPlayer != null) {
      StringBuilder videoBuilder = new StringBuilder();
      videoBuilder.append("{state:").append(videoPlayer.getVideoState()).append(",")
          .append("duration:").append(videoPlayer.getDuration()).append(",")
          .append("position:").append(videoPlayer.getCurrentPosition()).append("}");
      return videoBuilder.toString();
    }
    return null;
  }

  @Override
  public void onNoAD(AdError adError) {
    Log.i(
        TAG,
        String.format("onNoAD, error code: %d, error msg: %s", adError.getErrorCode(),
            adError.getErrorMsg()));
  }

  @Override
  public void onADLoaded(List<NativeExpressADView> adList) {
    Log.i(TAG, "onADLoaded: " + adList.size());
    // 释放前一个展示的NativeExpressADView的资源
    if (nativeExpressADView != null) {
      nativeExpressADView.destroy();
    }

    if (container.getVisibility() != View.VISIBLE) {
      container.setVisibility(View.VISIBLE);
    }

    if (container.getChildCount() > 0) {
      container.removeAllViews();
    }

    nativeExpressADView = adList.get(0);
    reportBiddingResult(nativeExpressADView);
    if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
      nativeExpressADView.setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
    }
    Log.i(TAG, "onADLoaded, video info: " + getAdInfo(nativeExpressADView) + ", getAdNetWorkName: " +
        nativeExpressAD.getAdNetWorkName());
    if (nativeExpressADView.getBoundData().getAdPatternType() == AdPatternType.NATIVE_VIDEO) {
      nativeExpressADView.setMediaListener(mediaListener);
      if(isPreloadVideo) {
        // 预加载视频素材，加载成功会回调mediaListener的onVideoCached方法，失败的话回调onVideoError方法errorCode为702。
        nativeExpressADView.preloadVideo();
      }
    } else {
      isPreloadVideo = false;
    }
    if(!isPreloadVideo) {
      // 广告可见才会产生曝光，否则将无法产生收益。
      container.addView(nativeExpressADView);
      nativeExpressADView.render();
    }
  }

  /**
   * 上报给优量汇服务端在开发者客户端竞价中优量汇的竞价结果，以便于优量汇服务端调整策略提供给开发者更合理的报价
   *
   * 优量汇竞价失败调用 sendLossNotification，并填入优量汇竞败原因（必填）、竞胜ADN ID（选填）、竞胜ADN报价（选填）
   * 优量汇竞价胜出调用 sendWinNotification，并填入开发者期望扣费价格（单位分）
   * 请开发者如实上报相关参数，以保证优量汇服务端能根据相关参数调整策略，使开发者收益最大化
   */
  private void reportBiddingResult(NativeExpressADView adData) {
    if (DemoUtil.isReportBiddingLoss() == DemoUtil.REPORT_BIDDING_LOSS) {
      adData.sendLossNotification(100, BiddingLossReason.LOW_PRICE, "WinAdnID");
    } else if (DemoUtil.isReportBiddingLoss() == DemoUtil.REPORT_BIDDING_WIN) {
      adData.sendWinNotification(200);
    }
    if (DemoUtil.isNeedSetBidECPM()) {
      adData.setBidECPM(300);
    }
  }

  @Override
  public void onRenderFail(NativeExpressADView adView) {
    Log.i(TAG, "onRenderFail");
  }

  @Override
  public void onRenderSuccess(NativeExpressADView adView) {
    Log.i(TAG, "onRenderSuccess");
  }

  @Override
  public void onADExposure(NativeExpressADView adView) {
    Log.i(TAG, "onADExposure");
  }

  @Override
  public void onADClicked(NativeExpressADView adView) {
    Log.i(TAG, "onADClicked" + adView.ext.get("clickUrl"));
  }

  @Override
  public void onADClosed(NativeExpressADView adView) {
    Log.i(TAG, "onADClosed");
    // 当广告模板中的关闭按钮被点击时，广告将不再展示。NativeExpressADView也会被Destroy，释放资源，不可以再用来展示。
    if (container != null && container.getChildCount() > 0) {
      container.removeAllViews();
      container.setVisibility(View.GONE);
    }
  }

  @Override
  public void onADLeftApplication(NativeExpressADView adView) {
    Log.i(TAG, "onADLeftApplication");
  }

  @Override
  public void onADOpenOverlay(NativeExpressADView adView) {
    Log.i(TAG, "onADOpenOverlay，即将废弃");
  }

  @Override
  public void onADCloseOverlay(NativeExpressADView adView) {
    Log.i(TAG, "onADCloseOverlay，即将废弃");
  }

  private boolean checkEditTextEmpty() {
    String width = editTextWidth.getText().toString();
    String height = editTextHeight.getText().toString();
    if (TextUtils.isEmpty(width) || TextUtils.isEmpty(height)) {
      Toast.makeText(this, "请先输入广告位的宽、高！", Toast.LENGTH_SHORT).show();
      return true;
    }

    return false;
  }

  private boolean checkEditTextChanged() {
    return Integer.valueOf(editTextWidth.getText().toString()) != adWidth
        || Integer.valueOf(editTextHeight.getText().toString()) != adHeight;
  }

  // 隐藏软键盘，这只是个简单的隐藏软键盘示例实现，与广告sdk功能无关
  private void hideSoftInput() {
    if (getCurrentFocus() == null || getCurrentFocus().getWindowToken() == null) {
      return;
    }

    ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(
        NativeExpressDemoActivity.this.getCurrentFocus().getWindowToken(),
        InputMethodManager.HIDE_NOT_ALWAYS);
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (buttonView.getId() == R.id.checkboxFullWidth) {
      if (isChecked) {
        isAdFullWidth = true;
        editTextWidth.setText("-1");
        editTextWidth.setEnabled(false);
      } else {
        isAdFullWidth = false;
        editTextWidth.setText("340");
        editTextWidth.setEnabled(true);
      }
    }

    if (buttonView.getId() == R.id.checkboxAutoHeight) {
      if (isChecked) {
        isAdAutoHeight = true;
        editTextHeight.setText("-2");
        editTextHeight.setEnabled(false);
      } else {
        isAdAutoHeight = false;
        editTextHeight.setText("320");
        editTextHeight.setEnabled(true);
      }
    }
  }

  /**
   * 注意：带有视频的广告被点击后会进入全屏播放视频，此时视频可以跟随屏幕方向的旋转而旋转，
   * 请开发者注意处理好自己的Activity的运行时变更，不要让Activity销毁。
   * 例如，在AndroidManifest文件中给Activity添加属性android:configChanges="keyboard|keyboardHidden|orientation|screenSize"，
   */
  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
  }

  private NativeExpressMediaListener mediaListener = new NativeExpressMediaListener() {
    @Override
    public void onVideoInit(NativeExpressADView nativeExpressADView) {
      Log.i(TAG, "onVideoInit: "
          + getVideoInfo(nativeExpressADView.getBoundData().getProperty(AdData.VideoPlayer.class)));
    }

    @Override
    public void onVideoLoading(NativeExpressADView nativeExpressADView) {
      Log.i(TAG, "onVideoLoading");
    }

    @Override
    public void onVideoCached(NativeExpressADView adView) {
      Log.i(TAG, "onVideoCached");
      // 视频素材加载完成，此时展示视频广告不会有进度条。
      if(isPreloadVideo && nativeExpressADView != null) {
        if(container.getChildCount() > 0){
          container.removeAllViews();
        }
        // 广告可见才会产生曝光，否则将无法产生收益。
        container.addView(nativeExpressADView);
        nativeExpressADView.render();
      }
    }

    @Override
    public void onVideoReady(NativeExpressADView nativeExpressADView, long l) {
      Log.i(TAG, "onVideoReady");
    }

    @Override
    public void onVideoStart(NativeExpressADView nativeExpressADView) {
      Log.i(TAG, "onVideoStart: "
          + getVideoInfo(nativeExpressADView.getBoundData().getProperty(AdData.VideoPlayer.class)));
    }

    @Override
    public void onVideoPause(NativeExpressADView nativeExpressADView) {
      Log.i(TAG, "onVideoPause: "
          + getVideoInfo(nativeExpressADView.getBoundData().getProperty(AdData.VideoPlayer.class)));
    }

    @Override
    public void onVideoComplete(NativeExpressADView nativeExpressADView) {
      Log.i(TAG, "onVideoComplete: "
          + getVideoInfo(nativeExpressADView.getBoundData().getProperty(AdData.VideoPlayer.class)));
    }

    @Override
    public void onVideoError(NativeExpressADView nativeExpressADView, AdError adError) {
      Log.i(TAG, "onVideoError");
    }

    @Override
    public void onVideoPageOpen(NativeExpressADView nativeExpressADView) {
      Log.i(TAG, "onVideoPageOpen");
    }

    @Override
    public void onVideoPageClose(NativeExpressADView nativeExpressADView) {
      Log.i(TAG, "onVideoPageClose");
    }
  };

}
