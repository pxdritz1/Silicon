package silicon.px.mesh;

import java.util.Arrays;

/**
 * Merges equal ids into rectangles, one 16x16 slice at a time. Id 0 means "nothing to merge here".
 * Not thread safe, keep one per thread.
 */
public final class GreedyMesher {
	private final int[] slice = new int[256];

	public void mesh(int[][] ids, QuadSink out) {
		for (int face = 0; face < 6; face++) {
			for (int layer = 0; layer < 16; layer++) {
				if (fill(ids[face], face, layer))
					merge(face, layer, out);
			}
		}
	}

	private void merge(int face, int layer, QuadSink out) {
		for (int v = 0; v < 16; v++) {
			for (int u = 0; u < 16; ) {
				int id = slice[v * 16 + u];
				if (id == 0) {
					u++;
					continue;
				}

				int w = 1;
				while (u + w < 16 && slice[v * 16 + u + w] == id)
					w++;
				int h = 1;
				while (v + h < 16 && rowMatches(v + h, u, w, id))
					h++;

				for (int row = v; row < v + h; row++)
					Arrays.fill(slice, row * 16 + u, row * 16 + u + w, 0);
				out.quad(face, layer, u, v, w, h, id);
				u += w;
			}
		}
	}

	private boolean fill(int[] faceIds, int face, int layer) {
		boolean any = false;
		for (int v = 0; v < 16; v++) {
			for (int u = 0; u < 16; u++) {
				int id = faceIds[Faces.blockIndex(face, layer, u, v)];
				slice[v * 16 + u] = id;
				any |= id != 0;
			}
		}
		return any;
	}

	private boolean rowMatches(int v, int u, int w, int id) {
		int start = v * 16 + u;
		for (int i = start; i < start + w; i++) {
			if (slice[i] != id)
				return false;
		}
		return true;
	}
}
