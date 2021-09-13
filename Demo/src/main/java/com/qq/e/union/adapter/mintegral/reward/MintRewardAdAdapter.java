package com.qq.e.union.adapter.mintegral.reward;

import android.content.Context;
import android.os.SystemClock;
import android.text.format.DateUtils;
import android.util.Log;


import com.mintegral.msdk.MIntegralConstans;
import com.mintegral.msdk.mtgbid.out.BidListennning;
import com.mintegral.msdk.mtgbid.out.BidManager;
import com.mintegral.msdk.mtgbid.out.BidResponsed;
import com.mintegral.msdk.out.MTGBidRewardVideoHandler;
import com.mintegral.msdk.out.RewardVideoListener;
import com.qq.e.ads.rewardvideo.ServerSideVerificationOptions;
import com.qq.e.comm.adevent.ADEvent;
import com.qq.e.comm.adevent.ADListener;
import com.qq.e.mediation.interfaces.BaseRewardAd;
import com.qq.e.union.adapter.mintegral.util.MTGSDKInitUtil;
import com.qq.e.union.adapter.util.Constant;
import com.qq.e.union.adapter.util.ErrorCode;

/**
 * 使用概述:
 *        1.初始化SDK
 *          所需参数:appId,appKey
 *        2.创建BidManager对象并设置监听,在回调方法onSuccessed中获取BidToken
 *          所需参数:posID(具体的广告位id)
 *        3.创建请求加载广告的MTGBidRewardVideoHandler对象,并设置监听。通过loadFromBid(token)发起请求
 *          所需参数:token(步骤二中获取)
 *        4.根据mMTGRewardVideoHandler.setRewardVideoListener中回调,作出相应的逻辑处理。
 *
 * 所需依赖:
 *         // Mintegral SDK
 *        implementation 'com.mintegral.msdk:videojs:10.1.31'
 *        implementation 'com.mintegral.msdk:mtgjscommon:10.1.31'
 *        implementation 'com.mintegral.msdk:playercommon:10.1.31'
 *        implementation 'com.mintegral.msdk:reward:10.1.31'
 *        implementation 'com.mintegral.msdk:videocommon:10.1.31'
 *        implementation 'com.mintegral.msdk:optimizedata:10.1.31'
 *        implementation 'com.mintegral.msdk:common:10.1.31'
 *        // 开发者后台创建App勾选APK为YES则加上mtgdownloads依赖
 *        implementation 'com.mintegral.msdk:mtgdownloads:10.1.31'
 *        //mtgbid
 *        implementation 'com.mintegral.msdk:mtgbid:10.1.31'
 *
 *        注:国内应用开发者在mintegral后台配置应用设置时,请在是否投放apk广告选择:是,否则可能获取不到广告资源。
 *
 *  manifest配置:
 *        mtg所需要的权限
 *        <uses-permission android:name="android.permission.INTERNET" />
 *        <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
 *        如果国内流量版本SDK ，以下两条权限必须加上
 *        <uses-permission android:name="android.permission.READ_PHONE_STATE" />
 *        <uses-permission android:name="android.permission.REQUEST_INSTALL_PACKAGES" />
 *
 *         MTG激励视频所需的Activity
 *         <activity
 *             android:name="com.mintegral.msdk.reward.player.MTGRewardVideoActivity"
 *             android:configChanges="orientation|keyboardHidden|screenSize"
 *             android:theme="@android:style/Theme.NoTitleBar.Fullscreen" />
 *         <activity
 *             android:name="com.mintegral.msdk.activity.MTGCommonActivity"
 *             android:configChanges="keyboard|orientation"
 *             android:exported="true"
 *             android:screenOrientation="portrait"
 *             android:theme="@android:style/Theme.Translucent.NoTitleBar"></activity>
 *         国内流量版本必需，海外版本可以不添加
 *         <receiver android:name="com.mintegral.msdk.click.AppReceiver">
 *             <intent-filter>
 *                 <action android:name="android.intent.action.PACKAGE_ADDED" />
 *                 <data android:scheme="package" />
 *             </intent-filter>
 *         </receiver>
 *         国内流量必需，海外版本可以不添加
 *         <service android:name="com.mintegral.msdk.shell.MTGService">
 *             <intent-filter>
 *                 <action android:name="com.mintegral.msdk.download.action" />
 *             </intent-filter>
 *         </service>
 *         国内流量版本必需，海外版本可以不添加。
 *         <provider
 *             android:name="com.mintegral.msdk.base.utils.MTGFileProvider"
 *             android:authorities="${applicationId}.mtgFileProvider"
 *             android:exported="false"
 *             android:grantUriPermissions="true">
 *             <meta-data
 *                 android:name="android.support.FILE_PROVIDER_PATHS"
 *                 android:resource="@xml/mtg_provider_paths" />
 *         </provider>
 *
 *  XML:(如果targetSDKVersion >= 24，需要适配FileProvider。 国内流量版本必需，海外版本可以不添加)
 *          在xml文件下添加mtg_provider_paths.xml
 *          <?xml version="1.0" encoding="utf-8"?>
 *          <paths xmlns:android="http://schemas.android.com/apk/res/android">
 *          <external-path name="external_files" path="."/>
 *          </paths>
 *
 *  混淆配置:
 *          -keepattributes Signature
 *          -keepattributes *Annotation*
 *          -keep class com.mintegral.** {*; }
 *          -keep interface com.mintegral.** {*; }
 *          -keep class android.support.v4.** { *; }
 *          -dontwarn com.mintegral.**
 *          -keep class **.R$* { public static final int mintegral*; }
 *          -keep class com.alphab.** {*; }
 *          -keep interface com.alphab.** {*; }
 *
 */

