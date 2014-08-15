package com.uc.webkit.helper;

import java.lang.reflect.Method;

import android.app.Application;

public final class ActivityThreadHelper {
    private static Class<?> activityThreadClass;
    
    private static Method methodCurrentApplication = null;
    
    static {
        try {
            activityThreadClass = Class.forName("android.app.ActivityThread");
            methodCurrentApplication = activityThreadClass.getMethod("currentApplication");
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public static Application currentApplication() {
        if (activityThreadClass != null && methodCurrentApplication != null) {
            try {
                Object app = methodCurrentApplication.invoke(null);
                return (Application) app;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return null;
    }    
}
