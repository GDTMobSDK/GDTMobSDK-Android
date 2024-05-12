package com.qq.e.union.adapter.util;

import com.baidu.mobads.sdk.api.MobadsPermissionSettings;
import com.bytedance.sdk.openadsdk.TTAdConfig;
import com.bytedance.sdk.openadsdk.TTAdSdk;
import com.kwad.sdk.api.KsAdSDK;
import com.qq.e.comm.managers.setting.GlobalSetting;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * 控制各个ADN的个性化推荐广告的开关
 */
public class PersonalRecommendUtils {

  /**
   * 穿山甲开关状态
   * "" -> 不传或传空或传非01值无任何影响
   * 0  -> 屏蔽
   * 1  -> 不屏蔽
   */
  public static String sTTState = "";

  /**
   * 快手开挂状态
   * true  -> 不屏蔽
   * false -> 屏蔽
   * null  -> 无影响
   */
  public static Boolean sKSState = null;

  public static final String sTTKey = "personal_ads_type";

  /**
   * 各个ADN的统一开关
   * 注意：建议在 Application 初始化时且所有 adn 初始化前调用
   *
   * @param state 0 -> 不屏蔽，1 -> 屏蔽，其他 -> 无影响
   */
  public static void setState(int state) {
    // 优量汇
    setGDTState(state);
    // 百度
    setBDState(state);
    // 穿山甲
    setTTState(state);
    // 快手
    setKSState(state);
  }

  /**
   * 优量汇，个性化推荐广告开关，设置即生效
   *
   * @param state 0 -> 不屏蔽，1 -> 屏蔽，其他 -> 无影响
   */
  public static void setGDTState(int state) {
    GlobalSetting.setPersonalizedState(state);
  }

  /**
   * 百度，个性化推荐广告开关，设置即生效
   *
   * @param state 0 -> 不屏蔽，1 -> 屏蔽，其他 -> 无影响
   */
  public static void setBDState(int state) {
    // 获取限制个性化广告推荐状态
    // MobadsPermissionSettings.getLimitPersonalAdsStatus()

    // 隐私敏感权限API&限制个性化广告推荐建议在Application初始化时添加，true为不启用
    MobadsPermissionSettings.setLimitPersonalAds(state == 1);
  }

  /**
   * 快手，个性化推荐广告开关
   * 注意：必须在 ks sdk 初始化前调用
   *
   * @param state 0 -> 不屏蔽，1 -> 屏蔽，其他 -> 无影响
   */
  public static void setKSState(int state) {
    sKSState = null;
    if (state == 1) {
      sKSState = false;
    } else if (state == 0) {
      sKSState = true;
    }
  }

  /**
   * 快手，个性化推荐广告开关
   * 注意：必须在 ks sdk 初始化成功后调用，
   *
   * @param state 0 -> 不屏蔽，1 -> 屏蔽，其他 -> 无影响
   */
  public static void setKSStateAfterInit(int state) {
    setKSState(state);
    // 请求广告的时候设置,true为启用
    KsAdSDK.setPersonalRecommend(sKSState);
  }

  /**
   * 穿山甲，个性化推荐广告开关
   * 注意：必须在 tt sdk 初始化前调用，此方法会覆盖掉 TTAdConfig 中的 data
   *
   * @param state 0 -> 不屏蔽，1 -> 屏蔽，其他 -> 无影响
   */
  public static void setTTState(int state) {
    sTTState = "";
    if (state == 1) {
      sTTState = "0";
    } else if (state == 0) {
      sTTState = "1";
    }
  }

  /**
   * 穿山甲，个性化推荐广告开关
   * 注意：必须在 tt sdk 初始化成功后调用，此方法会覆盖掉 TTAdConfig 中的 data
   *
   * @param state 0 -> 不屏蔽，1 -> 屏蔽，其他 -> 无影响
   */
  public static void setTTStateAfterInit(int state) {
    setTTState(state);
    updateTTPersonalData();
  }

  private static void updateTTPersonalData() {
    TTAdConfig ttAdConfig = new TTAdConfig.Builder().data(getTTData()).build();
    TTAdSdk.updateAdConfig(ttAdConfig);
  }

  private static String getTTData() {
    try {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("name", sTTKey);
      jsonObject.put("value", sTTState);
      JSONArray jsonArray = new JSONArray();
      jsonArray.put(jsonObject);
      return jsonArray.toString();
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

}
