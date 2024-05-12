package com.qq.e.union.adapter.test.model;

import android.text.TextUtils;

import org.json.JSONObject;

public class NetworkConfig {
  /**
   * name : SELF_SELLING // 三方渠道名称
   * posId : 1041 // 三方渠道posId
   * class_name : class_name_placeholder //类名
   * ext : {"x1": "a param"} // App 运营配置额外信息
   */

  private String name;
  private String identity;
  private String className;
  private String ext;
  private String adapterStatus;

  public NetworkConfig(JSONObject config) {
    name = config.optString("name");
    identity = config.optString("posId");
    className = config.optString("className");
    ext = config.optString("ext");
    adapterStatus = "不可用";
    if ("adshonor".equals(name) || "TSSP".equals(name) || "GDT".equals(name)) {
      adapterStatus = "内部渠道，忽略";
    } else if (!TextUtils.isEmpty(className)) {
      try {
        Class.forName(className);
        adapterStatus = "正常";
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
    }
  }

  public String getName() {
    return name;
  }

  public String getIdentity() {
    return identity;
  }

  public String getClassName() {
    return className;
  }

  public String getExt() {
    return ext;
  }

  public String getAdapterStatus() {
    return adapterStatus;
  }
}