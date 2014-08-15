/****************************************************************************
**
** Copyright (C) 2010-2012 UC Mobile Ltd. All Rights Reserved
** File        : CanvasHelper.java
**
** Description : Use reflection to call methods on Canvas above Android 2.2
**
** Creation    : 2012/11/07
** Author      : Roger (yixx@ucweb.com)
** History     :
**               Creation, 2012/11/07, Roger, Create the file
**
****************************************************************************/
package com.uc.webkit.helper;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;

import android.graphics.Canvas;

public final class CanvasHelper {
	
	public static final boolean IS_HARDWARE_ACCELERATED_FAST_CALL = true;
	
	private static final Class<Canvas> canvasClass = Canvas.class;
	private static Class<?> hwCanvasClass;
	
	private static Method methodIsHardwareAccelerated;
	private static Method methodCallDrawGLFunctionInt;
	private static Method methodCallDrawGLFunctionLong;
	
	private static WeakReference<Canvas> sLastCanvas;
	private static boolean sLastCanvasIsHardwareAccelerated;
	
	static {
		try {
			methodIsHardwareAccelerated = canvasClass.getMethod("isHardwareAccelerated");
			hwCanvasClass = Class.forName("android.view.HardwareCanvas");
			// In Android L, to support 64bits, the native-ptr pass through JNI 
			// has been changed from Integer to Long type
			try {
				methodCallDrawGLFunctionInt = hwCanvasClass.getMethod("callDrawGLFunction", new Class<?>[] {Integer.TYPE});
			} catch (Throwable t) {
				methodCallDrawGLFunctionLong = hwCanvasClass.getMethod("callDrawGLFunction", new Class<?>[] {Long.TYPE});				
			}
		} catch (Throwable t) {
		}
	}
	
	static public boolean isHardwareAccelerated(Canvas canvas) {
		// Return cached value, faster than use reflection
		if (sLastCanvas != null && sLastCanvas.get() == canvas) {
			return sLastCanvasIsHardwareAccelerated;
		}
		
		sLastCanvasIsHardwareAccelerated = false;		
		if (methodIsHardwareAccelerated != null) {
			try {
				Object result = methodIsHardwareAccelerated.invoke(canvas);
				sLastCanvasIsHardwareAccelerated = (Boolean) result;
			} catch (Throwable t) {
			}
		}
		if (IS_HARDWARE_ACCELERATED_FAST_CALL)
			sLastCanvas = new WeakReference<Canvas>(canvas);
		return sLastCanvasIsHardwareAccelerated;
	}
	
	static public int callDrawGLFunction(Canvas canvas, long functor) {
		if (methodCallDrawGLFunctionInt != null) {
			try {
				Object result = methodCallDrawGLFunctionInt.invoke(canvas, (int) functor);
				return (Integer) result;
			} catch (Throwable t) {
				t.printStackTrace();
			}
		} else if (methodCallDrawGLFunctionLong != null) {
			try {
			    Object result = methodCallDrawGLFunctionLong.invoke(canvas, functor);
                return (Integer) result;
			} catch (Throwable t) {
				t.printStackTrace();
			}			
		}
		return 0;
	}
}