public class MintRewardAdAdapter extends BaseRewardAd {
    private String mPosId;
    private Context mContext;
    private ADListener mListener;
    private MTGBidRewardVideoHandler mMTGRewardVideoHandler;
    private BidResponsed mBidResponsed;
    private boolean mIsShown;//广告是否展示
    private long mExpireTime;//广告失效时间
    private boolean mIsVolumeOn;//是否静音播放 true:非静音 false:静音播放
    private static final String KEY_APPID = "appId";
    private static final String KEY_APP = "appkey";
    private static final String TAG = MintRewardAdAdapter.class.getSimpleName();

    public MintRewardAdAdapter(Context context, String appId, String posID, String ext) {
        super(context, appId, posID, ext);
        this.mPosId = posID;
        this.mContext = context;
        //step 1 : 初始化SDK
        MTGSDKInitUtil.initSDK(context, appId, ext);
    }

    @Override
    public void setAdListener(ADListener listener) {
        this.mListener = listener;
    }

    @Override
    public void loadAD() {
        beginBidRequest();
    }

    /**
     * step 2:创建BidManager对象,在回调方法onSuccessed中获取BidToken
     */
    private void beginBidRequest() {
        // step 2:创建BidManager
        BidManager manager = new BidManager(mPosId);
        manager.setBidListener(new BidListennning() {
            @Override
            public void onFailed(String msg) {
                // bid failed
                onAdError(ErrorCode.NO_AD_FILL);
            }

            @Override
            public void onSuccessed(BidResponsed bidResponsed) {
                // bid successeful
                mBidResponsed = bidResponsed;
                String token = mBidResponsed.getBidToken();
                // 通过token请求RewardVideo
                requestRewardVideoAd(token);
                Log.d(TAG, "beginBidRequest onSuccessed: ");
            }
        });
        manager.bid();
    }


    @Override
    public void showAD() {
        showRewardVideoAd();
        mIsShown = true;
    }


    @Override
    public long getExpireTimestamp() {
        return mExpireTime;
    }

    @Override
    public boolean hasShown() {
        return mIsShown;
    }

    @Override
    public int getECPM() {
        // mBidResponsed.getPrice() 单位:元
        // return 单位:分
        int ecpm = (int) ((Float.valueOf(mBidResponsed.getPrice())) * 100);
        Log.i(TAG,"ecpm: "+ecpm+" currency: "+mBidResponsed.getCur());
        return mBidResponsed != null ? ecpm : Constant.VALUE_NO_ECPM;
    }

    @Override
    public String getECPMLevel() {
        return null;
    }

  @Override
    public void setVolumeOn(boolean volumOn) {
    this.mIsVolumeOn = volumOn;
  }

