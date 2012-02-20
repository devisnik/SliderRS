package de.devisnik.rs.slider;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import de.devisnik.sliding.IPiece;
import de.devisnik.sliding.Point;

public class ImagePieceDrawer {

	private static final int BORDER = 0;
	private Bitmap itsImage;
	private Paint itsImagePaint;
	private Rect itsDstRect;
	private Rect itsScrRect;
	private final Point itsTileSize;
	private Point itsHomeUpperLeft;

	public ImagePieceDrawer(Bitmap image, Point tileSize) {
		itsImage = image;
		itsTileSize = tileSize;
		itsImagePaint = new Paint();
		itsImagePaint.setAntiAlias(true);
		itsDstRect = new Rect(0, 0, tileSize.x - BORDER, tileSize.y - BORDER);
		itsScrRect = new Rect();
		itsHomeUpperLeft = new Point(0, 0);
	}

	public void drawTile(IPiece piece, Canvas canvas, Paint paint) {
		itsHomeUpperLeft.set(itsTileSize).multiplyBy(piece.getHomePosition());
		itsScrRect.set(itsHomeUpperLeft.x, itsHomeUpperLeft.y, itsHomeUpperLeft.x
				+ itsTileSize.x - BORDER, itsHomeUpperLeft.y + itsTileSize.y - BORDER);
		canvas.drawBitmap(itsImage, itsScrRect, itsDstRect, paint);
	}
}
