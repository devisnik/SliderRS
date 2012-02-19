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

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Float2;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramStore;
import android.renderscript.RenderScript;
import android.renderscript.RenderScriptGL;
import android.renderscript.ScriptC;

import com.android.wallpaper.RenderScriptScene;

import de.devisnik.sliding.FrameFactory;
import de.devisnik.sliding.IFrameListener;
import de.devisnik.sliding.IHole;
import de.devisnik.sliding.IPiece;
import de.devisnik.sliding.IRobotFrame;
import de.devisnik.sliding.Point;
import de.devisnik.sliding.ShiftingEvent;

public class WallDrawerRS extends RenderScriptScene {
	private static final int SINGLE_SHIFT_ANIMATION_FRAMES = 30;
	private static final int ALL_SHIFT_ANIMATION_FRAMES = 100;
	private int mTileSizeX = 150;
	private int mTileSizeY = 150;

	private NumberPieceDrawer mNumberPieceDrawer;
	private IRobotFrame mFrame;
	private ScriptField_Tile mTiles;
	private ImagePieceDrawer mImagePieceDrawer;

	private Handler mHandler = new Handler();
	private SolverRunnable mSolverRunnable;
	private IFrameListener mFrameListener = new IFrameListener() {

		@Override
		public void handleSwap(IPiece left, IPiece right) {
			updateTile(left, SINGLE_SHIFT_ANIMATION_FRAMES);
			updateTile(right, SINGLE_SHIFT_ANIMATION_FRAMES);
		}

		@Override
		public void handleShifting(ShiftingEvent[] arg0) {
			// TODO Auto-generated method stub

		}
	};

	private final class SolverRunnable implements Runnable {
		@Override
		public void run() {
			if (replayNextMove())
				mHandler.postDelayed(this, TimeUnit.MILLISECONDS.toMillis(500));
			else 
				((ScriptC_drawer)mScript).set_gSolving(0);
		}
	}

	public WallDrawerRS(int width, int height) {
		super(width, height);
		initModel(width, height);
		computeTileSize(width, height);
	}

