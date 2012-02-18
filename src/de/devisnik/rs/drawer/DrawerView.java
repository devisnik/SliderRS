/*
 * Copyright (C) 2011 The Android Open Source Project
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

package de.devisnik.rs.drawer;

import java.util.concurrent.TimeUnit;

import android.os.Handler;
import android.renderscript.RSSurfaceView;
import android.renderscript.RenderScriptGL;

import android.content.Context;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class DrawerView extends RSSurfaceView {
	
    private final class SolverRunnable implements Runnable {
		@Override
		public void run() {
			if (mRender.replayNextMove())
				mHandler.postDelayed(this, TimeUnit.MILLISECONDS.toMillis(500));
		}
	}

	// Renderscript context
    private RenderScriptGL mRS;
    // Script that does the rendering
    private DrawerRS mRender;
    private Handler mHandler = new Handler();
	private SolverRunnable mSolver;

    public DrawerView(Context context) {
        super(context);
        ensureRenderScript();
    }

    private void ensureRenderScript() {
        if (mRS == null) {
            // Initialize renderscript with desired surface characteristics.
            // In this case, just use the defaults
            RenderScriptGL.SurfaceConfig sc = new RenderScriptGL.SurfaceConfig();
            mRS = createRenderScriptGL(sc);
            // Create an instance of the script that does the rendering
            mRender = new DrawerRS();
            mRender.init(mRS, getResources());
            mSolver = new SolverRunnable();
        }
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    	super.surfaceChanged(holder, format, w, h);
    	mRender.setSize(w, h);
    }
    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ensureRenderScript();
		mHandler.postDelayed(mSolver, TimeUnit.SECONDS.toMillis(1));
    }

    @Override
    protected void onDetachedFromWindow() {
        // Handle the system event and clean up
    	mHandler.removeCallbacks(mSolver);
    	mRender = null;
        if (mRS != null) {
            mRS = null;
            destroyRenderScriptGL();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Pass touch events from the system to the rendering script
        int action = ev.getAction();
		if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            mRender.onActionDown((int)ev.getX(), (int)ev.getY());
            return true;
        }
		if (action == MotionEvent.ACTION_UP) {
			mRender.handleClick();
			mHandler.removeCallbacks(mSolver);
			mHandler.postDelayed(mSolver, TimeUnit.SECONDS.toMillis(1));			
			return true;
		}

        return false;
    }
}