    @Override
    public int getVideoDuration() {
        // 暂不支持
        return 0;
    }

    @Override
    public void setServerSideVerificationOptions(ServerSideVerificationOptions options) {
        // 暂不支持
    }

    /**
     * 显示 Mintegral RewardVideo
     */
    private void showRewardVideoAd() {
        // 先判断是否加载成功
        if (mMTGRewardVideoHandler != null && mMTGRewardVideoHandler.isBidReady()) {
            // rewardId 直接用默认的"1"
            mMTGRewardVideoHandler.showFromBid("1");
            // 在 MTG 竞价胜出，且开发者调用曝光时上报WinNotice
            mBidResponsed.sendWinNotice(mContext);
        } else {
            Log.i(TAG, "showRewardVideoAd: 暂无可用激励视频广告，请等待缓存加载或者重新刷新");
        }
    }

    /**
     * 请求BidRewardVideo
     *
     * @param token
     */
    private void requestRewardVideoAd(String token) {
        //step4:创建MTGBidRewardVideoHandler对象
        mMTGRewardVideoHandler = new MTGBidRewardVideoHandler(mContext, mPosId);
        //是否静音播放
        mMTGRewardVideoHandler.playVideoMute(mIsVolumeOn ? MIntegralConstans.REWARD_VIDEO_PLAY_NOT_MUTE : MIntegralConstans.REWARD_VIDEO_PLAY_MUTE);
        //step4:设置setRewardVideoListener
        mMTGRewardVideoHandler.setRewardVideoListener(new RewardVideoListener() {

            @Override
            public void onLoadSuccess(String unitId) {
                Log.i(TAG, "onLoadSuccess");
                if (mListener != null) {
                    mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_LOADED));
                }
            }

            @Override
            public void onVideoLoadSuccess(String unitId) {
                Log.i(TAG, "onVideoLoadSuccess");
                mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_VIDEO_CACHED));
                mExpireTime = SystemClock.elapsedRealtime() + 30 * DateUtils.MINUTE_IN_MILLIS;
            }

            @Override
            public void onVideoLoadFail(String errorMsg) {
                Log.e(TAG, "onVideoLoadFail");
                onAdError(ErrorCode.VIDEO_PLAY_ERROR);
            }

            @Override
            public void onShowFail(String errorMsg) {
                Log.e(TAG, "onShowFail");
                onAdError(ErrorCode.VIDEO_PLAY_ERROR);
            }

            @Override
            public void onAdShow() {
                Log.i(TAG, "onAdShow");
                mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_SHOW));
                // 由于MTG没有曝光回调，所以曝光和 show 一块回调
                mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_EXPOSE));
            }

            @Override
            public void onAdClose(boolean isCompleteView, String RewardName, float RewardAmout) {
                Log.i(TAG, "onAdClose rewardinfo :" + "RewardName:" + RewardName + "RewardAmout:" + RewardAmout + " isCompleteView：" + isCompleteView);
                if (isCompleteView) {
                    //激励视频播放完成,给予奖励操作  RewardName和RewardAmout由服务器返回
                    if (mListener != null) {
                        mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_REWARD));
                        mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_CLOSE));
                    }
                } else {
                    //不符合奖励条件
                    if (mListener != null) {
                        mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_CLOSE));
                    }
                }
            }

            @Override
            public void onVideoAdClicked(String unitId) {
                Log.i(TAG, "onVideoAdClicked");
                mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_AD_CLICK));
            }

            @Override
            public void onVideoComplete(String unitId) {
                Log.i(TAG, "onVideoComplete");
                mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_VIDEO_COMPLETE));
            }

            @Override
            //视频播放结束后,出现结束页的时候调用
            public void onEndcardShow(String unitId) {
                Log.i(TAG, "onEndcardShow");
            }

        });
        //step4:加载激励视频
        mMTGRewardVideoHandler.loadFromBid(token);
    }

    /**
     * @param errorCode 错误码
     */
    private void onAdError(int errorCode) {
        if (mListener != null) {
            mListener.onADEvent(new ADEvent(EVENT_TYPE_ON_ERROR, new Object[]{errorCode}));
        }
    }
}
