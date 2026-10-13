package silicon.px.mesh;

/**
 * Describes how a face's texture sits on the block, relative to the plain cube_all layout.
 * 0-3 are quarter turns, +4 mirrors u afterwards. terrain.fsh has the same maths, keep them in sync.
 */
public final class UvTransform {
	private static final float EPS = 1e-3f;

	private UvTransform() {
	}

	public static float[] standard(int face, float x, float y, float z) {
		return switch (face) {
			case Faces.DOWN -> new float[]{x, 1 - z};
			case Faces.UP -> new float[]{x, z};
			case Faces.NORTH -> new float[]{1 - x, 1 - y};
			case Faces.SOUTH -> new float[]{x, 1 - y};
			case Faces.WEST -> new float[]{z, 1 - y};
			default -> new float[]{1 - z, 1 - y};
		};
	}

	public static float[] apply(int t, float u, float v) {
		for (int i = 0; i < (t & 3); i++) {
			float tmp = u;
			u = 1 - v;
			v = tmp;
		}
		if (t >= 4)
			u = 1 - u;
		return new float[]{u, v};
	}

	/**
	 * @param pos 4 vertices as x,y,z triples in block space (0..1)
	 * @param uv  4 vertices as u,v pairs in sprite space (0..1)
	 * @return the transform, or -1 if the face isn't laid out as any of them
	 */
	public static int detect(int face, float[] pos, float[] uv) {
		outer:
		for (int t = 0; t < 8; t++) {
			for (int i = 0; i < 4; i++) {
				float[] s = standard(face, pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]);
				float[] r = apply(t, s[0], s[1]);
				if (Math.abs(r[0] - uv[i * 2]) > EPS || Math.abs(r[1] - uv[i * 2 + 1]) > EPS)
					continue outer;
			}
			return t;
		}
		return -1;
	}
}
