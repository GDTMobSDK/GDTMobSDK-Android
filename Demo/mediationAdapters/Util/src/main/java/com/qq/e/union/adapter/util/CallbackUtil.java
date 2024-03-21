package com.qq.e.union.adapter.util;

import java.util.HashMap;
import java.util.Map;

public class CallbackUtil {
  private static final Map<String, Boolean> sMETHOD_CACHE = new HashMap<>();

  public static boolean hasRenderSuccessCallback(Object o) {
    if (o == null) {
      return false;
    }
    return hasMethod(o.getClass(), "onRenderSuccess");
  }

  private static String getKey(Class clazz, String methodName, Class... args) {
    if (clazz == null) {
      return "";
    }
    StringBuilder stringBuilder = new StringBuilder();
    stringBuilder.append(clazz.getName()).append("#").append(methodName);
    for (Class arg : args) {
      stringBuilder.append("_").append(arg.getName());
    }
    return stringBuilder.toString();
  }

  private static boolean hasMethod(Class clazz, String methodName, Class... args) {
    String key = getKey(clazz, methodName, args);
    Boolean result = sMETHOD_CACHE.get(key);
    if (result == null) {
      try {
        clazz.getDeclaredMethod(methodName, args);
        sMETHOD_CACHE.put(key, Boolean.TRUE);
        return true;
      } catch (NoSuchMethodException e) {
        sMETHOD_CACHE.put(key, Boolean.FALSE);
        return false;
      }
    }
    return Boolean.TRUE.equals(result);
  }


}
