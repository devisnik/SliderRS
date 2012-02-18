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

import android.content.res.Resources;
import android.renderscript.Allocation;
import android.renderscript.Int2;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramStore;
import android.renderscript.RenderScript;
import android.renderscript.RenderScriptGL;

public class DrawerRS {
	private Resources mRes;
	private RenderScriptGL mRS;

	private ScriptC_drawer mScript;

	public DrawerRS() {
	}

	// This provides us with the renderscript context and resources that
	// allow us to create the script that does rendering
	public void init(RenderScriptGL rs, Resources res) {
		mRS = rs;
		mRes = res;
		initRS();
	}
	
    ProgramStore BLEND_ADD_DEPTH_NONE(RenderScript rs) {
        ProgramStore.Builder builder = new ProgramStore.Builder(rs);
        builder.setDepthFunc(ProgramStore.DepthFunc.ALWAYS);
        builder.setBlendFunc(ProgramStore.BlendSrcFunc.ONE, ProgramStore.BlendDstFunc.ONE);
        builder.setDitherEnabled(false);
        builder.setDepthMaskEnabled(false);
        return builder.create();
    }

	public void onActionDown(int x, int y) {
		mScript.set_gTouchX(x);
		mScript.set_gTouchY(y);
	}

	private void initRS() {
		mScript = new ScriptC_drawer(mRS, mRes, R.raw.drawer);
		initTiles();
		mScript.set_gProgramFragment(createProgramFragment());
		mRS.bindProgramStore(BLEND_ADD_DEPTH_NONE(mRS));
		mRS.bindRootScript(mScript);
	}

    private Allocation loadTexture(int id) {
        final Allocation allocation =
            Allocation.createFromBitmapResource(mRS, mRes,
                id, Allocation.MipmapControl.MIPMAP_NONE,
                Allocation.USAGE_GRAPHICS_TEXTURE);
        return allocation;
    }
    
    private ProgramFragmentFixedFunction createProgramFragment() {
    	ProgramFragmentFixedFunction.Builder texBuilder = new ProgramFragmentFixedFunction.Builder(mRS);
    	texBuilder.setTexture(ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
    			ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
    	return texBuilder.create();
    }

    private Int2 createInt2(int x, int y) {
        Int2 int2 = new Int2();
        int2.x = x;
        int2.y = y;
        return int2;
    }
    
    private void initTile(ScriptField_Tile tiles, int index, int posX, int posY, int width, int height, int imageResourceId) {
        tiles.set_position(index, createInt2(posX, posY),  true);
        tiles.set_size(index, createInt2(width, height), true);
        tiles.set_texture(index, loadTexture(imageResourceId), true);    	
    }
    
    private void initTiles() {        
        ScriptField_Tile tiles = new ScriptField_Tile(mRS, 3);
        initTile(tiles, 0, 0, 0, 100, 100, R.drawable.data);
        initTile(tiles, 1, 300, 0, 200, 200, R.drawable.leaf);
        initTile(tiles, 2, 600, 0, 300, 500, R.drawable.torusmap);
        mScript.bind_tiles(tiles);
    }
}
