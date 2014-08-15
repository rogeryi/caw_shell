/****************************************************************************
**
** Copyright (C) 2010-2013 UC Mobile Ltd. All Rights Reserved
** File        : SystemInfo.java
**
** Description : Get system information such as memory, cpu, gpu, etc...
**
** Creation    : 2013/05/07
** Author      : Roger (yixx@ucweb.com)
** History     :
**               Creation, 2013/05/07, Roger, Create the file
**
****************************************************************************/
package com.uc.webkit.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Pattern;

import com.uc.webkit.helper.DisplayHelper;

import android.app.ActivityManager;
import android.content.Context;
import android.content.res.Configuration;
import android.opengl.GLES10;
import android.os.Debug;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.WindowManager;

public final class SystemInfo {
	private static final boolean DEBUG = false;
	private static final String TAG = "sys";
	
	private static ActivityManager sManager = null;
	private static Display sDisplay = null;	
    private static ActivityManager.MemoryInfo sMemInfo;
    private static Debug.MemoryInfo sProcMemInfo;
    
    private static float sDisplayRefreshRate = 60.0f;
    
    private static long sTotalSize = 0;
    private static long sFreeSize = 0;
    private static long sCachedSize = 0;
    
    private static int sCpuCores = 0;
    private static int sCpuMaxFreq = 0;
    
    private static final DisplayMetrics sDisplayMetrics = new DisplayMetrics();
    private static final DisplayMetrics sDisplayRealMetrics = new DisplayMetrics();
    
    private static int sScreenLayout = 0;
    
