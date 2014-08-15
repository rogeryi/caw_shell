/****************************************************************************
**
** Copyright (C) 2010-2012 UC Mobile Ltd. All Rights Reserved
** File        : WebViewFPS.java
**
** Description : Used to calculate FPS for WebView
**
** Creation    : 2012/09/21
** Author      : Roger (yixx@ucweb.com)
** History     :
**               Creation, 2012/09/21, Roger, Create the file
**
****************************************************************************/
package com.uc.webkit.utils;

import android.app.ActivityManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.os.Debug;
import android.view.View;

import com.uc.webkit.helper.CanvasHelper;
/**
 * Used to calculate FPS for WebView.
 */
public class WebViewFPS {
	public static boolean ENABLE = false;
	//add by zhaochuang
	public static boolean ENABLE_AC_RENDERING_STATSTIC = false;
	// Enable profiling fps in GL draw path - drawGL in WebView.cpp,
	// this is more appropriate than in onDraw of WebView.java
	public static boolean ENABLE_GL = ENABLE;
	public static boolean ENABLE_GL_EX = false;
	public static boolean ENABLE_GL_CANVAS = false;	
	public static boolean ENABLE_GL_AUTO_REDRAW = false;
	public static boolean ENABLE_MEMORY_INFO = ENABLE;
	public static boolean ENABLE_NETWORK_INFO = ENABLE;
	public static boolean ENABLE_MEMORY_VERBOSE = false;
	
	public static final String FPS 			= "debug.uc.fps";
	public static final String FPS_EX 		= "debug.uc.fpsex";
	public static final String FPS_CANVAS 	= "debug.uc.fpscanvas";
	
	private static final int CACL_CIRCLE = 1000;
	
	private static long sFirstFrameCompensation = 17;
	private static long sOverallTime = 0;
	private static long sDrawTime = 0;
	private static long sLastTime = -1;
	private static double sOverallFps = 0;
	private static double sDrawFps = 0;
	private static int sTotalFrames = 0;
	private static int sFrames = 0;
	private static Paint sFpsBgPaint = null;
	private static Paint sFpsTxtPaint = null;
	private static Paint sFpsWarningTxtPaint = null;
	
	private static double sGLFps = 0;
	private static double sGLDrawAvg = 0;
	private static double sGLDrawMax = 0;	
	private static double sGLDrawMin = 0;	
	private static double sGLBaseLayerTexMem = 0;
	private static double sGLLayerTexMem = 0;
	private static double sGLGbMem = 0;
	private static int	  sGLLayerMode = 0;
	
	private static Debug.MemoryInfo sMemInfo;
	private static ActivityManager.MemoryInfo sActMemInfo;
	private static long sLastMemInfoUpdateTime;
	
	//add by zhouliang for  performance test
	public static boolean ENABLE_PERFORANCE_TEST = false;
	
	public static double getFPS() {
		return sGLFps != 0 ? sGLFps : sOverallFps;
	}
	
	//add by zhaochuang for AC rendering statstic
	public static double getGLBaseLayerTexMem() {
	    return sGLBaseLayerTexMem;
	}
	
	public static double getGLLayerTexMem() {
	    return sGLLayerTexMem;
	}
	
	public static double getGLGbMem() {
	    return sGLGbMem;
	}
	
	//for T1 time display
	//TODO: should move to new class
	private static long sT0 = 0;
	private static long sT1 = 0;
	public static void setT0(){
		sT0 = System.currentTimeMillis();
	}

	public static void setT1(){
		sT1 = System.currentTimeMillis();
	}
	
	public static void setGLFps(boolean isScrolling, double fps, double drawAvg, double drawMax, double drawMin,
			double baseLayerTexMem, double layerTexMem, double gbMem, int layerMode) {
		sGLFps = fps;
		sGLDrawAvg = drawAvg;
		sGLDrawMax = drawMax;
		sGLDrawMin = drawMin;
		sGLBaseLayerTexMem = baseLayerTexMem;
		sGLLayerTexMem = layerTexMem;
		sGLGbMem = gbMem;
		sGLLayerMode = layerMode;
		
		if (ENABLE && !isScrolling) {
			final long now = System.currentTimeMillis();
			if (ENABLE_MEMORY_INFO && now - sLastMemInfoUpdateTime > 5 * 1000) {
				sMemInfo = SystemInfo.getProcessMemoryInfo();
				sActMemInfo = SystemInfo.getActivityMemoryInfo();
				sLastMemInfoUpdateTime = now;
			}			
		} else if (ENABLE) {
			sLastMemInfoUpdateTime = System.currentTimeMillis();
		}
	}
	
