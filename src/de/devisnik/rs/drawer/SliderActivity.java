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

import android.app.Activity;
import android.os.Bundle;
import android.renderscript.RSSurfaceView;

// Renderscript activity
public class SliderActivity extends Activity {

    // Custom view to use with RenderScript
    private RSSurfaceView mView;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mView = new SliderView(this);
        setContentView(mView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mView.pause();
    }

}

