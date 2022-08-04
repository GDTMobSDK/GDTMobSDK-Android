package com.qq.e.union.demo;

import com.qq.e.comm.pi.IBidding;
import com.qq.e.comm.constants.BiddingLossReason;

import java.util.HashMap;

public class DemoBiddingC2SUtils {

  private static int reportBiddingWinLoss = -1;

  public static final int REPORT_BIDDING_DISABLE = -1;
  public static final int REPORT_BIDDING_WIN = 0;
  public static final int REPORT_BIDDING_LOSS_LOW_PRICE = BiddingLossReason.LOW_PRICE;
  public static final int REPORT_BIDDING_LOSS_TIME_OUT = BiddingLossReason.TIME_OUT;
  public static final int REPORT_BIDDING_LOSS_NO_AD = BiddingLossReason.NO_AD;
  public static final int REPORT_BIDDING_LOSS_AD_DATA_ERROR = BiddingLossReason.AD_DATA_ERROR;
  public static final int REPORT_BIDDING_LOSS_OTHER = BiddingLossReason.OTHER;

  public static void setReportBiddingWinLoss(int reportBiddingWinLoss) {
    DemoBiddingC2SUtils.reportBiddingWinLoss = reportBiddingWinLoss;
  }

  public static void reportBiddingWinLoss(IBidding ad) {
    switch (reportBiddingWinLoss) {
      case REPORT_BIDDING_WIN:
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put(IBidding.EXPECT_COST_PRICE, 200);
        hashMap.put(IBidding.HIGHEST_LOSS_PRICE, 199);
        ad.sendWinNotification(hashMap);
        break;
      case REPORT_BIDDING_LOSS_LOW_PRICE:
      case REPORT_BIDDING_LOSS_TIME_OUT:
      case REPORT_BIDDING_LOSS_NO_AD:
      case REPORT_BIDDING_LOSS_AD_DATA_ERROR:
      case REPORT_BIDDING_LOSS_OTHER: {
        HashMap<String, Object> hashMapLoss = new HashMap<>();
        hashMapLoss.put(IBidding.WIN_PRICE, 300);
        hashMapLoss.put(IBidding.LOSS_REASON, reportBiddingWinLoss);
        hashMapLoss.put(IBidding.ADN_ID, "WinAdnID");
        ad.sendLossNotification(hashMapLoss);
        break;
      }
    }
  }
}
