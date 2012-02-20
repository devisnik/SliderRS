package de.devisnik.rs.drawer;

import com.android.wallpaper.RenderScriptWallpaper;

public class SliderWallpaper extends RenderScriptWallpaper<SliderRS> {

	@Override
	protected SliderRS createScene(int width, int height) {
		return new SliderRS(width, height);
	}

}