	public static float getSmoothnessRate(final double frameRate) {
		final float displayRefreshRate = SystemInfo.getDisplayRefreshRate();
		float rate = 0;
		if (frameRate >= displayRefreshRate) {
			rate = 100;
		} else {
			rate = (float) (frameRate - (displayRefreshRate - frameRate)) * 100 / displayRefreshRate;
		}
		
		if (displayRefreshRate < 60) {
			rate *= (displayRefreshRate) / 60;
		}
		return rate >= 0 ? rate : 0;
	}
	
	public final static int SMOOTHNESS_VERY_SMOOTH = 5;
	public final static int SMOOTHNESS_SMOOTH = 4;
	public final static int SMOOTHNESS_SLIGHTLY_JANK = 3;
	public final static int SMOOTHNESS_JANK = 2;
	public final static int SMOOTHNESS_VERY_JANK = 1;
	public final static int SMOOTHNESS_FROZEN = 0;
	
	public static int getSmoothnessLevel(final float smoothnessRate) {
		final int srate = Math.round(smoothnessRate);
		if (srate >= 90) return SMOOTHNESS_VERY_SMOOTH;
		else if (srate >= 80) return SMOOTHNESS_SMOOTH;
		else if (srate >= 60) return SMOOTHNESS_SLIGHTLY_JANK;
		else if (srate >= 40) return SMOOTHNESS_JANK;
		else if (srate >= 0) return SMOOTHNESS_VERY_JANK;
		
		return SMOOTHNESS_FROZEN;
	}
	
	public static void fps(long begin, Canvas canvas, View view) {
		if (!(ENABLE || ENABLE_PERFORANCE_TEST))
			return;

		if (sFpsBgPaint == null) {
			sFpsBgPaint = new Paint();
			sFpsBgPaint.setStyle(Style.FILL);
			sFpsBgPaint.setColor(0x7f5f5f5f);
			sFpsTxtPaint = new Paint();
			sFpsTxtPaint.setStyle(Style.FILL);
			sFpsTxtPaint.setColor(0xff00ff00);
			sFpsTxtPaint.setTextSize(20);
			sFpsTxtPaint.setTextAlign(Align.RIGHT);
			sFpsWarningTxtPaint = new Paint();
			sFpsWarningTxtPaint.setStyle(Style.FILL);
			sFpsWarningTxtPaint.setColor(0xffff0000);
			sFpsWarningTxtPaint.setTextSize(20);
			sFpsWarningTxtPaint.setTextAlign(Align.RIGHT);				
		}
		
        final int width = view.getWidth();
        final int height = view.getHeight();
        final int scrollX = view.getScrollX();
        final int scrollY = view.getScrollY();		
		final boolean showGLFps = ENABLE_GL && sGLFps != 0;
		
		if (!showGLFps) {
			if (sLastTime <= 0) {
				sLastTime = System.currentTimeMillis() - sFirstFrameCompensation;
			}
			
			final long now = System.currentTimeMillis();
			final long used = Math.max(now - begin, 1);
			final long pass = now - sLastTime;
		
		    // If we wait too long for one frame, regard as a pause, 
		    // skip it, otherwise continue to accumulate the frames
			if (pass - used < 500) {
				sOverallTime += pass;
				sDrawTime += used;
				++sFrames;
				++sTotalFrames;
			}
			sLastTime = now;
			
			// calculation then reset every second
			if (sOverallTime > CACL_CIRCLE) {
				sDrawFps = sFrames * 1000.0 / sDrawTime;
				sOverallFps = sFrames * 1000.0 / sOverallTime;	
				sOverallTime = 0;
				sDrawTime = 0;
				sFrames = 0;
			}			
		}
		
		//add by zhouliang, do not show fps in performance test
		if (ENABLE_PERFORANCE_TEST) {
			return;
		}	

		//cal T1 time
		//TODO: should move to new class
		String t1Str = sT1 > sT0 ? String.format("T1: %.1fs", (sT1 - sT0) * 0.001) : "T1: ? sec";
		String version = CanvasHelper.isHardwareAccelerated(canvas) ? "hd>" : "sf>";

		//cal fps time
		final float rrate = SystemInfo.getDisplayRefreshRate();
		final float srate = getSmoothnessRate(showGLFps ? sGLFps : sOverallFps);
		String fpsStr = null;
		String drawStr = null;
		String texMemStr = null;
		if (showGLFps) {
			fpsStr = String.format("GL>%.1f/%dfps %d%%", sGLFps, Math.round(rrate), Math.round(srate));
			drawStr = String.format("Draw[%d]:%.1f/%.1f/%.1f", sGLLayerMode, sGLDrawAvg, sGLDrawMax, sGLDrawMin);
			texMemStr = String.format("Tex:%.1f=%.1f+%.1f+%.0f", sGLBaseLayerTexMem
					+ sGLLayerTexMem + sGLGbMem, sGLBaseLayerTexMem, sGLLayerTexMem, sGLGbMem);
		} else {
			fpsStr = String.format("%s%.1f/%dfps %d%%", version, sOverallFps, 
					Math.round(rrate), Math.round(srate));
		}

		final int fpsWidth = 220;
		final int fpsHeight = 30 + (drawStr != null ? 30 : 0) + (texMemStr != null ? 30 : 0)
				+ (sMemInfo != null ? (ENABLE_MEMORY_VERBOSE ? 120 : 30) : 0)
				+ (ENABLE_NETWORK_INFO && t1Str != null ? 30 : 0);

		canvas.save();
		canvas.translate(scrollX, scrollY + height - fpsHeight);
		canvas.drawRect(width - fpsWidth, 0, width, fpsHeight, sFpsBgPaint);
		canvas.drawText(fpsStr, width - 2, 22, sFpsTxtPaint);

		if (drawStr != null) {
			canvas.translate(0, 30);
			canvas.drawText(drawStr, width - 2, 22, sFpsTxtPaint);
		}

		if (texMemStr != null) {
			canvas.translate(0, 30);
			canvas.drawText(texMemStr, width - 2, 22, sFpsTxtPaint);
		}

		if (sMemInfo != null && sActMemInfo != null) {
			final Debug.MemoryInfo mi = sMemInfo;
			final ActivityManager.MemoryInfo ami = sActMemInfo;
			String str;
			if (ENABLE_MEMORY_VERBOSE) {
				str = String.format("D Mem:%.1f/%.1f/%.1f",
						mi.dalvikPss / 1024.0f, mi.dalvikSharedDirty / 1024.0f,
						mi.dalvikPrivateDirty / 1024.0f);
				canvas.translate(0, 30);
				canvas.drawText(str, width - 2, 22, sFpsTxtPaint);
			}
			if (ENABLE_MEMORY_VERBOSE) {
				str = String.format("N Mem:%.1f/%.1f/%.1f",
						mi.nativePss / 1024.0f, mi.nativeSharedDirty / 1024.0f,
						mi.nativePrivateDirty / 1024.0f);
				canvas.translate(0, 30);
				canvas.drawText(str, width - 2, 22, sFpsTxtPaint);
			}
			if (ENABLE_MEMORY_VERBOSE) {
				str = String.format("O Mem:%.1f/%.1f/%.1f",
						mi.otherPss / 1024.0f, mi.otherSharedDirty / 1024.0f,
						mi.otherPrivateDirty / 1024.0f);
				canvas.translate(0, 30);
				canvas.drawText(str, width - 2, 22, sFpsTxtPaint);
			}
			final float mb = 1024.0f * 1024.0f;
			str = String.format("Mem:%.1f[%.0f/%.0f]",
					mi.getTotalPss() / 1024.0f,
					ami.availMem / mb, ami.threshold / mb);
			canvas.translate(0, 30);
			canvas.drawText(str, width - 2, 22, ami.lowMemory ? sFpsWarningTxtPaint : sFpsTxtPaint);
		}
		if (ENABLE_NETWORK_INFO && t1Str != null) {
			canvas.translate(0, 30);
			canvas.drawText(t1Str, width - 2, 22, sFpsTxtPaint);
		}
		canvas.restore();		
	}
		
