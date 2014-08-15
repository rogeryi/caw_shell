/****************************************************************************
**
** Copyright (C) 2010-2012 UC Mobile Ltd. All Rights Reserved
** File        : ContextHelper.java
**
** Description : Use reflection to call methods on Context above Android 2.2
**
** Creation    : 2012/11/12
** Author      : Roger (yixx@ucweb.com)
** History     :
**               Creation, 2012/11/12, Roger, Create the file
**
****************************************************************************/
package com.uc.webkit.helper;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

public final class ContextHelper {
	
	public static interface ComponentCallbacks2 {

	    /**
	     * Level for {@link #onTrimMemory(int)}: the process is nearing the end
	     * of the background LRU list, and if more memory isn't found soon it will
	     * be killed.
	     */
	    static final int TRIM_MEMORY_COMPLETE = 80;
	    
	    /**
	     * Level for {@link #onTrimMemory(int)}: the process is around the middle
	     * of the background LRU list; freeing memory can help the system keep
	     * other processes running later in the list for better overall performance.
	     */
	    static final int TRIM_MEMORY_MODERATE = 60;
	    
	    /**
	     * Level for {@link #onTrimMemory(int)}: the process has gone on to the
	     * LRU list.  This is a good opportunity to clean up resources that can
	     * efficiently and quickly be re-built if the user returns to the app.
	     */
	    static final int TRIM_MEMORY_BACKGROUND = 40;
	    
	    /**
	     * Level for {@link #onTrimMemory(int)}: the process had been showing
	     * a user interface, and is no longer doing so.  Large allocations with
	     * the UI should be released at this point to allow memory to be better
	     * managed.
	     */
	    static final int TRIM_MEMORY_UI_HIDDEN = 20;

	    /**
	     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
	     * background process, but the device is running extremely low on memory
	     * and is about to not be able to keep any background processes running.
	     * Your running process should free up as many non-critical resources as it
	     * can to allow that memory to be used elsewhere.  The next thing that
	     * will happen after this is {@link #onLowMemory()} called to report that
	     * nothing at all can be kept in the background, a situation that can start
	     * to notably impact the user.
	     */
	    static final int TRIM_MEMORY_RUNNING_CRITICAL = 15;

	    /**
	     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
	     * background process, but the device is running low on memory.
	     * Your running process should free up unneeded resources to allow that
	     * memory to be used elsewhere.
	     */
	    static final int TRIM_MEMORY_RUNNING_LOW = 10;


	    /**
	     * Level for {@link #onTrimMemory(int)}: the process is not an expendable
	     * background process, but the device is running moderately low on memory.
	     * Your running process may want to release some unneeded resources for
	     * use elsewhere.
	     */
	    static final int TRIM_MEMORY_RUNNING_MODERATE = 5;

	    /**
	     * Called when the operating system has determined that it is a good
	     * time for a process to trim unneeded memory from its process.  This will
	     * happen for example when it goes in the background and there is not enough
	     * memory to keep as many background processes running as desired.  You
	     * should never compare to exact values of the level, since new intermediate
	     * values may be added -- you will typically want to compare if the value
	     * is greater or equal to a level you are interested in.
	     *
	     * <p>To retrieve the processes current trim level at any point, you can
	     * use {@link android.app.ActivityManager#getMyMemoryState
	     * ActivityManager.getMyMemoryState(RunningAppProcessInfo)}.
	     *
	     * @param level The context of the trim, giving a hint of the amount of
	     * trimming the application may like to perform.  May be
	     * {@link #TRIM_MEMORY_COMPLETE}, {@link #TRIM_MEMORY_MODERATE},
	     * {@link #TRIM_MEMORY_BACKGROUND}, {@link #TRIM_MEMORY_UI_HIDDEN},
	     * {@link #TRIM_MEMORY_RUNNING_CRITICAL}, {@link #TRIM_MEMORY_RUNNING_LOW},
	     * or {@link #TRIM_MEMORY_RUNNING_MODERATE}.
	     */
	    void onTrimMemory(int level);
	    
	    /**
	     * Called by the system when the device configuration changes while your
	     * component is running.  Note that, unlike activities, other components
	     * are never restarted when a configuration changes: they must always deal
	     * with the results of the change, such as by re-retrieving resources.
	     * 
	     * <p>At the time that this function has been called, your Resources
	     * object will have been updated to return resource values matching the
	     * new configuration.
	     * 
	     * @param newConfig The new device configuration.
	     */
	    void onConfigurationChanged(Configuration newConfig);
	    
	    /**
	     * This is called when the overall system is running low on memory, and
	     * would like actively running process to try to tighten their belt.  While
	     * the exact point at which this will be called is not defined, generally
	     * it will happen around the time all background process have been killed,
	     * that is before reaching the point of killing processes hosting
	     * service and foreground UI that we would like to avoid killing.
	     * 
	     * <p>Applications that want to be nice can implement this method to release
	     * any caches or other unnecessary resources they may be holding on to.
	     * The system will perform a gc for you after returning from this method.
	     */
	    void onLowMemory();
	}
	
	private static final Class<Context> contextClass = Context.class;
	private static Class<?> componentCallbacksInterface = null;
	private static Class<?> componentCallbacks2Interface = null;

	private static Method methodRegisterComponentCallbacks = null;
	
	static {
		try {
			componentCallbacksInterface = Class.forName("android.content.ComponentCallbacks");
			componentCallbacks2Interface = Class.forName("android.content.ComponentCallbacks2");
			methodRegisterComponentCallbacks = contextClass.getMethod(
					"registerComponentCallbacks", new Class<?>[] { componentCallbacksInterface });
		} catch (Throwable t) {
		}
	}
	
	private static class ComponentCallbacks2InvocationHandler implements InvocationHandler {
		
		private final ComponentCallbacks2 mCallback;

		public ComponentCallbacks2InvocationHandler(ComponentCallbacks2 callback) {
			mCallback = callback;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
				throws Throwable {
			try {
				Log.i("ContextHelper", "Call " + method.toString());
				
				Class<?> callbackClass = mCallback.getClass();
				Method realMethod = callbackClass.getMethod(method.getName(), 
						method.getParameterTypes());
				return realMethod.invoke(mCallback, args);
			} catch (Throwable t) {
				Log.e("ContextHelper", "Call " + method.toString() + " error!");
				t.printStackTrace();
				throw t;
			}
		}
	}
	
	public static void registerComponentCallbacks(Context context, ComponentCallbacks2 callback) {
		if (componentCallbacks2Interface != null && 
				methodRegisterComponentCallbacks != null) {
			ComponentCallbacks2InvocationHandler handler = 
					new ComponentCallbacks2InvocationHandler(callback);
			try {
				Object proxy = Proxy.newProxyInstance(
						componentCallbacks2Interface.getClassLoader(), 
						new Class<?>[] { componentCallbacks2Interface }, 
						handler);
				methodRegisterComponentCallbacks.invoke(context, proxy);
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
	}
}
