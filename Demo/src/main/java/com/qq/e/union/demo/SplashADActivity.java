package com.qq.e.union.demo;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;

import com.qq.e.ads.splash.SplashAD;
import com.qq.e.comm.constants.LoadAdParams;
import com.qq.e.union.demo.adapter.PosIdArrayAdapter;
import com.qq.e.union.demo.util.ToastUtil;

/**
 * 开屏广告接入示例入口
 */
public class SplashADActivity extends BaseActivity implements View.OnClickListener,
    AdapterView.OnItemSelectedListener {
  private static final String TAG = "SplashADActivity";
  private static final Pair<String, Integer>[] DEV_LOGO_DATA = new Pair[]{
      new Pair<>("无 Logo", 0),
      new Pair<>("长条 Logo（默认）", R.drawable.gdt_logo_white),
      new Pair<>("方形 Logo", R.drawable.gdt_icon)
  };

  private EditText mPosIdEdt;
  private CheckBox mIsFullScreen;
  private Spinner mPosIdSpinner;
  private Spinner mDevLogoSpinner;
  private PosIdArrayAdapter mPosIdArrayAdapter;
  private PosIdArrayAdapter mDevLogoArrayAdapter;

  private int mDevLogoResId;


  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_splash_ad);
    mPosIdEdt = findViewById(R.id.posId);

    findViewById(R.id.splashADPreloadButton).setOnClickListener(this);
    findViewById(R.id.splashADDemoButton).setOnClickListener(this);
    findViewById(R.id.splashFetchAdOnly).setOnClickListener(this);

    mPosIdSpinner = findViewById(R.id.id_spinner);
    mDevLogoSpinner = findViewById(R.id.devLogo);
    mPosIdArrayAdapter = new PosIdArrayAdapter(this, android.R.layout.simple_spinner_item,
        getResources().getStringArray(R.array.splash_ad));
    mPosIdArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mPosIdSpinner.setAdapter(mPosIdArrayAdapter);
    mPosIdSpinner.setOnItemSelectedListener(this);
    String[] devLogo = new String[DEV_LOGO_DATA.length];
    for (int i = 0; i < DEV_LOGO_DATA.length; i++) {
      devLogo[i] = DEV_LOGO_DATA[i].first;
    }
    mDevLogoArrayAdapter = new PosIdArrayAdapter(this, android.R.layout.simple_spinner_item, devLogo);
    mDevLogoArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    mDevLogoSpinner.setAdapter(mDevLogoArrayAdapter);
    mDevLogoSpinner.setOnItemSelectedListener(this);

    mIsFullScreen = findViewById(R.id.isFullScreen);
    mIsFullScreen.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override
      public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        // 默认展示长条 logo
        mDevLogoSpinner.setSelection(1, true);
        mDevLogoSpinner.setVisibility(isChecked ? View.VISIBLE : View.GONE);
      }
    });
  }

  private Integer getFetchDelay() {
    CharSequence s = ((EditText) findViewById(R.id.fetchDelay)).getText();
    if (TextUtils.isEmpty(s)) {
      return null;
    }
    try {
      return Integer.parseInt(s.toString());
    } catch (Exception e) {
      ToastUtil.s("开屏加载超时时间输入有误！");
      return null;
    }
  }

  private boolean isFullScreen() {
    return mIsFullScreen.isChecked();
  }

  protected String getPosId() {
    String posId = ((EditText) findViewById(R.id.posId)).getText().toString();
    return TextUtils.isEmpty(posId) ? PositionId.SPLASH_POS_ID : posId;
  }

  private boolean needLogo() {
    return ((CheckBox) findViewById(R.id.checkBox)).isChecked();
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.splashADPreloadButton:
        SplashAD splashAD = new SplashAD(this, getPosId(), null);
        LoadAdParams params = new LoadAdParams();
        params.setLoginAppId("testAppId");
        params.setLoginOpenid("testOpenId");
        params.setUin("testUin");
        splashAD.setLoadAdParams(params);
        splashAD.preLoad();
        break;
      case R.id.splashADDemoButton:
        startActivity(getSplashActivityIntent());
        break;
      case R.id.splashFetchAdOnly:
        Intent intent = getSplashActivityIntent();
        intent.putExtra("load_ad_only", true);
        startActivity(intent);
        break;
    }
  }

  protected Intent getSplashActivityIntent() {
    return getSplashActivityIntent(SplashActivity.class);
  }

  protected Intent getSplashActivityIntent(Class<?> cls) {
    Intent intent = new Intent(SplashADActivity.this, cls);
    intent.putExtra("pos_id", getPosId());
    intent.putExtra("need_logo", needLogo());
    intent.putExtra("need_start_demo_list", false);
    boolean fullScreen = isFullScreen();
    intent.putExtra("is_full_screen", fullScreen);
    if (fullScreen) {
      intent.putExtra("developer_logo", mDevLogoResId);
    }
    intent.putExtra("fetch_delay", getFetchDelay());
    Log.d(TAG, "getSplashActivityIntent: BiddingToken " + mS2sBiddingToken);
    if (!TextUtils.isEmpty(mS2sBiddingToken)) {
      intent.putExtra(Constants.TOKEN, mS2sBiddingToken);
    }
    return intent;
  }


  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    if (parent == mPosIdSpinner) {
      mPosIdArrayAdapter.setSelectedPos(position);
      mPosIdEdt.setText(getResources().getStringArray(R.array.splash_ad_value)[position]);
    } else if (parent == mDevLogoSpinner) {
      mDevLogoArrayAdapter.setSelectedPos(position);
      mDevLogoResId = DEV_LOGO_DATA[position].second;
    }
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {

  }


  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    Log.i("AD_DEMO", requestCode + "==" + resultCode);
  }
}
