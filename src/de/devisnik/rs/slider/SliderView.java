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

package de.devisnik.rs.slider;

import android.content.Context;
import android.renderscript.RSSurfaceView;
import android.renderscript.RenderScriptGL;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

public class SliderView extends RSSurfaceView {
	
    private RenderScriptGL mRS;
    private SliderRS mRender;
	
    public SliderView(Context context) {
        super(context);
        ensureRenderScript();
    }

    private void ensureRenderScript() {
        if (mRS == null) {
            RenderScriptGL.SurfaceConfig sc = new RenderScriptGL.SurfaceConfig();
            mRS = createRenderScriptGL(sc);
        }
    }
    
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
    	super.surfaceChanged(holder, format, w, h);
    	if (mRender != null)
    		mRender.stop();
    	mRender = new SliderRS(w, h);
    	mRender.init(mRS, getResources(), false);
    	mRender.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        ensureRenderScript();
    }

    @Override
    protected void onDetachedFromWindow() {
        // Handle the system event and clean up
    	mRender.stop();
    	mRender = null;
        if (mRS != null) {
            mRS = null;
            destroyRenderScriptGL();
        }
        super.onDetachedFromWindow();
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Pass touch events from the system to the rendering script
        int action = ev.getAction();
		if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_MOVE) {
            return true;
        }
		if (action == MotionEvent.ACTION_UP) {
			mRender.handleClick();
			return true;
		}

        return false;
    }
}


