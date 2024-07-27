package com.qq.e.union.demo;

import android.app.Application;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.webkit.WebView;

import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.comm.managers.setting.GlobalSetting;
import com.tencent.bugly.crashreport.CrashReport;

import androidx.multidex.MultiDexApplication;

public class InnerDemoApplication extends MultiDexApplication {

  protected static Application appContext;

  @Override
  public void onCreate() {
    super.onCreate();
    appContext = this;
    config(this);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      String processName = Application.getProcessName();
      String packageName = this.getPackageName();
      if (!packageName.equals(processName)) {
        WebView.setDataDirectorySuffix(processName);
      }
    }
    DemoUtil.setAQueryImageUserAgent();
  }

  protected void config(Context context) {
    try {
      CrashReport.initCrashReport(this, Constants.BuglyAppID, true);
      // 建议在初始化 SDK 前进行此设置
      GlobalSetting.setEnableCollectAppInstallStatus(true);
      // 开发者请注意，4.560.1430版本后GDTAdSdk.init接口已废弃，请尽快迁移至GDTAdSdk.initWithoutStart、GDTAdSdk.start
      // GDTAdSdk.init(context, Constants.APPID);
      GDTAdSdk.initWithoutStart(context, Constants.APPID); // 调用此接口进行初始化，该接口不会采集用户信息
      // 调用initWithoutStart后请尽快调用start，否则可能影响广告填充，造成收入下降
      GDTAdSdk.start(new GDTAdSdk.OnStartListener() {
        @Override
        public void onStartSuccess() {
          // 推荐开发者在onStartSuccess回调后开始拉广告
        }

        @Override
        public void onStartFailed(Exception e) {
          Log.e("gdt onStartFailed:", e.toString());
        }
      });
      GlobalSetting.setChannel(1);
      GlobalSetting.setEnableMediationTool(true);
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

  public static Context getAppContext() {
    return appContext;
  }
}
