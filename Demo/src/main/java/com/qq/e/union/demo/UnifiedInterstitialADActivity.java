package com.qq.e.union.demo;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.qq.e.ads.cfg.VideoOption;
import com.qq.e.ads.interstitial2.UnifiedInterstitialAD;
import com.qq.e.ads.interstitial2.UnifiedInterstitialADListener;
import com.qq.e.ads.interstitial2.UnifiedInterstitialMediaListener;
import com.qq.e.comm.listeners.NegativeFeedbackListener;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.adapter.PosIdArrayAdapter;
import com.qq.e.union.demo.util.DownloadConfirmHelper;
import com.qq.e.union.demo.util.ToastUtil;
import com.qq.e.union.demo.view.S2SBiddingDemoUtils;

import java.util.Locale;

import static com.qq.e.union.demo.Constants.VIDEO_DURATION_SETTING_MAX;
import static com.qq.e.union.demo.Constants.VIDEO_DURATION_SETTING_MIN;


public class UnifiedInterstitialADActivity extends BaseActivity implements OnClickListener,
    UnifiedInterstitialADListener, UnifiedInterstitialMediaListener, CompoundButton.OnCheckedChangeListener, AdapterView.OnItemSelectedListener {

  private static final String TAG = UnifiedInterstitialADActivity.class.getSimpleName();
  private UnifiedInterstitialAD iad;
  private String currentPosId;
  private String s2sBiddingToken;

  private CheckBox btnNoOption;
  private CheckBox btnMute;
  private CheckBox btnDetailMute;
  private Spinner networkSpinner;

  private EditText posIdEdt;

  private Spinner spinner;
  private PosIdArrayAdapter arrayAdapter;

  private boolean isRenderFail;
  private boolean mLoadSuccess;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setContentView(R.layout.activity_unified_interstitial_ad);
    spinner = findViewById(R.id.id_spinner);
    arrayAdapter = new PosIdArrayAdapter(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.unified_interstitial));
    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(arrayAdapter);
    spinner.setOnItemSelectedListener(this);

    posIdEdt = findViewById(R.id.posId);
    posIdEdt.setText(PositionId.UNIFIED_VIDEO_PICTURE_ID_LARGE);
    this.findViewById(R.id.loadIAD).setOnClickListener(this);
    this.findViewById(R.id.showIAD).setOnClickListener(this);
    this.findViewById(R.id.showIADAsPPW).setOnClickListener(this);
    this.findViewById(R.id.closeIAD).setOnClickListener(this);
    this.findViewById(R.id.isAdValid).setOnClickListener(this);
    btnNoOption = findViewById(R.id.cb_none_video_option);
    btnNoOption.setOnCheckedChangeListener(this);
    btnMute = findViewById(R.id.btn_mute);
    btnDetailMute = findViewById(R.id.btn_detail_mute);
    networkSpinner = findViewById(R.id.spinner_network);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void loadAd() {
    if (mIsLoadAndShow) {
      posIdEdt.setText(mBackupPosId);
    }
    mLoadSuccess = false;
    iad = getIAD();
    setVideoOption();
    iad.loadAD();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    if (iad != null) {
      iad.destroy();
    }
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.loadIAD:
        loadAd();
        break;
      case R.id.showIAD:
        if (DemoUtil.isAdValid(mLoadSuccess, iad != null && iad.isValid(), true) && !isRenderFail) {
          iad.show();
        }
        break;
      case R.id.showIADAsPPW:
        if (DemoUtil.isAdValid(mLoadSuccess, iad != null && iad.isValid(), true) && !isRenderFail) {
          iad.showAsPopupWindow();
        }
        break;
      case R.id.closeIAD:
        mLoadSuccess = false;
        close();
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
    VideoOption option = builder.build();
    if(!btnNoOption.isChecked()){
      option = builder.setAutoPlayMuted(btnMute.isChecked())
          .setAutoPlayPolicy(networkSpinner.getSelectedItemPosition())
          .setDetailPageMuted(btnDetailMute.isChecked())
          .build();
    }
    iad.setVideoOption(option);
    iad.setMinVideoDuration(getMinVideoDuration());
    iad.setMaxVideoDuration(getMaxVideoDuration());

  }

  private UnifiedInterstitialAD getIAD() {
    if (this.iad != null) {
      iad.close();
      iad.destroy();
    }
    isRenderFail = false;
    String posId = getPosId();
    Log.d(TAG, "getIAD: BiddingToken " + s2sBiddingToken);
    if (!posId.equals(currentPosId) || iad == null || !TextUtils.isEmpty(s2sBiddingToken)) {
      if (!TextUtils.isEmpty(s2sBiddingToken)) {
        iad = new UnifiedInterstitialAD(this, posId, this, null, s2sBiddingToken);
      } else {
        iad = new UnifiedInterstitialAD(this, posId, this);
      }
      iad.setNegativeFeedbackListener(new NegativeFeedbackListener() {
        @Override
        public void onComplainSuccess() {
          Log.i(TAG, "onComplainSuccess");
        }
      });
      iad.setMediaListener(this);
      iad.setLoadAdParams(DemoUtil.getLoadAdParams("interstitial"));
      currentPosId = posId;
    }
    return iad;
  }

  @NonNull
  private String getPosId() {
    return posIdEdt.getText().toString();
  }

  public void requestS2SBiddingToken(View view) {
    S2SBiddingDemoUtils.requestBiddingToken(getPosId(), token -> s2sBiddingToken = token);
  }

  private void close() {
    if (iad != null) {
      iad.close();
    } else {
      ToastUtil.l("广告尚未加载 ！ ");
    }
  }

  @Override
  public void onADReceive() {
    mLoadSuccess = true;
    ToastUtil.l("广告加载成功 ！ ");
    // onADReceive之后才可调用getECPM()
    Log.d(TAG, "onADReceive eCPMLevel = " + iad.getECPMLevel()+ ", ECPM: " + iad.getECPM()
        + ", videoduration=" + iad.getVideoDuration()
        + ", testExtraInfo:" + iad.getExtraInfo().get("mp")
        + ", request_id:" + iad.getExtraInfo().get("request_id"));
    if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
      iad.setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
    }
    reportBiddingResult(iad);
    if(mIsLoadAndShow && DemoUtil.isAdValid(mLoadSuccess, iad != null && iad.isValid(), true) && !isRenderFail){
      mIsLoadAndShow = false;
      iad.show();
    }
  }

  /**
   * 上报给优量汇服务端在开发者客户端竞价中优量汇的竞价结果，以便于优量汇服务端调整策略提供给开发者更合理的报价
   *
   * 优量汇竞价失败调用 sendLossNotification，并填入优量汇竞败原因（必填）、竞胜ADN ID（选填）、竞胜ADN报价（选填）
   * 优量汇竞价胜出调用 sendWinNotification，并填入开发者期望扣费价格（单位分）
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
  public void onADLeftApplication() {
    Log.i(TAG, "onADLeftApplication");
  }

  @Override
  public void onADClosed() {
    Log.i(TAG, "onADClosed");
  }

  @Override
  public void onRenderSuccess() {
    Log.i(TAG, "onRenderSuccess，建议在此回调后再调用展示方法");
  }

  @Override
  public void onRenderFail() {
    Log.i(TAG, "onRenderFail");
    isRenderFail = true;
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
    Log.i(TAG, "onVideoError, code = " + error.getErrorCode() + ", msg = " + error.getErrorMsg());
  }

  @Override
  public void onVideoPageOpen() {
    Log.i(TAG, "onVideoPageOpen");
  }

  @Override
  public void onVideoPageClose() {
    Log.i(TAG, "onVideoPageClose");
  }

  @Override
  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    if (buttonView == btnNoOption) {
      btnMute.setEnabled(!isChecked);
      btnDetailMute.setEnabled(!isChecked);
      networkSpinner.setEnabled(!isChecked);
    }
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
        int rst = Integer.parseInt(((EditText) findViewById(R.id.etMaxVideoDuration)).getText()
            .toString());
        if (rst >= VIDEO_DURATION_SETTING_MIN && rst <= VIDEO_DURATION_SETTING_MAX) {
          return rst;
        } else {
          String msg = String.format("最大视频时长输入不在有效区间[%d,%d]内",
              VIDEO_DURATION_SETTING_MIN, VIDEO_DURATION_SETTING_MAX);
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
    posIdEdt.setText(getResources().getStringArray(R.array.unified_interstitial_value)[position]);
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }
}
