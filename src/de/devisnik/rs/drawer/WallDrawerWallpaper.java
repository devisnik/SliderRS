package de.devisnik.rs.drawer;

import com.android.wallpaper.RenderScriptWallpaper;

public class WallDrawerWallpaper extends RenderScriptWallpaper<WallDrawerRS> {

	@Override
	protected WallDrawerRS createScene(int width, int height) {
		return new WallDrawerRS(width, height);
	}

}
