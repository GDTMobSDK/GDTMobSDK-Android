package com.qq.e.union.demo;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.WindowManager;

public class BaseActivity extends FragmentActivity {

  private static final String AUTO_LOAD_AND_SHOW = "autoLoadAndShow";

  protected boolean mIsLoadSuccess;
  protected boolean mIsLoadAndShow;
  protected String mBackupPosId;

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

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    if (mIsLoadAndShow) {
      loadAd();
    }
  }

}
