package de.devisnik.rs.slider;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import de.devisnik.sliding.IPiece;
import de.devisnik.sliding.Point;

public class TileImageProvider {

	private NumberPieceDrawer mNumberPieceDrawer;
	private ImagePieceDrawer mImagePieceDrawer;
	private final int mWidth;
	private final int mHeight;
	private final boolean mNumbers;
	private int mTileSizeX;
	private int mTileSizeY;

	public TileImageProvider(int width, int height, int tilesX, int tilesY,
			boolean numbers, Resources resources) {
		mWidth = width;
		mHeight = height;
		mNumbers = numbers;
		mTileSizeX = width / tilesX;
		mTileSizeY = height / tilesY;
		mNumberPieceDrawer = new NumberPieceDrawer(mTileSizeX, mTileSizeY);
		Bitmap originalImage = BitmapFactory.decodeResource(resources,
				R.drawable.coast);
		if (!numbers)
			mImagePieceDrawer = new ImagePieceDrawer(
					createTargetBitmap(originalImage), new Point(mTileSizeX,
							mTileSizeY));
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

	private Bitmap createTile(IPiece piece) {
		Bitmap bitmap = Bitmap.createBitmap(mTileSizeX, mTileSizeY,
				Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		if (!mNumbers)
			mImagePieceDrawer.drawTile(piece, canvas, null);
		else
			mNumberPieceDrawer.drawTile(piece, canvas);
		return bitmap;
	}

	public Bitmap getImage(IPiece piece) {
		return createTile(piece);
	}
}
