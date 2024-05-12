package com.qq.e.union.adapter.bd.util;

import android.content.Context;
import android.util.Log;

import com.baidu.mobads.sdk.api.BDAdConfig;
import com.baidu.mobads.sdk.api.MobadsPermissionSettings;


public class BDAdManager {

  private static volatile boolean inited;

  public static void init(Context context, String appId) {
    Log.d("BDAdManager", "init: context: " + context + ", appId: " + appId + ", inited: " + inited);
    if (inited) {
      return;
    }
    synchronized (BDAdManager.class) {
      if (!inited) {
        inited = true;
        BDAdConfig bdAdConfig = new BDAdConfig.Builder()
            // 1、设置app名称，可选
            .setAppName("网盟demo")
            // 2、应用在mssp平台申请到的appsid，和包名一一对应，此处设置等同于在AndroidManifest.xml里面设置
            .setAppsid(appId)
            // 注意，如果setHttps设置为true，会导致百度无法拉取到 banner 广告
            .setHttps(false)
            .build(context);
        bdAdConfig.init();

        // 设置SDK可以使用的权限，包含：设备信息、定位、存储、APP LIST
        // 注意：建议授权SDK读取设备信息，SDK会在应用获得系统权限后自行获取IMEI等设备信息
        // 授权SDK获取设备信息会有助于提升ECPM
        MobadsPermissionSettings.setPermissionReadDeviceID(true);
        MobadsPermissionSettings.setPermissionLocation(true);
        MobadsPermissionSettings.setPermissionStorage(true);
        MobadsPermissionSettings.setPermissionAppList(true);
      }
    }
  }
}
