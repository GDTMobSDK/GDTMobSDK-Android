package com.qq.e.union.demo;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;

import com.qq.e.ads.rewardvideo.RewardVideoAD;
import com.qq.e.ads.rewardvideo.RewardVideoADListener;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.listeners.NegativeFeedbackListener;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.adapter.PosIdArrayAdapter;
import com.qq.e.union.demo.util.DownloadConfirmHelper;
import com.qq.e.union.demo.util.ToastUtil;
import com.qq.e.union.demo.view.S2SBiddingDemoUtils;

import java.util.Locale;
import java.util.Map;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

/**
 * 激励视频广告基本接入示例，演示了基本的激励视频广告功能（1.初始化激励视频广告;2.加载激励视频广告;3.展示激励视频广告）。
 * <p>
 * Created by chaotao on 2018/10/8.
 */

public class RewardVideoActivity extends BaseActivity implements RewardVideoADListener,
        AdapterView.OnItemSelectedListener {

  private static final String TAG = RewardVideoActivity.class.getSimpleName();
  private RewardVideoAD mRewardVideoAD;
  private EditText mPosIdEdt;
  private String mCurrentPosId;
  private boolean mCurrentVolumeOn;

  private Spinner mSpinner;
  private PosIdArrayAdapter mArrayAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    setContentView(R.layout.activity_reward_video);
    mPosIdEdt = findViewById(R.id.position_id);

    mSpinner = findViewById(R.id.id_spinner);
    mArrayAdapter = new PosIdArrayAdapter(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.reward_video));
    mArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mSpinner.setAdapter(mArrayAdapter);
    mSpinner.setOnItemSelectedListener(this);
    super.onCreate(savedInstanceState);
  }

  @Override
  protected void loadAd(){
    if (!TextUtils.isEmpty(mBackupPosId)) {
      mPosIdEdt.setText(mBackupPosId);
      mBackupPosId = null;
    }
    // 1. 初始化激励视频广告
    mRewardVideoAD = getRewardVideoAD();
    mIsLoadSuccess = false;
    // 2. 加载激励视频广告
    mRewardVideoAD.loadAD();
  }

  public void onClick(View view) {
    switch (view.getId()) {
      case R.id.change_orientation_button:
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == ORIENTATION_PORTRAIT) {
          setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else if (currentOrientation == ORIENTATION_LANDSCAPE) {
          setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        break;
      case R.id.load_ad_button:
        loadAd();
        break;
      case R.id.load_ad_and_show_ad_button:
        mIsLoadAndShow = true;
        loadAd();
        break;
      case R.id.is_ad_valid_button:
        DemoUtil.isAdValid(mIsLoadSuccess, mRewardVideoAD != null && mRewardVideoAD.isValid(), false);
        break;
      case R.id.show_ad_button:
      case R.id.show_ad_button_activity:
        // 3. 展示激励视频广告
        if (DemoUtil.isAdValid(mIsLoadSuccess, mRewardVideoAD != null && mRewardVideoAD.isValid(), true)) {
          if (view.getId() == R.id.show_ad_button) {
            mRewardVideoAD.showAD();
          } else {
            mRewardVideoAD.showAD(RewardVideoActivity.this);
          }
        }
        break;
    }
  }

  protected RewardVideoAD getRewardVideoAD() {
    String editPosId = getPosId();
    boolean volumeOn = ((CheckBox) findViewById(R.id.volume_on_checkbox)).isChecked();
    RewardVideoAD rvad;
    Log.d(TAG, "getRewardVideoAD: BiddingToken " + mS2sBiddingToken);
    if (mRewardVideoAD == null || !editPosId.equals(mCurrentPosId) || volumeOn != mCurrentVolumeOn
        || !TextUtils.isEmpty(mS2sBiddingToken)) {
      if (!TextUtils.isEmpty(mS2sBiddingToken)) {
        rvad = new RewardVideoAD(this, editPosId, this, volumeOn, mS2sBiddingToken);
      } else {
        rvad = new RewardVideoAD(this, editPosId, this, volumeOn);
      }
      rvad.setNegativeFeedbackListener(new NegativeFeedbackListener() {
        @Override
        public void onComplainSuccess() {
          Log.i(TAG, "onComplainSuccess");
        }
      });
      ServerSideVerificationOptions options = new ServerSideVerificationOptions.Builder()
              .setCustomData("APP's custom data") // 设置激励视频服务端验证的自定义信息
              .setUserId("APP's user id for server verify") // 设置服务端验证的用户信息
              .build();
      rvad.setServerSideVerificationOptions(options);
      rvad.setLoadAdParams(DemoUtil.getLoadAdParams("reward_video"));
      mCurrentPosId = editPosId;
      mCurrentVolumeOn = volumeOn;
    } else {
      rvad = this.mRewardVideoAD;
    }
    return rvad;
  }

  @NonNull
  protected String getPosId() {
    return mPosIdEdt.getText().toString();
  }


  /**
   * 广告加载成功，可在此回调后进行广告展示
   **/
  @Override
  public void onADLoad() {
    ToastUtil.l("广告加载成功 ！ ");
    if (mRewardVideoAD.getRewardAdType() == RewardVideoAD.REWARD_TYPE_VIDEO) {
      Log.d(TAG, "eCPMLevel = " + mRewardVideoAD.getECPMLevel() + ", ECPM: " + mRewardVideoAD.getECPM()
          + " ,video duration = " + mRewardVideoAD.getVideoDuration()
          + ", testExtraInfo:" + mRewardVideoAD.getExtraInfo().get("mp")
          + ", request_id:" + mRewardVideoAD.getExtraInfo().get("request_id"));
    } else if (mRewardVideoAD.getRewardAdType() == RewardVideoAD.REWARD_TYPE_PAGE) {
      Log.d(TAG, "eCPMLevel = " + mRewardVideoAD.getECPMLevel()
          + ", ECPM: " + mRewardVideoAD.getECPM()
          + ", testExtraInfo:" + mRewardVideoAD.getExtraInfo().get("mp")
          + ", request_id:" + mRewardVideoAD.getExtraInfo().get("request_id"));
    }
    if (DownloadConfirmHelper.USE_CUSTOM_DIALOG) {
      mRewardVideoAD.setDownloadConfirmListener(DownloadConfirmHelper.DOWNLOAD_CONFIRM_LISTENER);
    }
    reportBiddingResult(mRewardVideoAD);
    mIsLoadSuccess = true;
    if (mIsLoadAndShow && DemoUtil.isAdValid(true, mRewardVideoAD != null && mRewardVideoAD.isValid(), true)) {
      mRewardVideoAD.showAD();
      mIsLoadAndShow = false;
    }
  }

  /**
   * 上报给优量汇服务端在开发者客户端竞价中优量汇的竞价结果，以便于优量汇服务端调整策略提供给开发者更合理的报价
   *
   * 优量汇竞价失败调用 sendLossNotification，并填入优量汇竞败原因（必填）、竞胜ADN ID（选填）、竞胜ADN报价（选填）
   * 优量汇竞价胜出调用 sendWinNotification
   * 请开发者如实上报相关参数，以保证优量汇服务端能根据相关参数调整策略，使开发者收益最大化
   */
  private void reportBiddingResult(RewardVideoAD rewardVideoAD) {
    DemoBiddingC2SUtils.reportBiddingWinLoss(rewardVideoAD);
    if (DemoUtil.isNeedSetBidECPM()) {
      rewardVideoAD.setBidECPM(300);
    }
  }

  /**
   * 视频素材缓存成功，可在此回调后进行广告展示
   */
  @Override
  public void onVideoCached() {
    Log.i(TAG, "onVideoCached");
  }

  /**
   * 激励视频广告页面展示
   */
  @Override
  public void onADShow() {
    Log.i(TAG, "onADShow");
  }

  /**
   * 激励视频广告曝光
   */
  @Override
  public void onADExpose() {
    Log.i(TAG, "onADExpose");
  }

  /**
   * 激励视频触发激励（观看视频大于一定时长或者视频播放完毕）
   *
   * @param map 若选择了服务端验证，可以通过 ServerSideVerificationOptions#TRANS_ID 键从 map 中获取此次交易的 id；若未选择服务端验证，则不需关注 map 参数。
   */
  @Override
  public void onReward(Map<String, Object> map) {
    Log.i(TAG, "onReward " + map.get(ServerSideVerificationOptions.TRANS_ID));  // 获取服务端验证的唯一 ID
  }

  /**
   * 激励视频广告被点击
   */
  @Override
  public void onADClick() {
    Log.i(TAG, "onADClick");
  }

  /**
   * 激励视频播放完毕
   */
  @Override
  public void onVideoComplete() {
    Log.i(TAG, "onVideoComplete");
  }

  /**
   * 激励视频广告被关闭
   */
  @Override
  public void onADClose() {
    Log.i(TAG, "onADClose");
  }

  /**
   * 广告流程出错
   */
  @Override
  public void onError(AdError adError) {
    String msg = String.format(Locale.getDefault(), "onError, error code: %d, error msg: %s",
        adError.getErrorCode(), adError.getErrorMsg());
    ToastUtil.s(msg);
    Log.i(TAG, "onError, adError=" + msg);
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    mArrayAdapter.setSelectedPos(position);
    mPosIdEdt.setText(getResources().getStringArray(R.array.reward_video_value)[position]);
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }
}
