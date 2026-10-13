package silicon.px.mesh;

/**
 * Face numbering follows {@code Direction.ordinal()}: down, up, north, south, west, east.
 */
public final class Faces {
	public static final int DOWN = 0, UP = 1, NORTH = 2, SOUTH = 3, WEST = 4, EAST = 5;

	private static final int[] DX = {0, 0, 0, 0, -1, 1};
	private static final int[] DY = {-1, 1, 0, 0, 0, 0};
	private static final int[] DZ = {0, 0, -1, 1, 0, 0};

	private Faces() {
	}

	public static int index(int x, int y, int z) {
		return (y << 8) | (z << 4) | x;
	}

	// layer runs along the face normal, u/v span the face plane
	public static int blockIndex(int face, int layer, int u, int v) {
		return switch (face) {
			case DOWN, UP -> index(u, layer, v);
			case NORTH, SOUTH -> index(u, v, layer);
			default -> index(layer, v, u);
		};
	}

	public static int dx(int face) {
		return DX[face];
	}

	public static int dy(int face) {
		return DY[face];
	}

	public static int dz(int face) {
		return DZ[face];
	}
}