	public static void enableDebug() {
//		try {
//			int enableFps = DebugHelper.invokeSystemPropertiesGetInt(WebViewFPS.FPS, -1);
//			int enableFpsEx = DebugHelper.invokeSystemPropertiesGetInt(WebViewFPS.FPS_EX, -1);
//			if (enableFps == 1 || enableFpsEx == 1) {
//				WebViewFPS.ENABLE = true;
//				WebViewFPS.ENABLE_GL = true;
//				WebViewFPS.ENABLE_MEMORY_INFO = enableFpsEx == 1;
//				WebViewFPS.ENABLE_NETWORK_INFO = enableFpsEx == 1;
//				if (enableFpsEx == 1) {	
//					WebViewFPS.ENABLE_GL_EX = true;
//				}
//			} else if (enableFps == 0 && enableFpsEx == 0) {
//				WebViewFPS.ENABLE = false;
//				WebViewFPS.ENABLE_GL = false;
//				WebViewFPS.ENABLE_GL_EX = false;			
//				WebViewFPS.ENABLE_MEMORY_INFO = false;				
//			}
//	
//			int enableFpsCanvas = DebugHelper.invokeSystemPropertiesGetInt(WebViewFPS.FPS_CANVAS, -1);
//			if (enableFpsCanvas == 1) {
//				WebViewFPS.ENABLE_GL_CANVAS = true;
//			} else if (enableFpsCanvas == 0) {
//				WebViewFPS.ENABLE_GL_CANVAS = false;			
//			}
//		} catch (Throwable t) {
//		}
	}
	
	public static void enableFps(boolean enable) {
		ENABLE = enable;
		ENABLE_GL = enable;
		ENABLE_MEMORY_INFO = enable;
		ENABLE_MEMORY_VERBOSE = enable;
		enableDebug();
	}
}
