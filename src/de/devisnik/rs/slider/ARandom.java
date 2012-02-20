/**
 * 
 */
package de.devisnik.rs.slider;

import java.util.Random;

import de.devisnik.sliding.IRandom;

final class ARandom implements IRandom {
	Random random = new Random();

	@Override
	public int nextInt(int border) {
		return random.nextInt(border);
	}
}