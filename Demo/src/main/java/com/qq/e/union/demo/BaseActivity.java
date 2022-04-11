package com.qq.e.union.demo;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class BaseActivity extends FragmentActivity {

  private static final String AUTO_LOAD_AND_SHOW = "autoLoadAndShow";

  protected boolean mIsLoadSuccess;
  protected boolean mIsLoadAndShow;
  protected String mBackupPosId;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    if (getIntent().getBooleanExtra(AUTO_LOAD_AND_SHOW, false)) {
      mIsLoadAndShow = true;
      mBackupPosId = getIntent().getStringExtra(Constants.POS_ID);
      loadAd();
    }

  }

  protected void loadAd() {
  }


}
