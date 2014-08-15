/****************************************************************************
 **
 ** Copyright (C) 2010-2012 UC Mobile Ltd. All Rights Reserved
 ** File        : ViewHelper.java
 **
 ** Description : Use reflection to call methods on View above Android 2.2
 **
 ** Creation    : 2012/11/13
 ** Author      : Roger (yixx@ucweb.com)
 ** History     :
 **               Creation, 2012/11/13, Roger, Create the file
 **
 ****************************************************************************/
package com.uc.webkit.helper;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import android.util.Log;
import android.view.View;

public final class ViewHelper {
    private static final String TAG = "ViewHelper";

    public static final int LAYER_TYPE_NONE = 0;
    public static final int LAYER_TYPE_SOFTWARE = 1;
    public static final int LAYER_TYPE_HARDWARE = 2;

    public static final int SYSTEM_UI_FLAG_VISIBLE = 0;
    public static final int SYSTEM_UI_FLAG_LOW_PROFILE = 1;
    public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 2;
    public static final int SYSTEM_UI_FLAG_FULLSCREEN = 4;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE = 0x00000800;
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 0x00001000;

    public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 1024;
    public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 512;
    public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = 256;

    private static final Class<View> viewClass = View.class;

    private static Method methodIsHardwareAccelerated = null;
    private static Method methodGetLayerType = null;
    private static Method methodGetViewRootImpl = null;
    private static Method methodSetSystemUiVisibility = null;
    private static Method methodGetScaleX = null;
    private static Method methodGetScaleY = null;

    private static Field fieldScrollX = null;
    private static Field fieldScrollY = null;

    private static Method methodGetVerticalScrollFactor = null;
    private static Method methodGetHorizontalScrollFactor = null;
    private static Method methodSetFrame = null;
    private static Method methodExecuteHardwareAction = null;

    static {
        try {
            try {
                methodGetViewRootImpl = viewClass.getMethod("getViewRootImpl");
            } catch (NoSuchMethodException e) {
                methodGetViewRootImpl = viewClass.getMethod("getViewRoot");
            }
            
            methodIsHardwareAccelerated = viewClass.getMethod("isHardwareAccelerated");
            methodGetLayerType = viewClass.getMethod("getLayerType");
            methodSetSystemUiVisibility = viewClass.getMethod(
                    "setSystemUiVisibility", new Class<?>[] { Integer.TYPE });
            
            methodGetScaleX = viewClass.getMethod("getScaleX");
            methodGetScaleY = viewClass.getMethod("getScaleY");

            fieldScrollX = viewClass.getDeclaredField("mScrollX");
            fieldScrollX.setAccessible(true);
            fieldScrollY = viewClass.getDeclaredField("mScrollY");
            fieldScrollY.setAccessible(true);

            try {
                methodGetVerticalScrollFactor = viewClass.getDeclaredMethod("getVerticalScrollFactor");
                methodGetVerticalScrollFactor.setAccessible(true);
                methodGetHorizontalScrollFactor = viewClass.getDeclaredMethod("getHorizontalScrollFactor");
                methodGetHorizontalScrollFactor.setAccessible(true);
            } catch (Throwable t) {
                Log.e(TAG, "android.view.View can not get method getVerticalScrollFactor!");
            }

            try {
                methodSetFrame = viewClass.getDeclaredMethod("setFrame",
                        new Class<?>[] { Integer.TYPE, Integer.TYPE, Integer.TYPE, Integer.TYPE });
                methodSetFrame.setAccessible(true);
            } catch (Throwable t) {
                Log.e(TAG, "android.view.View can not get method setFrame!");
            }
            
            try {
                methodExecuteHardwareAction = viewClass.getMethod("executeHardwareAction",
                        Runnable.class);
            } catch (Throwable t) {
                Log.e(TAG, "android.view.View can not get method executeHardwareAction!");
            }
        } catch (Throwable t) {
        }
    }

    static public boolean isHardwareAccelerated(View view) {
        if (methodIsHardwareAccelerated != null) {
            try {
                Object result = methodIsHardwareAccelerated.invoke(view);
                return (Boolean) result;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return false;
    }

    static public int getLayerType(View view) {
        if (methodGetLayerType != null) {
            try {
                Object result = methodGetLayerType.invoke(view);
                return (Integer) result;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return LAYER_TYPE_NONE;
    }

    static public Object getViewRootImpl(View view) {
        if (methodGetViewRootImpl != null) {
            try {
                Object result = methodGetViewRootImpl.invoke(view);
                return result;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return null;
    }

    static public void setSystemUiVisibility(View v, int visibility) {
        if (methodSetSystemUiVisibility != null) {
            try {
                methodSetSystemUiVisibility.invoke(v, visibility);
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    static public float getScaleX(View v) {
        if (methodGetScaleX != null) {
            try {
                Object result = methodGetScaleX.invoke(v);
                return (Float) result;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return 1.0f;
    }

    static public float getScaleY(View v) {
        if (methodGetScaleY != null) {
            try {
                Object result = methodGetScaleY.invoke(v);
                return (Float) result;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return 1.0f;
    }

    static public void setFieldScrollX(View v, int sx) {
        if (fieldScrollX != null) {
            try {
                fieldScrollX.set(v, Integer.valueOf(sx));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    static public void setFieldScrollY(View v, int sy) {
        if (fieldScrollY != null) {
            try {
                fieldScrollY.set(v, Integer.valueOf(sy));
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
    }

    static public float getVerticalScrollFactor(View v) {
        if (methodGetVerticalScrollFactor != null) {
            try {
                Object result = methodGetVerticalScrollFactor.invoke(v);
                return (Float) result;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return 1.0f;
    }

    static public float getHorizontalScrollFactor(View v) {
        if (methodGetHorizontalScrollFactor != null) {
            try {
                Object result = methodGetHorizontalScrollFactor.invoke(v);
                return (Float) result;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return 1.0f;
    }
    
    static public boolean setFrame(View v, int left, int top, int right, int bottom) {
        if (methodSetFrame != null) {
            try {
                Object result = methodSetFrame.invoke(v, left, top, right, bottom);
                return (Boolean) result;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return false;
    }
    
    static public boolean executeHardwareAction(View v, Runnable action) {
        if (methodExecuteHardwareAction != null) {
            try {
                Object result = methodExecuteHardwareAction.invoke(v, action);
                return (Boolean) result;
            } catch (Throwable t) {
                t.printStackTrace();
            }
        }
        return false;        
    }
}
