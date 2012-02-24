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
	private static final int FRAME_DIMENSION_SHORT_EDGE = 3;
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
		((ScriptC_slider) mScript).set_gSize(new Float2(size.x, size.y));
		startReplay();
	}

	private void initModel(int width, int height) {
		int shorter = FRAME_DIMENSION_SHORT_EDGE;
		int longer = computeLongerEdgeDimension(width, height, shorter);
		mFrame = FrameFactory.createRobot(width > height ? longer : shorter,
				width > height ? shorter : longer, new ARandom());
		mFrame.scramble();
	}

	private int computeLongerEdgeDimension(int width, int height,
			int shorterEdgeDimension) {
		int longer = shorterEdgeDimension;
		if (width > height)
			longer = Math
					.round(((float) width / height) * shorterEdgeDimension);
		else
			longer = Math
					.round(((float) height / width) * shorterEdgeDimension);
		return longer;
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

	private Float2 createInt2(Point point) {
		Point size = mFrame.getSize();
		Float2 fsize = new Float2(size.x, size.y);
		return new Float2(point.x - fsize.x / 2, point.y - fsize.y / 2);
	}

	private Item createTileItem(IPiece piece, TileImageProvider provider) {
		Point position = piece.getPosition();
		Item item = new ScriptField_Tile.Item();
		item.position = createInt2(position);
		item.destination = createInt2(position);
		item.texture = loadTexture(provider.getImage(piece));
		item.hole = piece instanceof IHole ? 1 : 0;
		return item;
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
		int number = getNumber(piece);
		mTiles.set_destination(number, createInt2(piece.getPosition()), true);
		mTiles.set_steps(number, frames, true);
		((ScriptC_slider) mScript).set_gSolving(mFrame.isResolved() ? 0 : 1);
	}

	private int getNumber(IPiece piece) {
		Point size = mFrame.getSize();
		Point homePosition = piece.getHomePosition();
		return homePosition.y * size.x + homePosition.x;
	}

	private ScriptField_Tile initTiles() {
		Point size = mFrame.getSize();
		TileImageProvider provider = new TileImageProvider(mWidth, mHeight,
				size.x, size.y, mWidth < mHeight, mResources);
		ScriptField_Tile tiles = new ScriptField_Tile(mRS, size.x * size.y);
		for (IPiece piece : mFrame)
			tiles.set(createTileItem(piece, provider), getNumber(piece), true);
		return tiles;
	}

	@Override
	protected ScriptC createScript() {

		ScriptC_slider scriptC_drawer = new ScriptC_slider(mRS, mResources,
				R.raw.slider);

		ProgramVertexFixedFunction.Builder builder = new ProgramVertexFixedFunction.Builder(
				mRS);
		ProgramVertexFixedFunction programVertex = builder.create();
		Point size = mFrame.getSize();
		mPvOrthoAlloc = new ProgramVertexFixedFunction.Constants(mRS);
		((ProgramVertexFixedFunction) programVertex)
				.bindConstants(mPvOrthoAlloc);
		Matrix4f proj = new Matrix4f();
		proj.loadOrtho(0, size.x, size.y, 0, -10f, 10f);
		mPvOrthoAlloc.setProjection(proj);
		mRS.bindProgramVertex(programVertex);

		scriptC_drawer.set_gProgramFragment(createProgramFragment());
		mTiles = initTiles();
		scriptC_drawer.bind_tiles(mTiles);
		mRS.bindProgramStore(BLEND_ADD_DEPTH_NONE(mRS));

		scriptC_drawer.set_gSize(new Float2(size.x, size.y));
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
