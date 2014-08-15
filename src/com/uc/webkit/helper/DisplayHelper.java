/****************************************************************************
**
** Copyright (C) 2010-2013 UC Mobile Ltd. All Rights Reserved
** File        : DisplayHelper.java
**
** Description : Use reflection to call methods on DisplayHelper
** 				 above Android 2.2
**
** Creation    : 2013/6/06
** Author      : Roger (yixx@ucweb.com)
** History     :
**               Creation, 2013/6/06, Roger, Create the file
**
****************************************************************************/
package com.uc.webkit.helper;

import java.lang.reflect.Method;

import android.os.Build;
import android.util.DisplayMetrics;
import android.view.Display;

public final class DisplayHelper {
	private static Class<Display> displayClass = Display.class;
	private static Method methodGetRealMetrics = null;
	
	static {
		try {
			methodGetRealMetrics = displayClass.getMethod("getRealMetrics", new Class<?>[] {DisplayMetrics.class});
		} catch (Throwable t) {
		}
	}
	
	static public void getRealMetrics(Display display, DisplayMetrics outMetrics) {
		if (methodGetRealMetrics != null
				//by huaj for mantis:0253545 
				&& Build.VERSION.SDK_INT >= 17) {
			try {
				methodGetRealMetrics.invoke(display, outMetrics);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		} else {
			display.getMetrics(outMetrics);
		}
	}
}

