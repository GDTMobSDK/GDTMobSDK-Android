package com.qq.e.union.demo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.multidex.MultiDexApplication;
import android.util.Log;
import android.webkit.WebView;

import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.managers.setting.GlobalSetting;
import com.qq.e.union.adapter.bd.util.BDAdManager;
import com.qq.e.union.adapter.kuaishou.util.KSSDKInitUtil;
import com.qq.e.union.adapter.tt.util.TTAdManagerHolder;
import com.tencent.bugly.crashreport.CrashReport;

import java.util.HashMap;
import java.util.Map;

public class InnerDemoApplication extends MultiDexApplication {

  @Override
  public void onCreate() {
    super.onCreate();
    config(this);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      String processName = getProcessName(this);
      String packageName = this.getPackageName();
      if (!packageName.equals(processName)) {
        WebView.setDataDirectorySuffix(processName);
      }
    }
    DemoUtil.setAQueryImageUserAgent();
  }

  void config(Context context) {
    try {
      CrashReport.initCrashReport(this, Constants.BuglyAppID, true);
      // 通过调用此方法初始化 SDK。如果需要在多个进程拉取广告，每个进程都需要初始化 SDK。
      GDTAdSdk.init(context, Constants.APPID);
      GlobalSetting.setChannel(1);
      GlobalSetting.setEnableMediationTool(true);
      Map<String, String> maps = new HashMap<>();
      maps.put(GlobalSetting.BD_SDK_WRAPPER, BDAdManager.class.getName());
      maps.put(GlobalSetting.TT_SDK_WRAPPER, TTAdManagerHolder.class.getName());
      maps.put(GlobalSetting.KS_SDK_WRAPPER, KSSDKInitUtil.class.getName());
      GlobalSetting.setPreloadAdapters(maps);
      String packageName = context.getPackageName();
      //Get all activity classes in the AndroidManifest.xml
      PackageInfo packageInfo = context.getPackageManager().getPackageInfo(
              packageName, PackageManager.GET_ACTIVITIES | PackageManager.GET_META_DATA);
      if (packageInfo.activities != null) {
        for (ActivityInfo activity : packageInfo.activities) {
          Bundle metaData = activity.metaData;
          if (metaData != null && metaData.containsKey("id")
                  && metaData.containsKey("content") && metaData.containsKey("action")) {
            Log.e("gdt", activity.name);
            try {
              Class.forName(activity.name);
            } catch (ClassNotFoundException e) {
              continue;
            }
            String id = metaData.getString("id");
            String content = metaData.getString("content");
            String action = metaData.getString("action");
            DemoListActivity.register(action, id, content);
          }
        }
      }
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
  }

  private String getProcessName(Context context) {
    if (context == null) return null;
    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses()) {
      if (processInfo.pid == android.os.Process.myPid()) {
        return processInfo.processName;
      }
    }
    return null;
  }
}
