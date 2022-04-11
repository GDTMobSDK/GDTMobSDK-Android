package com.qq.e.union.demo.view;

import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import com.qq.e.comm.managers.GDTAdSdk;
import com.qq.e.union.demo.util.ToastUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Server bidding 模拟请求服务端进行比价的工具类
 */
public class S2SBiddingDemoUtils {
  public static final ExecutorService SINGLE_THREAD_EXECUTOR =
      Executors.newSingleThreadExecutor(r -> new Thread(r, "BIDDING_THREAD"));
  private static final String TAG = S2SBiddingDemoUtils.class.getSimpleName();
  private static final String SERVER_BIDDING_URL = "https://mi.gdt.qq.com/server_bidding";
  private static final String POST_DATA = "{\"id\":\"5f0417f6354b680001e94518\",\"imp\":[{\"id\":\"1\"," +
      "\"video\":{\"minduration\":0,\"maxduration\":46,\"w\":720,\"h\":1422,\"linearity\":1,\"minbitrate\":250," +
      "\"maxbitrate\":15000,\"ext\":{\"skip\":0,\"videotype\":\"rewarded\",\"rewarded\":1}},\"tagid\":\"POSID\"," +
      "\"bidfloor\":1,\"bidfloorcur\":\"CNY\",\"secure\":1}],\"app\":{\"id\":\"5afa947e9c8119360fba1bea\"," +
      "\"name\":\"VungleApp123\",\"bundle\":\"com.qq.e.union.demo.union\"},\"device\":{\"ua\":\"Mozilla/5.0 (Linux; " +
      "Android 9; SM-A207F Build/PPR1.180610.011; wv) AppleWebKit/537.36 KHTML, like Gecko) Version/4.0 Chrome/74.0" +
      ".3729.136 Mobile Safari/537.36\",\"geo\":{\"lat\":-7.2484,\"lon\":112.7419},\"ip\":\"115.178.227.128\"," +
      "\"devicetype\":1,\"make\":\"samsung\",\"model\":\"SM-A207F\",\"os\":\"android\",\"osv\":\"9\",\"h\":1422," +
      "\"w\":720,\"language\":\"en\",\"connectiontype\":2,\"ifa\":\"dd94e183d8790d057fc73d9c761ea2fa\"," +
      "\"ext\":{\"oaid" +
      "\":\"0176863C3B9A5E419BCAF702B37BEFB38B8D05CEA84022FB76BD723BA89D2ED2116F960A73FE1FFB12499E31EF664F5EAE87386F19D8A41390FEBAA5362042BC7A601D4CB006DA4E66\"}},\"cur\":[\"CNY\"],\"ext\":{\"buyer_id\":\"TOKEN\",\"sdk_info\":\"SDK_INFO\"}}";

  private static Handler sHandler = new Handler(Looper.getMainLooper());

  public static void requestBiddingToken(String posId, RequestTokenCallBack callBack) {
    Map<String, Object> map = new HashMap<>();
    map.put("staIn", "abcdefg"); // 开发者自定义参数，默认不传
    String buyerId = GDTAdSdk.getGDTAdManger().getBuyerId(map);
    String sdkInfo = GDTAdSdk.getGDTAdManger().getSDKInfo(posId);
    Log.d(TAG, "sdk_info: " + sdkInfo);
    SINGLE_THREAD_EXECUTOR.execute(() -> {
      try {
        HttpURLConnection connection =
            (HttpURLConnection) new URL(SERVER_BIDDING_URL).openConnection();
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("User-Agent", "GDTMobApp/0 CFNetwork/1220.1 Darwin/19.6.0");
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("Accept-Language", "en-us");
        connection.setRequestProperty("X-OpenRTB-Version", "2.5");
        String postData = POST_DATA.replace("POSID", posId)
            .replace("TOKEN", buyerId)
            .replace("SDK_INFO", sdkInfo);
        Log.d(TAG, "post_data: " + postData);
        byte[] postDataBytes = postData.getBytes(Charset.forName("UTF-8"));
        if (postDataBytes != null && postDataBytes.length > 0) {
          OutputStream out = new BufferedOutputStream(connection.getOutputStream());
          out.write(postDataBytes);
          out.flush();
          out.close();
        }
        int responseCode = connection.getResponseCode();
        if (HttpURLConnection.HTTP_OK == responseCode) {
          String response = getStringContent(connection);
          JSONObject jsonObject = new JSONObject(response);
          String token = jsonObject.optString("token");
          if (TextUtils.isEmpty(token)) {
            ToastUtil.s("回包中无 token");
          } else {
            ToastUtil.s("请求 token 成功");
            if (callBack != null) {
              Log.d(TAG, "requestBiddingToken: " + callBack);
              callBack.onSuccess(token);
            }
          }
        } else {
          ToastUtil.s("请求 token 失败： " + responseCode);
          Log.e(TAG,
              "requestBiddingToken: responseCode: " + responseCode + ", msg:" + connection.getResponseMessage());
        }
      } catch (IOException e) {
        ToastUtil.s("请求 token 失败： " + e.getMessage());
        e.printStackTrace();
      } catch (JSONException e) {
        ToastUtil.s("请求 token 失败： " + e.getMessage());
        e.printStackTrace();
      }
    });
  }

  public static byte[] getBytesContent(HttpURLConnection connection) throws IllegalStateException
      , IOException {
    InputStream in = connection.getInputStream();
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    try {
      byte[] buffer = new byte[1024];
      int len = 0;
      while ((len = (in.read(buffer))) > 0) {
        bo.write(buffer, 0, len);
      }
    } finally {
      try {
        if (in != null) {
          in.close();
        }
      } catch (IOException ignore) {
      }
    }
    return bo.toByteArray();
  }

  public static String getStringContent(HttpURLConnection connection) throws IOException {
    byte[] bytes = getBytesContent(connection);
    if (bytes == null) {
      return null;
    } else if (bytes.length == 0) {
      return "";
    }

    String charset = null;
    try {
      charset = connection.getContentEncoding();
    } catch (Exception e) {
      e.printStackTrace();
    }
    if (charset == null) {
      charset = "UTF-8";
    }
    return new String(bytes, charset);
  }

  public interface RequestTokenCallBack {
    void onSuccess(String token);
  }

}
