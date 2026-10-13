package silicon.px.mesh;

/**
 * Packs what the terrain shader needs for a merged quad into the three normal bytes.
 * Vanilla terrain never reads the normal, and x == 127 with y != 0 can't come from a real unit normal.
 */
public final class Payload {
	public static final int MARK = 127;

	private Payload() {
	}

	public static int encode(int face, int transform, int log2W, int log2H) {
		int y = 1 + face + 6 * transform;
		int z = 1 + log2W + 8 * log2H;
		return (MARK << 16) | (y << 8) | z;
	}

	public static int x(int p) {
		return (p >> 16) & 0xFF;
	}

	public static int y(int p) {
		return (p >> 8) & 0xFF;
	}

	public static int z(int p) {
		return p & 0xFF;
	}

	// BufferBuilder truncates f * 127 into a byte, so aim at the middle of the bucket
	public static float normalFloat(int b) {
		return (b + 0.5f) / 127f;
	}

	public static boolean isMarked(int x, int y) {
		return x == MARK && y > 0;
	}

	public static int face(int p) {
		return (y(p) - 1) % 6;
	}

	public static int transform(int p) {
		return (y(p) - 1) / 6;
	}

	public static int log2W(int p) {
		return (z(p) - 1) & 7;
	}

	public static int log2H(int p) {
		return (z(p) - 1) >> 3;
	}
}
