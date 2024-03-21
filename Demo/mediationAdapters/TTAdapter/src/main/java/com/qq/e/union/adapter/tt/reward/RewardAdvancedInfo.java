package com.qq.e.union.adapter.tt.reward;

public class RewardAdvancedInfo {
  /**
   * 最终发放的数量
   */
  public float mRewardAmount = 0;

  /**
   * 开发者自行配置此项
   * 是否使用平台上代码位上配置的Amount
   */
  public static final boolean isUsePlatformRewardAmount = false;

  /**
   * 开发者自行配置此项
   * 再看一个的奖励折算比率,当奖励是再看一个过来时,折算的比率
   * 比如正常计算出的奖励数量是10,但是为再看一个时触发的,
   * 若奖励折算比率配置为0.5,此次回调推荐给出的奖励就是 (10 * 0.5 = 5)
   */
  public static final float sPlayAgainPercent = 0.5f;

  /**
   * 解析并处理普通奖励与进阶奖励
   */
  public void proxyRewardModel(RewardBundleModel model, boolean isPlayAgain) {
    float nowAmount;
    if (isUsePlatformRewardAmount) {
      nowAmount = model.getRewardAmount() * model.getRewardPropose();
    } else {
      nowAmount = model.getRewardPropose();
    }
    if (isPlayAgain) {
      nowAmount *= sPlayAgainPercent;
    }
    mRewardAmount += nowAmount;
  }

  /**
   * 在onClose调用，获得最终应该发奖励的百分比/奖励数量
   * 如果未启了平台配置的奖励数量功能，返回的是 最终建议的奖励数量的百分比，比如1.2（120%）
   * 如果开启了平台配置的奖励数量功能，返回的是 最终建议的奖励数量（配置的奖励数量*推荐的奖励百分比）
   */
  public float getRewardAdvancedAmount() {
    return mRewardAmount;
  }

}
