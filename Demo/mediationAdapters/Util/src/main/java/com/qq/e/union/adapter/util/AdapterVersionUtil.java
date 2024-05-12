package com.qq.e.union.adapter.util;

import com.qq.e.comm.managers.status.SDKStatus;
import com.qq.e.union.util.BuildConfig;

public class AdapterVersionUtil {

    public static String getAdapterVersion() {
        return SDKStatus.getIntegrationSDKVersion() + "." + BuildConfig.ADAPTER_VERSION;
    }
}
