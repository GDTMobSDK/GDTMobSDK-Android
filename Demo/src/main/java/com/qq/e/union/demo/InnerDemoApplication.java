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
    initializeApp();
  }

  private void initializeApp() {
    configureDemo();
    configureWebView();
    configureGDTAdSDK();
    startGDTAdSDK();
    registerActivities();
  }

  private void configureDemo() {
    DemoUtil.setAQueryImageUserAgent();
    CrashReport.initCrashReport(this, Constants.BuglyAppID, true);
  }

  private void configureWebView() {
    // 如果 App 是多进程，建议设置 WebView.setDataDirectorySuffix 避免 WebView 多进程崩溃
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      String processName = Application.getProcessName();
      String packageName = this.getPackageName();
      if (!packageName.equals(processName)) {
        WebView.setDataDirectorySuffix(processName);
      }
    }
  }

  protected void configureGDTAdSDK() {
    // 建议在初始化 SDK 前进行此设置
    GlobalSetting.setChannel(1);
    GlobalSetting.setEnableMediationTool(true);
    GlobalSetting.setEnableCollectAppInstallStatus(true);
  }

  private void startGDTAdSDK() {
    // 开发者请注意，4.560.1430 版本后 GDTAdSdk.init 接口已废弃，请尽快迁移至 GDTAdSdk.initWithoutStart、GDTAdSdk.start
    // 调用 initWithoutStart 接口进行初始化，该接口不会采集用户信息
    GDTAdSdk.initWithoutStart(this, Constants.APPID);
    // 调用 initWithoutStart 后请尽快调用 start，否则可能影响广告填充，造成收入下降
    GDTAdSdk.start(new GDTAdSdk.OnStartListener() {
      @Override
      public void onStartSuccess() {
        // 推荐开发者在 onStartSuccess 回调后开始拉广告
      }

      @Override
      public void onStartFailed(Exception e) {
        Log.e("gdt onStartFailed:", e.toString());
      }
    });
  }

  private void registerActivities() {
    try {
      String packageName = this.getPackageName();
      //Get all activity classes in the AndroidManifest.xml
      PackageInfo packageInfo = this.getPackageManager().getPackageInfo(
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
            registerActivity(action, id, content);
          }
        }
      }
    } catch (PackageManager.NameNotFoundException e) {
      e.printStackTrace();
    }
  }

  protected void registerActivity(String action, String id, String content) {
    DemoListActivity.register(action, id, content);
  }

  public static Context getAppContext() {
    return appContext;
  }
}
