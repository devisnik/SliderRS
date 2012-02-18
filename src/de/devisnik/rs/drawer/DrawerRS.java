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
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.renderscript.Allocation;
import android.renderscript.Float2;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramStore;
import android.renderscript.RenderScript;
import android.renderscript.RenderScriptGL;
import de.devisnik.sliding.FrameFactory;
import de.devisnik.sliding.IFrameListener;
import de.devisnik.sliding.IHole;
import de.devisnik.sliding.IPiece;
import de.devisnik.sliding.IRobotFrame;
import de.devisnik.sliding.Point;
import de.devisnik.sliding.ShiftingEvent;

public class DrawerRS {
	private int mTileSizeX = 150;
	private int mTileSizeY = 150;
	private Resources mRes;
	private RenderScriptGL mRS;

	private ScriptC_drawer mScript;
	private NumberPieceDrawer mNumberPieceDrawer;
	private IRobotFrame mFrame;
	private ScriptField_Tile mTiles;

	public DrawerRS() {
		mNumberPieceDrawer = new NumberPieceDrawer(200, 200);
	}

	public void init(RenderScriptGL rs, Resources res) {
		mRS = rs;
		mRes = res;
		mFrame = FrameFactory.createRobot(5, 5, new ARandom());
		mFrame.scramble();
		initRS();
		mFrame.addListener(new IFrameListener() {

			@Override
			public void handleSwap(IPiece left, IPiece right) {
				updateTile(left);
				updateTile(right);
			}

			@Override
			public void handleShifting(ShiftingEvent[] arg0) {
				// TODO Auto-generated method stub

			}
		});
	}

	ProgramStore BLEND_ADD_DEPTH_NONE(RenderScript rs) {
		ProgramStore.Builder builder = new ProgramStore.Builder(rs);
		builder.setDepthFunc(ProgramStore.DepthFunc.ALWAYS);
		builder.setBlendFunc(ProgramStore.BlendSrcFunc.ONE,
				ProgramStore.BlendDstFunc.ONE);
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

	private Allocation loadTexture(Bitmap bitmap) {
		final Allocation allocation = Allocation.createFromBitmap(mRS, bitmap,
				Allocation.MipmapControl.MIPMAP_NONE,
				Allocation.USAGE_GRAPHICS_TEXTURE);
		return allocation;
	}

	private ProgramFragmentFixedFunction createProgramFragment() {
		ProgramFragmentFixedFunction.Builder texBuilder = new ProgramFragmentFixedFunction.Builder(
				mRS);
		texBuilder.setTexture(
				ProgramFragmentFixedFunction.Builder.EnvMode.REPLACE,
				ProgramFragmentFixedFunction.Builder.Format.RGBA, 0);
		return texBuilder.create();
	}

	private Float2 createInt2(int x, int y) {
		return new Float2(x, y);
	}

	private void initTile(ScriptField_Tile tiles, int index, int posX,
			int posY, int width, int height, Bitmap bitmap) {
		tiles.set_position(index, createInt2(posX, posY), true);
		tiles.set_destination(index, createInt2(posX, posY), true);
		tiles.set_size(index, createInt2(width, height), true);
		tiles.set_texture(index, loadTexture(bitmap), true);
	}

	private Bitmap createTile(IPiece piece) {
		Bitmap bitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		if (piece instanceof IHole)
			return bitmap;
		mNumberPieceDrawer.drawTile(piece, canvas);
		return bitmap;
	}

	public boolean replayNextMove() {
		boolean replayed = mFrame.replayNext();
		// for (IPiece piece : mFrame)
		// updateTile(piece);
		return replayed;
	}

	public void handleClick() {
		if (mFrame.isResolved())
			mFrame.scramble();
		else
			mFrame.resolve();
		for (IPiece piece : mFrame)
			updateTile(piece);
	}

	private void updateTile(IPiece piece) {
		Point size = mFrame.getSize();
		Point homePosition = piece.getHomePosition();
		int number = homePosition.y * size.x + homePosition.x;
		Point position = piece.getPosition();
		mTiles.set_destination(number,
				createInt2(mTileSizeX * position.x, mTileSizeY * position.y),
				true);
		mTiles.set_steps(number, 50, true);
	}

	private void initTiles() {
		Point size = mFrame.getSize();
		mTiles = new ScriptField_Tile(mRS, size.x * size.y);
		for (IPiece piece : mFrame) {
			Point homePosition = piece.getHomePosition();
			int number = homePosition.y * size.x + homePosition.x;
			Point position = piece.getPosition();
			initTile(mTiles, number, mTileSizeX * position.x, mTileSizeY
					* position.y, mTileSizeX, mTileSizeY, createTile(piece));
		}
		mScript.bind_tiles(mTiles);
	}

	public void setSize(int w, int h) {
		mTileSizeX = w / 5;
		mTileSizeY = h / 5;
		Point size = mFrame.getSize();
		for (IPiece piece : mFrame) {
			Point homePosition = piece.getHomePosition();
			int number = homePosition.y * size.x + homePosition.x;
			mTiles.set_size(number, createInt2(mTileSizeX, mTileSizeY), true);
		}
	}
}
