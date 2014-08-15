/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.uc.webkit.impl;

import com.uc.webkit.helper.CanvasHelper;
import com.uc.webkit.helper.ViewRootHelper;
import android.graphics.Canvas;
import android.util.Log;

import org.chromium.content.common.CleanupReference;

// Simple Java abstraction and wrapper for the native DrawGLFunctor flow.
// An instance of this class can be constructed, bound to a single view context (i.e. AwContennts)
// and then drawn and detached from the view tree any number of times (using requestDrawGL and
// detach respectively). Then when finished with, it can be explicitly released by calling
// destroy() or will clean itself up as required via finalizer / CleanupReference.
public class DrawGLFunctor {

    private static final String TAG = DrawGLFunctor.class.getSimpleName();

    // NOTE: The STATUS_* values *must* match the enum in DrawGlInfo.h

    /**
     * Indicates that the display list is done drawing.
     * 
     * @see HardwareCanvas#drawDisplayList(DisplayList, android.graphics.Rect, int)
     *
     * @hide
     */
    public static final int STATUS_DONE = 0x0;

    /**
     * Indicates that the display list needs another drawing pass.
     * 
     * @see HardwareCanvas#drawDisplayList(DisplayList, android.graphics.Rect, int)
     *
     * @hide
     */
    public static final int STATUS_DRAW = 0x1;

    /**
     * Indicates that the display list needs to re-execute its GL functors.
     * 
     * @see HardwareCanvas#drawDisplayList(DisplayList, android.graphics.Rect, int) 
     * @see HardwareCanvas#callDrawGLFunction(int)
     *
     * @hide
     */
    public static final int STATUS_INVOKE = 0x2;

    /**
     * Indicates that the display list performed GL drawing operations.
     *
     * @see HardwareCanvas#drawDisplayList(DisplayList, android.graphics.Rect, int)
     *
     * @hide
     */
    public static final int STATUS_DREW = 0x4;
    
    // Pointer to native side instance
    private CleanupReference mCleanupReference;
    private DestroyRunnable mDestroyRunnable;

    public DrawGLFunctor(int viewContext) {
        mDestroyRunnable = new DestroyRunnable(nativeCreateGLFunctor(viewContext));
        mCleanupReference = new CleanupReference(this, mDestroyRunnable);
    }

    public void destroy() {
        if (mCleanupReference != null) {
            mCleanupReference.cleanupNow();
            mCleanupReference = null;
            mDestroyRunnable = null;
        }
    }

    public void detach() {
        mDestroyRunnable.detachNativeFunctor();
    }

    public boolean requestDrawGL(Canvas canvas, Object viewRootImpl) {
        if (mDestroyRunnable.mNativeDrawGLFunctor == 0) {
            throw new RuntimeException("requested DrawGL on already destroyed DrawGLFunctor");
        }
        mDestroyRunnable.mViewRootImpl = viewRootImpl;
        if (canvas != null) {
            // int ret = canvas.callDrawGLFunction(mDestroyRunnable.mNativeDrawGLFunctor);
            int ret = CanvasHelper.callDrawGLFunction(canvas, mDestroyRunnable.mNativeDrawGLFunctor);
            if (ret != STATUS_DONE) {
                Log.e(TAG, "callDrawGLFunction error: " + ret);
                return false;
            }
        } else {
            // viewRootImpl.attachFunctor(mDestroyRunnable.mNativeDrawGLFunctor);
            ViewRootHelper.attachFunctor(viewRootImpl, mDestroyRunnable.mNativeDrawGLFunctor);
        }
        return true;
    }

    public static void setChromiumAwDrawGLFunction(int functionPointer) {
        nativeSetChromiumAwDrawGLFunction(functionPointer);
    }

    // Holds the core resources of the class, everything required to correctly cleanup.
    // IMPORTANT: this class must not hold any reference back to the outer DrawGLFunctor
    // instance, as that will defeat GC of that object.
    private static final class DestroyRunnable implements Runnable {
        // ViewRootImpl mViewRootImpl;
        Object mViewRootImpl;
        int mNativeDrawGLFunctor;
        DestroyRunnable(int nativeDrawGLFunctor) {
            mNativeDrawGLFunctor = nativeDrawGLFunctor;
        }

        // Called when the outer DrawGLFunctor instance has been GC'ed, i.e this is its finalizer.
        @Override
        public void run() {
            detachNativeFunctor();
            nativeDestroyGLFunctor(mNativeDrawGLFunctor);
            mNativeDrawGLFunctor = 0;
        }

        void detachNativeFunctor() {
            if (mNativeDrawGLFunctor != 0 && mViewRootImpl != null) {
                // mViewRootImpl.detachFunctor(mNativeDrawGLFunctor);
                ViewRootHelper.detachFunctor(mViewRootImpl, mNativeDrawGLFunctor);
            }
            mViewRootImpl = null;
        }
    }

    private static native int nativeCreateGLFunctor(int viewContext);
    private static native void nativeDestroyGLFunctor(int functor);
    private static native void nativeSetChromiumAwDrawGLFunction(int functionPointer);
}
