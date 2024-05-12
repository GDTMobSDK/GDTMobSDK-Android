package com.qq.e.union.adapter.tt.reward;

import android.content.Context;
import android.util.Log;
import com.bytedance.sdk.openadsdk.AdSlot;


/**
 * 穿山甲模板激励视频适配器
 * 作用：封装穿山甲模板激励视频，适配优量汇模板激励视频
 */

public class TTExpressRewardAdAdapter extends TTRewardAdAdapter {

  public TTExpressRewardAdAdapter(Context context, String appID, String posID, String ext) {
    super(context, appID, posID, ext);
  }

  @Override protected AdSlot.Builder setAdSlotParams(AdSlot.Builder builder) {
    // 模板广告需要设置期望个性化模板广告的大小,单位dp,激励视频场景,只要设置的值大于0即可,可通过构造函数的ext参数传入
    return super.setAdSlotParams(builder).setExpressViewAcceptedSize(500, 500);
  }

}
