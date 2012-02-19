package de.devisnik.rs.drawer;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.RadialGradient;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import de.devisnik.sliding.IPiece;

public class NumberPieceDrawer {

	private static final int TEXT_COLOR = 0xff333333;

	private final Paint mPaint;
	private final Paint mBackPaint;
	private final Rect mBackRect;
	private float mTextPosX;
	private float mTextPosY;

	public NumberPieceDrawer(int tileSizeX, int tileSizeY) {
		mPaint = new Paint();
		mPaint.setColor(TEXT_COLOR);
		mPaint.setAntiAlias(true);
		mPaint.setStrokeWidth(2);
		mPaint.setStrokeCap(Paint.Cap.ROUND);
		mPaint.setStyle(Paint.Style.FILL);
		mPaint.setTextSize(tileSizeX / 4);
		mPaint.setTextAlign(Align.CENTER);

		mBackPaint = new Paint();
		mBackPaint.setShader(new RadialGradient(tileSizeX / 2, tileSizeY / 2,
				tileSizeX / 2, 0xff888800, 0x00000000, TileMode.CLAMP));

		mBackRect = new Rect(0, 0, tileSizeX, tileSizeY);
		mTextPosX = tileSizeX / 2f;
		mTextPosY = (tileSizeY - mPaint.ascent() - mPaint.descent()) / 2;
	}

	public void drawTile(IPiece piece, Canvas canvas) {
		canvas.drawRect(mBackRect, mBackPaint);
		canvas.drawText(piece.getLabel(), mTextPosX, mTextPosY, mPaint);
	}

}
