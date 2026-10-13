package silicon.px.mesh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FacesTest {
	@Test
	void everyFaceVisitsEveryBlockOnce() {
		for (int face = 0; face < 6; face++) {
			boolean[] seen = new boolean[4096];
			for (int layer = 0; layer < 16; layer++) {
				for (int v = 0; v < 16; v++) {
					for (int u = 0; u < 16; u++) {
						int i = Faces.blockIndex(face, layer, u, v);
						assertFalse(seen[i], "face " + face + " hits " + i + " twice");
						seen[i] = true;
					}
				}
			}
		}
	}

	@Test
	void upUsesYAsLayer() {
		assertEquals(Faces.index(3, 7, 5), Faces.blockIndex(Faces.UP, 7, 3, 5));
	}

	@Test
	void westUsesXAsLayer() {
		assertEquals(Faces.index(2, 9, 4), Faces.blockIndex(Faces.WEST, 2, 4, 9));
	}

	@Test
	void normals() {
		assertEquals(-1, Faces.dy(Faces.DOWN));
		assertEquals(1, Faces.dz(Faces.SOUTH));
		assertEquals(1, Faces.dx(Faces.EAST));
		assertEquals(0, Faces.dx(Faces.UP));
	}
}
