package com.qq.e.union.demo;

import static android.content.res.Configuration.ORIENTATION_LANDSCAPE;
import static android.content.res.Configuration.ORIENTATION_PORTRAIT;

import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

import androidx.fragment.app.FragmentActivity;

public class BaseActivity extends FragmentActivity {

  public static final String AUTO_LOAD_AND_SHOW = "autoLoadAndShow";

  protected boolean mIsLoadSuccess;
  protected boolean mIsLoadAndShow;
  protected String mBackupPosId;
  protected String mS2sBiddingToken;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if(getIntent().getBooleanExtra(AUTO_LOAD_AND_SHOW, false)){
      mIsLoadAndShow = true;
      mBackupPosId = getIntent().getStringExtra(Constants.POS_ID);

      WindowManager wm =
          ((WindowManager) this.getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
      if (getIntent().getIntExtra("orientation", 1) != DemoUtil.getOrientation(wm.getDefaultDisplay().getRotation())) {
        setRequestedOrientation(getIntent().getIntExtra("orientation", 1));
      } else {
        loadAd();
      }
    }

  }

  protected void loadAd() {
  }

  public void requestS2SBiddingToken(View view) {
    BuildConfig.DemoRequestUtils.requestBiddingToken(getPosId(), token -> mS2sBiddingToken = token);
  }

  protected String getPosId(){
    return "";
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (mIsLoadAndShow) {
      loadAd();
    }
  }

  protected void changeScreenOrientation() {
    int currentOrientation = getResources().getConfiguration().orientation;
    if (currentOrientation == ORIENTATION_PORTRAIT) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
    } else if (currentOrientation == ORIENTATION_LANDSCAPE) {
      setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    }
  }

}
