package com.qq.e.union.demo;

import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.interstitial2.UnifiedInterstitialAD;
import com.qq.e.ads.interstitial2.UnifiedInterstitialADListener;
import com.qq.e.ads.interstitial2.UnifiedInterstitialMediaListener;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.listeners.ADRewardListener;
import com.qq.e.comm.listeners.NegativeFeedbackListener;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.adapter.PosIdArrayAdapter;
import com.qq.e.union.demo.util.DownloadConfirmHelper;
import com.qq.e.union.demo.util.ToastUtil;
import com.qq.e.union.demo.view.S2SBiddingDemoUtils;

import java.util.Locale;
import java.util.Map;

import static com.qq.e.union.demo.Constants.VIDEO_DURATION_SETTING_MAX;
import static com.qq.e.union.demo.Constants.VIDEO_DURATION_SETTING_MIN;


public class UnifiedInterstitialFullScreenADActivity extends BaseActivity implements OnClickListener,
    UnifiedInterstitialADListener, UnifiedInterstitialMediaListener, ADRewardListener, AdapterView.OnItemSelectedListener {

  private static final String TAG = UnifiedInterstitialFullScreenADActivity.class.getSimpleName();
  private UnifiedInterstitialAD iad;
  private String currentPosId;

  private EditText posIdEdt;
  private CheckBox btnMute;
  private Spinner spinner;
  private PosIdArrayAdapter arrayAdapter;
  private boolean mLoadSuccess;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setContentView(R.layout.activity_unified_interstitial_fullscreen_video_ad);
    spinner = findViewById(R.id.id_spinner);
    arrayAdapter = new PosIdArrayAdapter(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.unified_interstitial_video));
    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    btnMute = findViewById(R.id.btn_mute);
    btnMute.setChecked(true);
    spinner.setAdapter(arrayAdapter);
    spinner.setOnItemSelectedListener(this);

    posIdEdt = findViewById(R.id.posId);
    posIdEdt.setText(PositionId.UNIFIED_VIDEO_PICTURE_ID_LARGE);

    this.findViewById(R.id.loadIADFullScreen).setOnClickListener(this);
    this.findViewById(R.id.showIADFullScreen).setOnClickListener(this);
    this.findViewById(R.id.loadAndShowAd).setOnClickListener(this);
    this.findViewById(R.id.isAdValid).setOnClickListener(this);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void loadAd() {
    if (!TextUtils.isEmpty(mBackupPosId)) {
      posIdEdt.setText(mBackupPosId);
      mBackupPosId = null;
    }
    mLoadSuccess = false;
    iad = getIAD();
    setVideoOption();
    iad.loadFullScreenAD();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.loadIADFullScreen:
        loadAd();
        break;
      case R.id.loadAndShowAd:
        mIsLoadAndShow = true;
        loadAd();
        break;
      case R.id.showIADFullScreen:
        if (DemoUtil.isAdValid(mLoadSuccess, iad != null && iad.isValid(), true)) {
          iad.showFullScreenAD(this);
        }
        break;
      case R.id.isAdValid:
        DemoUtil.isAdValid(mLoadSuccess, iad != null && iad.isValid(), false);
        break;
      default:
        break;
    }
  }

  private void setVideoOption() {
    VideoOption.Builder builder = new VideoOption.Builder();
    VideoOption option = builder.setAutoPlayMuted(btnMute.isChecked()).build();
    iad.setVideoOption(option);
    // 如果支持奖励，最大、最小视频时长设置将不生效
    iad.setMinVideoDuration(getMinVideoDuration());
    iad.setMaxVideoDuration(getMaxVideoDuration());
  }

  private UnifiedInterstitialAD getIAD() {
    String posId = getPosId();
    Log.d(TAG, "getIAD: BiddingToken " + mS2sBiddingToken);
    if (!posId.equals(currentPosId) || iad == null || !TextUtils.isEmpty(mS2sBiddingToken)) {
      if (!TextUtils.isEmpty(mS2sBiddingToken)) {
        iad = new UnifiedInterstitialAD(this, posId, this, null, mS2sBiddingToken);
      } else {
        iad = new UnifiedInterstitialAD(this, posId, this);
      }
      ServerSideVerificationOptions options = new ServerSideVerificationOptions.Builder()
          .setCustomData("APP's custom data") // 设置插屏全屏视频服务端验证的自定义信息
          .setUserId("APP's user id for server verify") // 设置服务端验证的用户信息
          .build();
      iad.setServerSideVerificationOptions(options);
      iad.setLoadAdParams(DemoUtil.getLoadAdParams("full_screen_interstitial"));
      iad.setNegativeFeedbackListener(new NegativeFeedbackListener() {
        @Override
        public void onComplainSuccess() {
          Log.d(TAG,"onComplainSuccess");
        }
      });
      currentPosId = posId;
    }
    return iad;
  }

  @NonNull
  protected String getPosId() {
    return posIdEdt.getText().toString();
  }


  @Override
  public void onADReceive() {
    mLoadSuccess = true;
    ToastUtil.l("广告加载成功 ！ ");
    iad.setMediaListener(this);
    // 如果支持奖励，设置ADRewardListener接收onReward回调；图文广告暂不支持奖励
    iad.setRewardListener(this);
    // onADReceive之后才可调用getECPM()
    Log.d(TAG, "onADReceive， eCPMLevel = " + iad.getECPMLevel() + ", ECPM: " + iad.getECPM()
        + ", videoduration=" + iad.getVideoDuration()
        + ", adPatternType=" + iad.getAdPatternType()
        + ", testExtraInfo:" + iad.getExtraInfo().get("mp")
        + ", request_id:" + iad.getExtraInfo().get("request_id"));
    if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
      iad.setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
    }
    reportBiddingResult(iad);
    if(mIsLoadAndShow && DemoUtil.isAdValid(mLoadSuccess, iad != null && iad.isValid(), true)){
      mIsLoadAndShow = false;
      iad.showFullScreenAD(this);
    }
  }

  /**
   * 上报给优量汇服务端在开发者客户端竞价中优量汇的竞价结果，以便于优量汇服务端调整策略提供给开发者更合理的报价
   *
   * 优量汇竞价失败调用 sendLossNotification，并填入优量汇竞败原因（必填）、竞胜ADN ID（选填）、竞胜ADN报价（选填）
   * 优量汇竞价胜出调用 sendWinNotification
   * 请开发者如实上报相关参数，以保证优量汇服务端能根据相关参数调整策略，使开发者收益最大化
   */
  private void reportBiddingResult(UnifiedInterstitialAD interstitialAD) {
    DemoBiddingC2SUtils.reportBiddingWinLoss(interstitialAD);
    if (DemoUtil.isNeedSetBidECPM()) {
      interstitialAD.setBidECPM(300);
    }
  }

  @Override
  public void onVideoCached() {
    // 视频素材加载完成，在此时调用iad.show()或iad.showAsPopupWindow()视频广告不会有进度条。
    Log.i(TAG, "onVideoCached");
  }

  @Override
  public void onNoAD(AdError error) {
    String msg = String.format(Locale.getDefault(), "onNoAD, error code: %d, error msg: %s",
        error.getErrorCode(), error.getErrorMsg());
    ToastUtil.l(msg);
  }

  @Override
  public void onADOpened() {
    Log.i(TAG, "onADOpened");
  }

  @Override
  public void onADExposure() {
    Log.i(TAG, "onADExposure");
  }

  @Override
  public void onADClicked() {
    Log.i(TAG, "onADClicked");
  }

  @Override
  public void onReward(Map<String, Object> map) {
    Log.i(TAG, "onReward " + map.get(ServerSideVerificationOptions.TRANS_ID));  // 获取服务端验证的唯一 ID
  }

  @Override
  public void onADLeftApplication() {
    Log.i(TAG, "onADLeftApplication");
  }

  @Override
  public void onADClosed() {
    Log.i(TAG, "onADClosed");
  }

  @Override
  public void onRenderSuccess() {
    Log.i(TAG, "onRenderSuccess");
  }

  @Override
  public void onRenderFail() {
    Log.i(TAG, "onRenderFail");
  }

  @Override
  public void onVideoInit() {
    Log.i(TAG, "onVideoInit");
  }

  @Override
  public void onVideoLoading() {
    Log.i(TAG, "onVideoLoading");
  }

  @Override
  public void onVideoReady(long videoDuration) {
    Log.i(TAG, "onVideoReady, duration = " + videoDuration);
  }

  @Override
  public void onVideoStart() {
    Log.i(TAG, "onVideoStart");
  }

  @Override
  public void onVideoPause() {
    Log.i(TAG, "onVideoPause");
  }

  @Override
  public void onVideoComplete() {
    Log.i(TAG, "onVideoComplete");
  }

  @Override
  public void onVideoError(AdError error) {
    String msg = "onVideoError, code = " + error.getErrorCode() + ", msg = " + error.getErrorMsg();
    Log.i(TAG, msg);
    ToastUtil.l(msg);
  }

  @Override
  public void onVideoPageOpen() {
    Log.i(TAG, "onVideoPageOpen");
  }

  @Override
  public void onVideoPageClose() {
    Log.i(TAG, "onVideoPageClose");
  }

  private int getMinVideoDuration() {
    if (((CheckBox) findViewById(R.id.cbMinVideoDuration)).isChecked()) {
      try {
        int rst =
            Integer.parseInt(((EditText) findViewById(R.id.etMinVideoDuration)).getText().toString());
        if (rst > 0) {
          return rst;
        } else {
          ToastUtil.l("最小视频时长输入须大于0!");
        }
      } catch (NumberFormatException e) {
        ToastUtil.l("最小视频时长输入不是整数!");
      }
    }
    return 0;
  }

  private int getMaxVideoDuration() {
    if (((CheckBox) findViewById(R.id.cbMaxVideoDuration)).isChecked()) {
      try {
        int rst = Integer.parseInt(((EditText) findViewById(R.id.etMaxVideoDuration)).getText().toString());
        if (rst >= VIDEO_DURATION_SETTING_MIN && rst <= VIDEO_DURATION_SETTING_MAX) {
          return rst;
        } else {
          String msg = String.format("最大视频时长输入不在有效区间[%d,%d]内", VIDEO_DURATION_SETTING_MIN, VIDEO_DURATION_SETTING_MAX);
          ToastUtil.l(msg);
        }
      } catch (NumberFormatException e) {
        ToastUtil.l("最大视频时长输入不是整数!");
      }
    }
    return 0;
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    arrayAdapter.setSelectedPos(position);
    posIdEdt.setText(getResources().getStringArray(R.array.unified_interstitial_video_value)[position]);
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }
}
