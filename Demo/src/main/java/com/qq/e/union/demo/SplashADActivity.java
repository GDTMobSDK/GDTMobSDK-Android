package com.qq.e.union.demo;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.qq.e.ads.splash.SplashAD;
import com.qq.e.ads.splash.SplashADZoomOutListener;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.util.AdError;
import com.qq.e.union.demo.adapter.PosIdArrayAdapter;
import com.qq.e.union.demo.util.SplashZoomOutManager;
import com.qq.e.union.demo.view.S2SBiddingDemoUtils;
import com.qq.e.union.demo.view.ViewUtils;

/**
 * @author tysche
 */

public class SplashADActivity extends Activity implements View.OnClickListener,
    AdapterView.OnItemSelectedListener {
  private static final String TAG = "AD_DEMO_SPLASH_ZOOMOUT";
  private static final int REQ_ZOOM_OUT = 1024;
  private EditText posIdEdt;
  private String s2sBiddingToken;

  private PosIdArrayAdapter arrayAdapter;

  private ViewGroup zoomOutView;
  private CheckBox preloadSupportZoomOut;
  private CheckBox supportZoomOut;
  private CheckBox zoomOutInAnother;
  private CheckBox isFullScreen;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash_ad);
    posIdEdt = findViewById(R.id.posId);

    findViewById(R.id.splashADPreloadButton).setOnClickListener(this);
    findViewById(R.id.splashADDemoButton).setOnClickListener(this);
    findViewById(R.id.splashFetchAdOnly).setOnClickListener(this);

    Spinner spinner = findViewById(R.id.id_spinner);
    arrayAdapter = new PosIdArrayAdapter(this, android.R.layout.simple_spinner_item, getResources().getStringArray(R.array.splash_ad));
    arrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner.setAdapter(arrayAdapter);
    spinner.setOnItemSelectedListener(this);
    preloadSupportZoomOut = findViewById(R.id.checkBoxPreloadSupportZoomOut);
    Spinner devLogo = findViewById(R.id.devLogo);
    Pair<String, Integer>[] devLogoData = new Pair[] {
      new Pair<>("选择开发者 logo", 0),
      new Pair<>("长条 logo", R.drawable.gdt_splash_logo) ,
      new Pair<>("方形 logo", R.drawable.gdticon)
    };
    ArrayAdapter<Pair<String, Integer>> devLogoAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, devLogoData);
    devLogoAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    devLogo.setAdapter(devLogoAdapter);
    isFullScreen = findViewById(R.id.isFullScreen);
    isFullScreen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        devLogo.setVisibility(isChecked ? View.VISIBLE : View.GONE);
      }
    });
    supportZoomOut = findViewById(R.id.checkSupportZoomOut);
    zoomOutInAnother = findViewById(R.id.checkZoomOutInAnother);
    supportZoomOut.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        zoomOutInAnother.setVisibility(isChecked ? View.VISIBLE : View.GONE);
      }
    });
  }

  private int getDeveloperLogo() {
    return ((Pair<String, Integer>)((Spinner)findViewById(R.id.devLogo)).getSelectedItem()).second;
  }

  private Integer getFetchDelay() {
    CharSequence s = ((EditText) findViewById(R.id.fetchDelay)).getText();
    if (TextUtils.isEmpty(s)) {
      return null;
    }
    try {
      return Integer.parseInt(s.toString());
    } catch (Exception e) {
      Toast.makeText(SplashADActivity.this.getApplicationContext(), "开屏加载超时时间输入有误！",
              Toast.LENGTH_SHORT).show();
      return null;
    }
  }

  private boolean isFullScreen() {
    return isFullScreen.isChecked();
  }

  private String getPosID() {
    String posId = ((EditText) findViewById(R.id.posId)).getText().toString();
    return TextUtils.isEmpty(posId) ? PositionId.SPLASH_POS_ID : posId;
  }

  private boolean needLogo() {
    return ((CheckBox) findViewById(R.id.checkBox)).isChecked();
  }

  private boolean isPreloadSupportZoomOut(){
    return preloadSupportZoomOut.isChecked();
  }
  @Override
  public void onClick(View v) {
    cleanZoomOut();
    switch (v.getId()) {
      case R.id.splashADPreloadButton:
        //如果需要预加载支持开屏V+的广告这里adListener参数需要是SplashADZoomOutListener的实例
        SplashAD splashAD = new SplashAD(this, getPosID(), isPreloadSupportZoomOut() ? new PreloadSplashZoomOutListener() : null);
        LoadAdParams params = new LoadAdParams();
        params.setLoginAppId("testAppId");
        params.setLoginOpenid("testOpenId");
        params.setUin("testUin");
        splashAD.setLoadAdParams(params);
        splashAD.preLoad();
        break;
      case R.id.splashADDemoButton:
        startActivityForResult(getSplashActivityIntent(), REQ_ZOOM_OUT);
        break;
      case R.id.splashFetchAdOnly:
        Intent intent = getSplashActivityIntent();
        intent.putExtra("load_ad_only", true);
        startActivityForResult(intent, REQ_ZOOM_OUT);
        break;
    }
  }

  protected Intent getSplashActivityIntent() {
    return getSplashActivityIntent(SplashActivity.class);
  }

  protected Intent getSplashActivityIntent(Class<?> cls) {
    Intent intent = new Intent(SplashADActivity.this, cls);
    intent.putExtra("pos_id", getPosID());
    intent.putExtra("need_logo", needLogo());
    intent.putExtra("need_start_demo_list", false);
    boolean isSupportZoomOut = supportZoomOut.isChecked();
    intent.putExtra("support_zoom_out", isSupportZoomOut);
    if (isSupportZoomOut) {
      intent.putExtra("zoom_out_in_another", zoomOutInAnother.isChecked());
    }
    boolean fullScreen = isFullScreen();
    intent.putExtra("is_full_screen", fullScreen);
    if (fullScreen) {
      intent.putExtra("developer_logo", getDeveloperLogo());
    }
    intent.putExtra("fetch_delay", getFetchDelay());
    Log.d(TAG, "getSplashActivityIntent: BiddingToken " + s2sBiddingToken);
    if (!TextUtils.isEmpty(s2sBiddingToken)) {
      intent.putExtra(Constants.TOKEN, s2sBiddingToken);
    }
    return intent;
  }

  public void requestS2SBiddingToken(View view) {
    S2SBiddingDemoUtils.requestBiddingToken(this, getPosID(),
        GDTAdSdk.getGDTAdManger().getBuyerId(), token -> {
      s2sBiddingToken = token;
    });
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    arrayAdapter.setSelectedPos(position);
    posIdEdt.setText(getResources().getStringArray(R.array.splash_ad_value)[position]);
    //支持开屏V+的广告位,自动打开预加载支持闪挂
    if (getResources().getStringArray(R.array.splash_ad)[position].contains("V+")) {
      preloadSupportZoomOut.setChecked(true);
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }

  private void cleanZoomOut() {
    if (zoomOutView != null) {
      ViewUtils.removeFromParent(zoomOutView);
      zoomOutView = null;
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.i("AD_DEMO", requestCode+"=="+resultCode);
    if (requestCode == REQ_ZOOM_OUT && resultCode == RESULT_OK) {
      final SplashZoomOutManager zoomOutManager = SplashZoomOutManager.getInstance();
      SplashAD zoomAd = zoomOutManager.getSplashAD();
      zoomOutView = zoomOutManager.startZoomOut((ViewGroup) getWindow().getDecorView(),
              findViewById(android.R.id.content), new SplashZoomOutManager.AnimationCallBack() {

                @Override
                public void animationStart(int animationTime) {

                }

                @Override
                public void animationEnd() {
                  zoomAd.zoomOutAnimationFinish();
                }
              });
    }
  }

  /**
   * 预加载如果要求支持V+，需要让 {@link SplashADZoomOutListener#isSupportZoomOut()} 返回 true
   */
  private class PreloadSplashZoomOutListener implements SplashADZoomOutListener {

    @Override
    public void onZoomOut() {

    }

    @Override
    public void onZoomOutPlayFinish() {

    }

    @Override
    public boolean isSupportZoomOut() {
      return true;
    }

    @Override
    public void onADDismissed() {

    }

    @Override
    public void onNoAD(AdError error) {

    }

    @Override
    public void onADPresent() {

    }

    @Override
    public void onADClicked() {

    }

    @Override
    public void onADTick(long millisUntilFinished) {

    }

    @Override
    public void onADExposure() {

    }

    @Override
    public void onADLoaded(long expireTimestamp) {

    }
  }
}