	private void computeTileSize(int width, int height) {
		Point size = mFrame.getSize();
		mTileSizeX = width / size.x;
		mTileSizeY = height / size.y;
	}
	
	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		if (mFrame != null)
			mFrame.resolve();
		initModel(width, height);
		computeTileSize(width, height);
		mTiles = initTiles();
		((ScriptC_drawer)mScript).bind_tiles(mTiles);
	}


	private void initModel(int width, int height) {
		int shorter = 4;
		int longer = shorter;
		if (width > height)
			longer = Math.round(((float) width / height) * shorter);
		else
			longer = Math.round(((float) height / width) * shorter);
		mFrame = FrameFactory.createRobot(width > height ? longer : shorter,
				width > height ? shorter : longer, new ARandom());
		mFrame.scramble();
	}

	private Bitmap createTargetBitmap(Bitmap bitmap) {
		if (bitmap == null)
			return null;
		Config bitmapConfig = bitmap.getConfig();
		Bitmap output = Bitmap.createBitmap(mWidth, mHeight,
				bitmapConfig == null ? Config.ARGB_8888 : bitmapConfig);
		Canvas canvas = new Canvas(output);
		Paint paint = new Paint();
		Rect clipRect = computeClipRectangle(bitmap);
		Rect targetRect = new Rect(0, 0, mWidth, mHeight);
		paint.setAntiAlias(true);
		// paint.setXfermode(new PorterDuffXfermode(Mode.SRC_IN));
		canvas.drawBitmap(bitmap, clipRect, targetRect, paint);
		return output;
	}

	private Rect computeClipRectangle(Bitmap bitmap) {
		float targetRatio = ((float) mWidth) / mHeight;
		Point bitmapArea = new Point(bitmap.getWidth(), bitmap.getHeight());
		float bitmapRatio = bitmapArea.ratio();
		Point clipArea = bitmapArea.copy();
		if (targetRatio <= bitmapRatio)
			clipArea.x = Math.round(targetRatio * clipArea.y);
		else
			clipArea.y = Math.round(clipArea.x / targetRatio);
		Point shift = Point.diff(bitmapArea, clipArea).divideBy(2);
		return new Rect(shift.x, shift.y, clipArea.x + shift.x, clipArea.y
				+ shift.y);
	}

	private ProgramStore BLEND_ADD_DEPTH_NONE(RenderScript rs) {
		ProgramStore.Builder builder = new ProgramStore.Builder(rs);
		builder.setDepthFunc(ProgramStore.DepthFunc.ALWAYS);
		builder.setBlendFunc(ProgramStore.BlendSrcFunc.ONE,
				ProgramStore.BlendDstFunc.ONE);
		builder.setDitherEnabled(false);
		builder.setDepthMaskEnabled(false);
		return builder.create();
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
			int posY, int width, int height, Bitmap bitmap, boolean isHole) {
		tiles.set_position(index, createInt2(posX, posY), true);
		tiles.set_destination(index, createInt2(posX, posY), true);
		tiles.set_size(index, createInt2(width, height), true);
		tiles.set_texture(index, loadTexture(bitmap), true);
		tiles.set_hole(index, isHole ? 1 : 0, true);
	}

	private Bitmap createTile(IPiece piece) {
		Bitmap bitmap = Bitmap.createBitmap(mTileSizeX, mTileSizeY,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		mImagePieceDrawer.drawTile(piece, canvas, null);
		// mNumberPieceDrawer.drawTile(piece, canvas);
		return bitmap;
	}

	public boolean replayNextMove() {
		return mFrame.replayNext();
	}

	public void handleClick() {
		if (mFrame.isResolved())
			mFrame.scramble();
		else
			mFrame.resolve();
		for (IPiece piece : mFrame)
			updateTile(piece, ALL_SHIFT_ANIMATION_FRAMES);
		mHandler.removeCallbacks(mSolverRunnable);
		mHandler.postDelayed(mSolverRunnable, TimeUnit.SECONDS.toMillis(3));

	}

	@Override
	public Bundle onCommand(String action, int x, int y, int z, Bundle extras,
			boolean resultRequested) {
		handleClick();
		return super.onCommand(action, x, y, z, extras, resultRequested);
	}
	
	private void updateTile(IPiece piece, int frames) {
		Point size = mFrame.getSize();
		Point homePosition = piece.getHomePosition();
		int number = homePosition.y * size.x + homePosition.x;
		Point position = piece.getPosition();
		mTiles.set_destination(number,
				createInt2(mTileSizeX * position.x, mTileSizeY * position.y),
				true);
		mTiles.set_steps(number, frames, true);
		((ScriptC_drawer) mScript).set_gSolving(mFrame.isResolved() ? 0 : 1);
	}

	private ScriptField_Tile initTiles() {
		mNumberPieceDrawer = new NumberPieceDrawer(mTileSizeX, mTileSizeY);
		Bitmap originalImage = BitmapFactory.decodeResource(mResources,
				R.drawable.coast);
		mImagePieceDrawer = new ImagePieceDrawer(
				createTargetBitmap(originalImage), new Point(mTileSizeX,
						mTileSizeY));

		Point size = mFrame.getSize();
		ScriptField_Tile scriptField_Tile = new ScriptField_Tile(mRS, size.x * size.y);
		for (IPiece piece : mFrame) {
			Point homePosition = piece.getHomePosition();
			int number = homePosition.y * size.x + homePosition.x;
			Point position = piece.getPosition();
			initTile(scriptField_Tile, number, mTileSizeX * position.x, mTileSizeY
					* position.y, mTileSizeX, mTileSizeY, createTile(piece),
					piece instanceof IHole);
		}
		return scriptField_Tile;
	}

	@Override
	protected ScriptC createScript() {

		ScriptC_drawer scriptC_drawer = new ScriptC_drawer(mRS, mResources,
				R.raw.walldrawer);
		scriptC_drawer.set_gProgramFragment(createProgramFragment());
		mTiles = initTiles();
		scriptC_drawer.bind_tiles(mTiles);
		mRS.bindProgramStore(BLEND_ADD_DEPTH_NONE(mRS));
		mSolverRunnable = new SolverRunnable();
		return scriptC_drawer;
	}

	@Override
	public void start() {
		super.start();
		mFrame.addListener(mFrameListener);
		mHandler.postDelayed(mSolverRunnable, TimeUnit.SECONDS.toMillis(1));
	}

	@Override
	public void stop() {
		mHandler.removeCallbacks(mSolverRunnable);
		mFrame.removeListener(mFrameListener);
		super.stop();
	}
}
