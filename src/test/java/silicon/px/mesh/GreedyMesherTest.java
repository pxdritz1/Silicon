package silicon.px.mesh;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

class GreedyMesherTest {
	record Quad(int face, int layer, int u, int v, int w, int h, int id) {
	}

	private static List<Quad> run(int[][] ids) {
		List<Quad> out = new ArrayList<>();
		new GreedyMesher().mesh(ids, (f, l, u, v, w, h, id) -> out.add(new Quad(f, l, u, v, w, h, id)));
		return out;
	}

	private static int[][] empty() {
		return new int[6][4096];
	}

	@Test
	void fullLayerBecomesOneQuad() {
		int[][] ids = empty();
		for (int u = 0; u < 16; u++)
			for (int v = 0; v < 16; v++)
				ids[Faces.UP][Faces.blockIndex(Faces.UP, 3, u, v)] = 1;
		assertEquals(List.of(new Quad(Faces.UP, 3, 0, 0, 16, 16, 1)), run(ids));
	}

	@Test
	void differentIdsNeverMerge() {
		int[][] ids = empty();
		ids[Faces.UP][Faces.blockIndex(Faces.UP, 0, 0, 0)] = 1;
		ids[Faces.UP][Faces.blockIndex(Faces.UP, 0, 1, 0)] = 2;
		assertEquals(2, run(ids).size());
	}

	@Test
	void lShapeSplitsIntoTwo() {
		int[][] ids = empty();
		ids[Faces.NORTH][Faces.blockIndex(Faces.NORTH, 0, 0, 0)] = 1;
		ids[Faces.NORTH][Faces.blockIndex(Faces.NORTH, 0, 1, 0)] = 1;
		ids[Faces.NORTH][Faces.blockIndex(Faces.NORTH, 0, 0, 1)] = 1;
		assertEquals(2, run(ids).size());
	}

	@Test
	void coversEveryCellExactlyOnce() {
		Random r = new Random(42);
		for (int round = 0; round < 200; round++) {
			int[][] ids = empty();
			for (int f = 0; f < 6; f++)
				for (int i = 0; i < 4096; i++)
					ids[f][i] = r.nextInt(4) == 0 ? 0 : 1 + r.nextInt(3);

			int[][] covered = empty();
			for (Quad q : run(ids)) {
				for (int dv = 0; dv < q.h; dv++) {
					for (int du = 0; du < q.w; du++) {
						int i = Faces.blockIndex(q.face, q.layer, q.u + du, q.v + dv);
						assertEquals(q.id, ids[q.face][i], "quad covers a cell with another id");
						assertEquals(0, covered[q.face][i]++, "cell covered twice");
					}
				}
			}
			for (int f = 0; f < 6; f++)
				for (int i = 0; i < 4096; i++)
					assertEquals(ids[f][i] != 0 ? 1 : 0, covered[f][i], "cell missed");
		}
	}

	@Test
	void doesNotModifyInput() {
		int[][] ids = empty();
		ids[0][0] = 5;
		run(ids);
		assertEquals(5, ids[0][0]);
	}
}
