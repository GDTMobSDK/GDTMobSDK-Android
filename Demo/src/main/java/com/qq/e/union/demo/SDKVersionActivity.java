package com.qq.e.union.demo;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.widget.TextView;

import com.qq.e.comm.managers.status.SDKStatus;

/**
 * 版本号展示 Activity
 */
public class SDKVersionActivity extends BaseActivity {
  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_sdk_version);
    ((TextView) findViewById(R.id.sdk_version_txt))
        .setText("SDk version is : " + SDKStatus.getIntegrationSDKVersion());
  }
}
