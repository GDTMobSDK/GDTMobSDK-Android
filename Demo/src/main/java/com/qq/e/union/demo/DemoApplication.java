package com.qq.e.union.demo;

import android.content.Context;

import com.qq.e.comm.managers.setting.GlobalSetting;
import com.qq.e.union.adapter.bd.util.BDAdManager;
import com.qq.e.union.adapter.kuaishou.util.KSSDKInitUtil;
import com.qq.e.union.adapter.tt.util.TTAdManagerHolder;

import java.util.HashMap;
import java.util.Map;

public class DemoApplication extends InnerDemoApplication {

  @Override
  protected void config(Context context) {
    Map<String, String> maps = new HashMap<>();
    maps.put(GlobalSetting.BD_SDK_WRAPPER, BDAdManager.class.getName());
    maps.put(GlobalSetting.TT_SDK_WRAPPER, TTAdManagerHolder.class.getName());
    maps.put(GlobalSetting.KS_SDK_WRAPPER, KSSDKInitUtil.class.getName());
    GlobalSetting.setPreloadAdapters(maps);
    super.config(context);
  }
}
