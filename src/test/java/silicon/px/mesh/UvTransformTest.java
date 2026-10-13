package silicon.px.mesh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UvTransformTest {
	// corners of each face in block space, any order works for the detector
	private static float[] corners(int face) {
		float[][] c = new float[4][];
		int[][] uv = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
		for (int i = 0; i < 4; i++) {
			int a = uv[i][0], b = uv[i][1];
			c[i] = switch (face) {
				case Faces.DOWN -> new float[]{a, 0, b};
				case Faces.UP -> new float[]{a, 1, b};
				case Faces.NORTH -> new float[]{a, b, 0};
				case Faces.SOUTH -> new float[]{a, b, 1};
				case Faces.WEST -> new float[]{0, b, a};
				default -> new float[]{1, b, a};
			};
		}
		float[] out = new float[12];
		for (int i = 0; i < 4; i++)
			System.arraycopy(c[i], 0, out, i * 3, 3);
		return out;
	}

	private static float[] uvs(int face, float[] pos, int t) {
		float[] out = new float[8];
		for (int i = 0; i < 4; i++) {
			float[] s = UvTransform.standard(face, pos[i * 3], pos[i * 3 + 1], pos[i * 3 + 2]);
			float[] r = UvTransform.apply(t, s[0], s[1]);
			out[i * 2] = r[0];
			out[i * 2 + 1] = r[1];
		}
		return out;
	}

	@Test
	void detectsEveryTransform() {
		for (int face = 0; face < 6; face++) {
			float[] pos = corners(face);
			for (int t = 0; t < 8; t++)
				assertEquals(t, UvTransform.detect(face, pos, uvs(face, pos, t)), "face " + face);
		}
	}

	@Test
	void rejectsPartialUvs() {
		float[] pos = corners(Faces.UP);
		float[] uv = uvs(Faces.UP, pos, 0);
		for (int i = 0; i < uv.length; i++)
			uv[i] *= 0.5f;
		assertEquals(-1, UvTransform.detect(Faces.UP, pos, uv));
	}

	@Test
	void fourTurnsIsIdentity() {
		float[] r = UvTransform.apply(1, 0.2f, 0.7f);
		for (int i = 0; i < 3; i++)
			r = UvTransform.apply(1, r[0], r[1]);
		assertArrayEquals(new float[]{0.2f, 0.7f}, r, 1e-6f);
	}
}
