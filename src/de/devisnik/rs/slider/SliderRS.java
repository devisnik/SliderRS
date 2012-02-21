package de.devisnik.rs.slider;

import java.util.concurrent.TimeUnit;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.renderscript.Allocation;
import android.renderscript.Float2;
import android.renderscript.Matrix4f;
import android.renderscript.ProgramFragmentFixedFunction;
import android.renderscript.ProgramStore;
import android.renderscript.ProgramVertex;
import android.renderscript.ProgramVertexFixedFunction;
import android.renderscript.ProgramVertexFixedFunction.Constants;
import android.renderscript.RenderScript;
import android.renderscript.ScriptC;

import com.android.wallpaper.RenderScriptScene;

import de.devisnik.rs.slider.ScriptField_Tile.Item;
import de.devisnik.sliding.FrameFactory;
import de.devisnik.sliding.IFrameListener;
import de.devisnik.sliding.IHole;
import de.devisnik.sliding.IPiece;
import de.devisnik.sliding.IRobotFrame;
import de.devisnik.sliding.Point;
import de.devisnik.sliding.ShiftingEvent;

public class SliderRS extends RenderScriptScene {
	private static final int SINGLE_SHIFT_ANIMATION_FRAMES = 20;
	private static final int ALL_SHIFT_ANIMATION_FRAMES = 60;

	private IRobotFrame mFrame;
	private ScriptField_Tile mTiles;

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
		}
	};
	private Constants mPvOrthoAlloc;

	private final class SolverRunnable implements Runnable {
		@Override
		public void run() {
			if (replayNextMove())
				mHandler.postDelayed(this, TimeUnit.MILLISECONDS.toMillis(500));
			else
				((ScriptC_slider) mScript).set_gSolving(0);
		}
	}

	public SliderRS(int width, int height) {
		super(width, height);
		initModel(width, height);
	}

	@Override
	public void resize(int width, int height) {
		super.resize(width, height);
		stopReplay();
		if (mFrame != null)
			mFrame.resolve();
		initModel(width, height);

		Point size = mFrame.getSize();
		Matrix4f proj = new Matrix4f();
		proj.loadOrthoWindow(size.x, size.y);
		mPvOrthoAlloc.setProjection(proj);

		mTiles = initTiles();
		((ScriptC_slider) mScript).bind_tiles(mTiles);
		startReplay();
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
			int posY, Bitmap bitmap, boolean isHole) {
		Item item = new ScriptField_Tile.Item();
		item.position = createInt2(posX, posY);
		item.destination = createInt2(posX, posY);
		item.texture = loadTexture(bitmap);
		item.hole = isHole ? 1 : 0;
		tiles.set(item, index, true);
//		tiles.set_position(index, createInt2(posX, posY), true);
//		tiles.set_destination(index, createInt2(posX, posY), true);
//		tiles.set_texture(index, loadTexture(bitmap), true);
//		tiles.set_hole(index, isHole ? 1 : 0, true);
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
				createInt2(position.x, position.y),
				true);
		mTiles.set_steps(number, frames, true);
		((ScriptC_slider) mScript).set_gSolving(mFrame.isResolved() ? 0 : 1);
	}

	private ScriptField_Tile initTiles() {
		Point size = mFrame.getSize();
		TileImageProvider provider = new TileImageProvider(mWidth, mHeight, size.x, size.y, mWidth < mHeight, mResources);

		ScriptField_Tile scriptField_Tile = new ScriptField_Tile(mRS, size.x
				* size.y);
		for (IPiece piece : mFrame) {
			Point homePosition = piece.getHomePosition();
			int number = homePosition.y * size.x + homePosition.x;
			Point position = piece.getPosition();
			initTile(scriptField_Tile, number, position.x,
					position.y,
					provider.getImage(piece), piece instanceof IHole);
		}
		return scriptField_Tile;
	}

	@Override
	protected ScriptC createScript() {

		ScriptC_slider scriptC_drawer = new ScriptC_slider(mRS, mResources,
				R.raw.slider);

		ProgramVertexFixedFunction.Builder builder = new ProgramVertexFixedFunction.Builder(
				mRS);
		ProgramVertex pvbo = builder.create();
		mPvOrthoAlloc = new ProgramVertexFixedFunction.Constants(mRS);
		((ProgramVertexFixedFunction) pvbo).bindConstants(mPvOrthoAlloc);
		Matrix4f proj = new Matrix4f();
		Point size = mFrame.getSize();
		proj.loadOrthoWindow(size.x, size.y);
		mPvOrthoAlloc.setProjection(proj);
		mRS.bindProgramVertex(pvbo);

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
		startReplay();
	}

	private void startReplay() {
		mFrame.addListener(mFrameListener);
		mHandler.postDelayed(mSolverRunnable, TimeUnit.SECONDS.toMillis(1));
	}

	@Override
	public void stop() {
		stopReplay();
		super.stop();
	}

	private void stopReplay() {
		mHandler.removeCallbacks(mSolverRunnable);
		mFrame.removeListener(mFrameListener);
	}
}
