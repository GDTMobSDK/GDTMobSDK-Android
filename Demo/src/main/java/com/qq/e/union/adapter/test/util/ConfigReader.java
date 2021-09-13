package com.qq.e.union.adapter.test.util;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

public class ConfigReader {
  private static final String TAG = ConfigReader.class.getSimpleName();

  public static String getConfig(Context context) {
    File dir = context.getDir("e_qq_com_mediation", Context.MODE_PRIVATE);
    if (!dir.exists()) {
      Log.e(TAG, "getConfig: e_qq_com_mediation is not directory");
      return null;
    }
    File configFile = new File(dir, "test");
    if (!configFile.exists()) {
      Log.e(TAG, "getConfig: file test is null");
      return null;
    }

    BufferedReader br = null;
    try {
      InputStreamReader reader = new InputStreamReader(new FileInputStream(configFile), "UTF-8");
      br = new BufferedReader(reader);
      String line = null;
      StringBuilder sb = new StringBuilder();
      while ((line = (br.readLine())) != null) {
        sb.append(line);
      }
      return sb.toString();
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (br != null) {
        try {
          br.close();
        } catch (Exception e1) {
          Log.e(TAG, "getConfig: Exception while close bufferreader");
        }
      }
    }
    return null;
  }
}
