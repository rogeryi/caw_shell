/****************************************************************************
**
** Copyright (C) 2010-2012 UC Mobile Ltd. All Rights Reserved
** File        : ViewRootHelper.java
**
** Description : Use reflection to call methods on ViewRoot/ViewRootImpl above Android 2.2
**
** Creation    : 2012/11/13
** Author      : Roger (yixx@ucweb.com)
** History     :
**               Creation, 2012/11/13, Roger, Create the file
**
****************************************************************************/
package com.uc.webkit.helper;

import java.lang.reflect.Method;

import android.graphics.Rect;
import android.util.Log;
import android.view.KeyEvent;

public class ViewRootHelper {
	private static final String TAG = "ViewRootHelper";
	
	private static Class<?> viewRootImplClass;
	
	private static Method methodDetachFunctorInt = null;
	private static Method methodAttachFunctorInt = null;
	private static Method methodDetachFunctorLong = null;
	private static Method methodInvalidate = null;
	private static Method methodInvalidateChildInParent = null;
	private static Method methodDispatchUnhandledKey = null;
	
	static {
		try {
            viewRootImplClass = Class.forName("android.view.ViewRootImpl");
            
			// In Android L, to support 64bits, the native-ptr pass through JNI 
			// has been changed from Integer to Long type, 
			// and attachFunctor method has been removed
			try {
				methodDetachFunctorInt = viewRootImplClass.getMethod("detachFunctor", new Class<?>[] {Integer.TYPE});		
				methodAttachFunctorInt = viewRootImplClass.getMethod("attachFunctor", new Class<?>[] {Integer.TYPE});				
			} catch (Throwable t) {
				methodDetachFunctorLong = viewRootImplClass.getMethod("detachFunctor", new Class<?>[] {Long.TYPE});
			}
			methodInvalidate = viewRootImplClass.getDeclaredMethod("invalidate");
			if (methodInvalidate != null)
				methodInvalidate.setAccessible(true);
			
			methodInvalidateChildInParent = viewRootImplClass.getMethod(
					"invalidateChildInParent", 
					new Class<?>[] {int[].class, Rect.class});
			
			try {
				methodDispatchUnhandledKey = viewRootImplClass.getMethod("dispatchUnhandledKey", KeyEvent.class);
			} catch (Throwable t) {
				Log.e(TAG, "android.view.ViewRootImpl can not get method dispatchUnhandledKey!");
			}
		} catch (Throwable t) {
			t.printStackTrace();
		}
	}
	
	public static void detachFunctor(Object viewRoot, long functor) {
		if (viewRootImplClass != null && methodDetachFunctorInt != null) {
			try {
				methodDetachFunctorInt.invoke(viewRoot, (int) functor);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		if (viewRootImplClass != null && methodDetachFunctorLong != null) {
			try {
				methodDetachFunctorLong.invoke(viewRoot, functor);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
	
	public static boolean attachFunctor(Object viewRoot, long functor) {
		if (viewRootImplClass != null && methodAttachFunctorInt != null) {
			try {
				Object result = methodAttachFunctorInt.invoke(viewRoot, (int) functor);
				return (Boolean) result;
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return false;
	}
	
	public static boolean invalidate(Object viewRoot) {
		if (viewRootImplClass != null && methodInvalidate != null) {
			try {
				methodInvalidate.invoke(viewRoot);
				return true;
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return false;
	}	
	
	public static boolean invalidateChildInParent(Object viewRoot, Rect dirty) {
		if (viewRootImplClass != null && methodInvalidateChildInParent != null) {
			try {
				methodInvalidateChildInParent.invoke(viewRoot, null, dirty);
				return true;
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return false;
	}
	
	public static void dispatchUnhandledKey(Object viewRoot, KeyEvent event) {
		if (viewRootImplClass != null && methodDispatchUnhandledKey != null) {
			try {
				methodDispatchUnhandledKey.invoke(viewRoot, event);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
}
