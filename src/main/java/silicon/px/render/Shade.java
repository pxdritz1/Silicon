package silicon.px.render;

/**
 * Rounds a corner's colour and light into buckets, so faces that differ by an invisible step still merge.
 * Merged quads keep each corner's real value, so any error stays within one bucket.
 * A step of 1 keeps everything exact.
 */
final class Shade {
	private Shade() {
	}

	static int color(int argb, int step) {
		if (step <= 1)
			return argb;
		int out = 0;
		for (int shift = 0; shift < 32; shift += 8)
			out |= (((argb >>> shift) & 0xFF) / step) << shift;
		return out;
	}

	// the lightmap packs block light and sky light as two 16 bit halves
	static int light(int packed, int step) {
		if (step <= 1)
			return packed;
		return ((packed & 0xFFFF) / step) | ((((packed >>> 16) & 0xFFFF) / step) << 16);
	}

	static int ao(float ao, int step) {
		return Math.round(ao * 255) / Math.max(step, 1);
	}
}
