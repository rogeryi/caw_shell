/****************************************************************************
**
** Copyright (C) 2010-2012 UC Mobile Ltd. All Rights Reserved
** File        : ActivityManagerHelper.java
**
** Description : Use reflection to call methods on ActivityManagerHelper
** 				 above Android 2.2
**
** Creation    : 2012/11/07
** Author      : Roger (yixx@ucweb.com)
** History     :
**               Creation, 2012/11/07, Roger, Create the file
**
****************************************************************************/
package com.uc.webkit.helper;

import java.lang.reflect.Method;

import com.uc.webkit.utils.SystemInfo;

import android.app.ActivityManager;

public final class ActivityManagerHelper {
	private static Class<ActivityManager> amClass = ActivityManager.class;
	private static Method methodGetLargeMemoryClass = null;
	
	static {
		try {
			methodGetLargeMemoryClass = amClass.getMethod("getLargeMemoryClass");
		} catch (Throwable t) {
		}
	}
	
	static public boolean isHighEndGfx() {
		
        final int maxSize = SystemInfo.getDisplayMaxWidth();
        final int minSize = SystemInfo.getDisplayMinWidth();
        if (minSize >= 720 || maxSize >= 1280)
			return true;
		
		if (SystemInfo.getMemTotalSize() >= (512*1024*1024))
			return true;
		
		return false;
	}
	
	static public int getLargeMemoryClass(ActivityManager am) {
		if (methodGetLargeMemoryClass != null) {
			try {
				Object result = methodGetLargeMemoryClass.invoke(am);
				return (Integer)result;
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return am.getMemoryClass();
	}
}