    private static boolean matchText(byte[] buffer, int index, String text) {
        int N = text.length();
        if ((index+N) >= buffer.length) {
            return false;
        }
        for (int i=0; i<N; i++) {
            if (buffer[index+i] != text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static long extractMemValue(byte[] buffer, int index) {
        while (index < buffer.length && buffer[index] != '\n') {
            if (buffer[index] >= '0' && buffer[index] <= '9') {
                int start = index;
                index++;
                while (index < buffer.length && buffer[index] >= '0'
                    && buffer[index] <= '9') {
                    index++;
                }
				String str = new String(buffer, 0, start, index-start);
                return ((long)Integer.parseInt(str)) * 1024;
            }
            index++;
        }
        return 0;
    }

    private static void readMemInfo() {
        // Permit disk reads here, as /proc/meminfo isn't really "on
        // disk" and should be fast.  TODO: make BlockGuard ignore
        // /proc/ and /sys/ files perhaps?
    	//StrictMode appear until 2.3, so do not used in here 
        //StrictMode.ThreadPolicy savedPolicy = StrictMode.allowThreadDiskReads();
        try {
            final byte[] buffer = new byte[1024];        	
            sTotalSize = 0;
            sFreeSize = 0;
            sCachedSize = 0;
            FileInputStream is = new FileInputStream("/proc/meminfo");
            int len = is.read(buffer);
            is.close();
            final int BUFLEN = buffer.length;
            int count = 0;
            for (int i=0; i<len && count < 3; i++) {
                if (matchText(buffer, i, "MemTotal")) {
                    i += 8;
                    sTotalSize = extractMemValue(buffer, i);
                    count++;
                } else if (matchText(buffer, i, "MemFree")) {
                    i += 7;
                    sFreeSize = extractMemValue(buffer, i);
                    count++;
                } else if (matchText(buffer, i, "Cached")) {
                    i += 6;
                    sCachedSize = extractMemValue(buffer, i);
                    count++;
                }
                while (i < BUFLEN && buffer[i] != '\n') {
                    i++;
                }
            }
        } catch (java.io.FileNotFoundException e) {
        } catch (java.io.IOException e) {
        } finally {
            //StrictMode.setThreadPolicy(savedPolicy);
        }
    }
	
	public static void setup(Context context) {
		sManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		sDisplay = wm.getDefaultDisplay();
		sDisplayRefreshRate = sDisplay.getRefreshRate();
		sDisplay.getMetrics(sDisplayMetrics);
		DisplayHelper.getRealMetrics(sDisplay, sDisplayRealMetrics);		
        sScreenLayout = context.getResources().getConfiguration().screenLayout;
	}
	
	/** 
	 * Memory Information
	 */	
    public static long getMemTotalSize() {
    	if (sTotalSize == 0)
    		readMemInfo();
    	
        return sTotalSize;
    }
    
    public static int getMemTotalSizeMb() {
    	final long total = getMemTotalSize();
    	final long mb = 1024 * 1024;
    	return (int) (total / mb);
    }
    
    public static long getMemFreeSize() {
    	readMemInfo();
    	
        return sFreeSize;
    }

    public static long getMemCachedSize() {
    	readMemInfo();
    	
        return sCachedSize;
    }
    
	public static long getMemAvailableSize() {
		getActivityMemoryInfo();
		
		return sMemInfo.availMem;
	}
	
	public static Debug.MemoryInfo getProcessMemoryInfo() {
		if (sProcMemInfo == null)
			sProcMemInfo = new Debug.MemoryInfo();
		
		Debug.getMemoryInfo(sProcMemInfo);
		return sProcMemInfo;
	}
	
	public static ActivityManager.MemoryInfo getActivityMemoryInfo() {
		if (sMemInfo == null) {
			sMemInfo = new ActivityManager.MemoryInfo();
		}		
        sManager.getMemoryInfo(sMemInfo);
        return sMemInfo;
	}
	
	/** 
	 * CPU Information
	 */	
	
	/**
	 * Gets the number of cores available in this device, across all processors.
	 * Requires: Ability to peruse the filesystem at "/sys/devices/system/cpu"
	 * @return The number of cores, or 1 if failed to get result
	 */
	public static int getCpuCoresNum() {
	    //Private Class to display only CPU devices in the directory listing
	    class CpuFilter implements FileFilter {
	        @Override
	        public boolean accept(File pathname) {
	            //Check if filename is "cpu", followed by a single digit number
	            if(Pattern.matches("cpu[0-9]", pathname.getName())) {
	                return true;
	            }
	            return false;
	        }      
	    }
	    
		if (sCpuCores > 0)
			return sCpuCores;
		
	    try {
	        //Get directory containing CPU info
	        File dir = new File("/sys/devices/system/cpu/");
	        //Filter to only list the devices we care about
	        File[] files = dir.listFiles(new CpuFilter());
	        if (DEBUG) Log.d(TAG, "CPU Count: "+files.length);
	        //Return the number of cores (virtual CPU devices)
	        sCpuCores = files.length > 0 ? files.length : 1;
	    } catch(Exception e) {
	        //Print exception
	    	if (DEBUG)  Log.d(TAG, "CPU Count: Failed.");
	    	e.printStackTrace();
	        //Default to return 1 core
	    	sCpuCores = 1;
	    }
	    return sCpuCores;
	}
	
	public static int getCpuMaxFreq() {
		if (sCpuMaxFreq > 0)
			return sCpuMaxFreq;
		
		try {
            final byte[] buffer = new byte[128];
            FileInputStream is = new FileInputStream("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq");
            int len = is.read(buffer);
            is.close();
			String str = new String(buffer, 0, len - 1);
            sCpuMaxFreq = Integer.parseInt(str) / 1000;
		} catch (IOException ex) {
			ex.printStackTrace();
			sCpuMaxFreq = 800;
		} catch (NumberFormatException nfe) {
			nfe.printStackTrace();
			sCpuMaxFreq = 800;
		}
		return sCpuMaxFreq;
	}
	
	/** 
	 * GPU Information
	 */	
	public static String getGlRenderer() {
		return GLES10.glGetString(GLES10.GL_RENDERER);		
	}
	
	/** 
	 * Screen Information
	 */			
	public static int getDisplayWidth() {
		return sDisplayMetrics.widthPixels;
	}
	
	public static int getDisplayHeight() {
		return sDisplayMetrics.heightPixels;
	}
	
	public static int getDisplayMaxWidth() {
        return Math.max(sDisplayMetrics.widthPixels, sDisplayMetrics.heightPixels);
	}
	
	public static int getDisplayMinWidth() {
		return Math.min(sDisplayMetrics.widthPixels, sDisplayMetrics.heightPixels);
	}
	
	public static int getDisplayRealWidth() {
		return sDisplayRealMetrics.widthPixels;
	}
	
	public static int getDisplayRealHeight() {
		return sDisplayRealMetrics.heightPixels;
	}
	
	public static int getDisplayRealMaxWidth() {
        return Math.max(sDisplayRealMetrics.widthPixels, sDisplayRealMetrics.heightPixels);
	}
	
	public static int getDisplayRealMinWidth() {
		return Math.min(sDisplayRealMetrics.widthPixels, sDisplayRealMetrics.heightPixels);
	}
	
	public static float getDisplayRefreshRate() {
		return sDisplayRefreshRate;
	}
	
    public static boolean isTablet() {
        return (sScreenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
        		>= Configuration.SCREENLAYOUT_SIZE_LARGE;
    }	
}
