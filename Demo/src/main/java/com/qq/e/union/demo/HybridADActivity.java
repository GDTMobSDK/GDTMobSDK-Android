package com.qq.e.union.demo;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.qq.e.ads.hybrid.HybridAD;
import com.qq.e.ads.hybrid.HybridADListener;
import com.qq.e.ads.hybrid.HybridADSetting;
import com.qq.e.comm.util.AdError;

import java.util.Locale;

/**
 * Created by chaotao on 2018/12/24 .
 **/
public class HybridADActivity extends Activity implements View.OnClickListener, HybridADListener {

  private static final String TAG = HybridADActivity.class.getSimpleName();

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_hybrid);
    findViewById(R.id.go).setOnClickListener(this);
  }

  @Override
  public void onClick(View v) {
    switch (v.getId()) {
      case R.id.go:
        String url = ((EditText) findViewById(R.id.url)).getText().toString();
        if (!TextUtils.isEmpty(url)) {
          HybridADSetting setting = fillHybridADSetting();
          if (setting != null) {
            HybridAD hybridAD = new HybridAD(this, setting, this);
            hybridAD.loadUrl(url);
          }
        }
        break;
    }
  }

  private HybridADSetting fillHybridADSetting() {
    HybridADSetting setting = new HybridADSetting().type(HybridADSetting.TYPE_REWARD_VIDEO);
    if (((CheckBox) findViewById(R.id.cb_titleBarHeight)).isChecked()) {
      String s = ((EditText) findViewById(R.id.et_titleBarHeight)).getText().toString();
      try {
        setting.titleBarHeight(Integer.valueOf(s));
      } catch (NumberFormatException e) {
        Toast.makeText(this, "导航栏高度输入不合法", Toast.LENGTH_LONG).show();
        return null;
      }
    }
    if (((CheckBox) findViewById(R.id.cb_titleBarColor)).isChecked()) {
      String s = ((EditText) findViewById(R.id.et_titleBarColor)).getText().toString();
      try {
        setting.titleBarColor(Long.valueOf(s, 16).intValue());
      } catch (NumberFormatException e) {
        Toast.makeText(this, "导航栏颜色输入不合法", Toast.LENGTH_LONG).show();
        return null;
      }
    }
    if (((CheckBox) findViewById(R.id.cb_title)).isChecked()) {
      String s = ((EditText) findViewById(R.id.et_title)).getText().toString();
      if (!TextUtils.isEmpty(s)) {
        setting.title(s);
      } else {
        Toast.makeText(this, "导航栏title输入为空", Toast.LENGTH_LONG).show();
        return null;
      }
    }
    if (((CheckBox) findViewById(R.id.cb_titleColor)).isChecked()) {
      String s = ((EditText) findViewById(R.id.et_titleColor)).getText().toString();
      try {
        setting.titleColor(Long.valueOf(s, 16).intValue());
      } catch (NumberFormatException e) {
        Toast.makeText(this, "导航栏title字体颜色输入不合法", Toast.LENGTH_LONG).show();
        return null;
      }
    }
    if (((CheckBox) findViewById(R.id.cb_titleSize)).isChecked()) {
      String s = ((EditText) findViewById(R.id.et_titleSize)).getText().toString();
      try {
        setting.titleSize(Integer.valueOf(s));
      } catch (NumberFormatException e) {
        Toast.makeText(this, "导航栏title字体大小输入不合法", Toast.LENGTH_LONG).show();
        return null;
      }
    }
    if (((CheckBox) findViewById(R.id.cb_back)).isChecked()) {
      String s = ((EditText) findViewById(R.id.et_back)).getText().toString();
      if (!TextUtils.isEmpty(s)) {
        setting.backButtonImage(s);
      } else {
        Toast.makeText(this, "导航栏back键icon资源名输入为空", Toast.LENGTH_LONG).show();
        return null;
      }
    }
    if (((CheckBox) findViewById(R.id.cb_close)).isChecked()) {
      String s = ((EditText) findViewById(R.id.et_close)).getText().toString();
      if (!TextUtils.isEmpty(s)) {
        setting.closeButtonImage(s);
      } else {
        Toast.makeText(this, "导航栏close键icon资源名输入为空", Toast.LENGTH_LONG).show();
        return null;
      }
    }
    if (((CheckBox) findViewById(R.id.cb_seperator)).isChecked()) {
      String s = ((EditText) findViewById(R.id.et_seperator)).getText().toString();
      try {
        setting.separatorColor(Long.valueOf(s, 16).intValue());
      } catch (NumberFormatException e) {
        Toast.makeText(this, "分割线色值输入不合法", Toast.LENGTH_LONG).show();
        return null;
      }
    }
    if (((CheckBox) findViewById(R.id.cb_back_seperator)).isChecked()) {
      String s = ((EditText) findViewById(R.id.et_back_seperator)).getText().toString();
      try {
        setting.backSeparatorLength(Integer.valueOf(s));
      } catch (NumberFormatException e) {
        Toast.makeText(this, "back键分割线高度输入不合法", Toast.LENGTH_LONG).show();
        return null;
      }
    }
    return setting;
  }

  @Override
  public void onPageShow() {
    Log.i(TAG, "onPageShow");
  }

  @Override
  public void onLoadFinished() {
    Log.i(TAG, "onLoadFinished");
  }

  @Override
  public void onClose() {
    Log.i(TAG, "onClose");
  }

  @Override
  public void onError(AdError error) {
    String msg = String.format(Locale.getDefault(), "onError, error code: %d, error msg: %s",
        error.getErrorCode(), error.getErrorMsg());
    Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
  }
}
