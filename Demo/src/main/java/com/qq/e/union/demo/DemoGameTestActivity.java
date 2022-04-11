package com.qq.e.union.demo;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.text.Editable;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;

import com.qq.e.ads.rewardvideo.RewardVideoAD;
import com.qq.e.ads.rewardvideo.RewardVideoADListener;
import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.util.ToastUtil;

import java.util.Locale;
import java.util.Map;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

/**
 * 试玩广告测试Demo
 * 输入试玩广告地址，在正常激励视频广告中展示。
 */
public class DemoGameTestActivity extends BaseActivity implements RewardVideoADListener {

  private static final String TAG = DemoGameTestActivity.class.getSimpleName();
  private static final String POS_ID = "6040295592058680";
  private RewardVideoAD rewardVideoAD;
  private EditText demoGameUrlEdt;
  private boolean showing;
  private String demoGameUrlDefault = "https://developers.adnet.qq.com/open/tryable?debug=unsdk";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_demo_game);
    demoGameUrlEdt = findViewById(R.id.demo_game_url);
    demoGameUrlEdt.setText(demoGameUrlDefault);
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
      case R.id.show_ad_button:
        if (checkDemoGameUrl()) {
          if (showing) {
            return;
          }
          boolean volumeOn = ((CheckBox) findViewById(R.id.volume_on_checkbox)).isChecked();
          rewardVideoAD = new RewardVideoAD(this, POS_ID, this, volumeOn);
          rewardVideoAD.loadAD();
        }
        break;
    }
  }

  private boolean checkDemoGameUrl() {
    Editable urlEditable = demoGameUrlEdt.getText();
    if (urlEditable == null || urlEditable.length() == 0) {
      ToastUtil.s("请输入试玩广告地址");
      return false;
    } else if (!Patterns.WEB_URL.matcher(urlEditable).matches()) {
      ToastUtil.l("请输入有效的试玩广告地址");
      return false;
    }
    GDTAdSdk.getGDTAdManger().getDevTools().testDemoGame(DemoGameTestActivity.this, urlEditable.toString());
    return true;
  }

  /**
   * 广告加载成功，可在此回调后进行广告展示
   **/
  @Override
  public void onADLoad() {
    Log.i(TAG, "onADLoad");
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
    showing = true;
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
   */
  @Override
  public void onReward(Map<String, Object> map) {
    Log.i(TAG, "onReward");
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
    showing = false;
    Log.i(TAG, "onADClose");
  }

  /**
   * 广告流程出错
   */
  @Override
  public void onError(AdError adError) {
    showing = false;
    String msg = String.format(Locale.getDefault(), "onError, error code: %d, error msg: %s",
            adError.getErrorCode(), adError.getErrorMsg());
    ToastUtil.l(msg);
    Log.i(TAG, "onError, adError=" + msg);
  }
}
