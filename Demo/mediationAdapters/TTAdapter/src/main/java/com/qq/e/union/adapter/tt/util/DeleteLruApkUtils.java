package com.qq.e.union.adapter.tt.util;


import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 删除 "/sdcard/ByteDownload/" 和 "/sdcard/Android/data/{package_name}/files/Download/pangle_com.byted.pangle" 目录下文件
 */
public class DeleteLruApkUtils {

  private static final String TAG = "DeleteLruApkUtils";
  private static final String TT_APK_PRIVACY_EXTERNAL_DOWNLOAD_DIR = "pangle_com.byted.pangle";
  private static final String TT_APK_EXTERNAL_FILE_DOWNLOAD_DIR = "ByteDownload";
  private static final long TIME_1_DAY = 24 * 60 * 60 * 1000;
  private static final int ANDROID_Q = 29;
  private static final AtomicBoolean sIsInProcess = new AtomicBoolean(false);
  private static long mTimeInterval = 7 * TIME_1_DAY;

  public static void deleteApkFile(final Context context, int timeIntervalInDays) {
    if (sIsInProcess.getAndSet(true)) {
      Log.d(TAG, "deleteApkFile: is deleting file");
      return;
    }
    if (timeIntervalInDays > 0) {
      mTimeInterval = timeIntervalInDays * TIME_1_DAY;
    }
    deleteApkFileAsync(context);
  }

  private static void deleteApkFileAsync(Context context) {
    new Thread(new Runnable() {
      @Override
      public void run() {
        String externalFilePath = getExternalFilesDir(context);
        File externalFile = getGlobalSaveDir(externalFilePath);
        String privacyExternalFilePath = getPrivacyExternalFile(context);
        File privacyExternalFile = getGlobalSaveDir(privacyExternalFilePath);
        try {
          deleteSubFileInDirectory(externalFile);
          deleteSubFileInDirectory(privacyExternalFile);
        } catch (Exception e) {
          Log.w(TAG, "deleteApkFileAsync: ", e);
        }
        sIsInProcess.set(false);
      }
    }).start();
  }

  private static String getExternalFilesDir(Context context) {
    try {
      int targetSdkVersion = context.getApplicationInfo().targetSdkVersion;
      if (Build.VERSION.SDK_INT < ANDROID_Q || targetSdkVersion <= ANDROID_Q) {
        return Environment.getExternalStorageDirectory().getPath() + File.separator +
               TT_APK_EXTERNAL_FILE_DOWNLOAD_DIR;
      }
      return null;
    } catch (Throwable th) {
      return null;
    }
  }

  private static String getPrivacyExternalFile(Context context) {
    return context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath() +
           File.separator + TT_APK_PRIVACY_EXTERNAL_DOWNLOAD_DIR;
  }

  /**
   * 这里如果没有返回则不 mkdir
   */
  private static File getGlobalSaveDir(String str) {
    if (TextUtils.isEmpty(str)) {
      return null;
    }
    try {
      File file = new File(str);
      if (!file.exists()) {
        return null;
      } else if (file.isDirectory()) {
        return file;
      } else {
        return null;
      }
    } catch (Throwable throwable) {
      return null;
    }
  }

  private static void deleteSubFileInDirectory(File rootFile) {
    if (rootFile == null || !rootFile.exists()) {
      return;
    }
    if (!rootFile.isDirectory()) {
      return;
    }
    File[] files = rootFile.listFiles();
    if (files == null) {
      return;
    }
    for (File file : files) {
      deleteFileAndSubFile(file);
    }
  }

  private static void deleteFileAndSubFile(File file) {
    long current = System.currentTimeMillis();
    Log.d(TAG,
        "deleteFileAndSubFile: condition = " + file + ", currentTime = " + current + ", fileTime = " +
        file.lastModified() + ", duration: " + (current - file.lastModified()));
    if (current - file.lastModified() > mTimeInterval) {
      Log.d(TAG, "deleteFileAndSubFile: deleted file: " + file);
      deleteFileAndDirectory(file);
    }
  }

  private static void deleteFileAndDirectory(File file) {
    if (file.isDirectory()) {
      File[] files = file.listFiles();
      if (files != null && files.length > 0) {
        for(File subFile : files) {
          deleteFileAndDirectory(subFile);
        }
      }
    }
    boolean isDeleted = file.delete();
    Log.d(TAG, "deleteFileAndDirectory: isDeleted " + isDeleted);
  }

}
