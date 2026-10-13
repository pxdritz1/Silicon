package silicon.px.mesh;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PayloadTest {
	@Test
	void roundTrip() {
		for (int face = 0; face < 6; face++)
			for (int t = 0; t < 8; t++)
				for (int w = 0; w < 8; w++)
					for (int h = 0; h < 8; h++) {
						int p = Payload.encode(face, t, w, h);
						assertTrue(Payload.isMarked(Payload.x(p), Payload.y(p)));
						assertTrue(Payload.y(p) <= 127 && Payload.z(p) <= 127);
						assertEquals(face, Payload.face(p));
						assertEquals(t, Payload.transform(p));
						assertEquals(w, Payload.log2W(p));
						assertEquals(h, Payload.log2H(p));
					}
	}

	@Test
	void realNormalsAreNotMarked() {
		assertFalse(Payload.isMarked(0, 0));
		assertFalse(Payload.isMarked(127, 0));
		assertFalse(Payload.isMarked(89, 0));
	}

	@Test
	void survivesBufferBuilderPacking() {
		for (int b = 0; b <= 127; b++)
			assertEquals(b, (int) (Math.clamp(Payload.normalFloat(b), -1f, 1f) * 127f));
	}
}
