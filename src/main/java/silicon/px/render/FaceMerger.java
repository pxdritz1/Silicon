package silicon.px.render;

import net.minecraft.client.renderer.block.model.BakedQuad;
import org.joml.Vector3fc;
import silicon.px.mesh.Faces;
import silicon.px.mesh.GreedyMesher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects mergeable faces of one section and turns them into rectangles. Shared by the vanilla and
 * Sodium paths, which only differ in how they record faces and write vertices. One instance per thread.
 *
 * Every face keeps its four corner colours, lights and shades (in the order of the block's own quad). In exact
 * mode those are all equal anyway; in fast mode a merged quad takes each corner from the block sitting in
 * that corner of the rectangle, so shading is smoothed across it instead of copied from one block.
 */
final class FaceMerger {
	interface Sink {
		void quad(Appearance look, float[] corners, int[] colors, int[] lights, int[] shades);
	}

	private final int[][] ids = new int[6][4096];
	private final int[] cornerColor = new int[6 * 4096 * 4];
	private final int[] cornerLight = new int[6 * 4096 * 4];
	private final int[] cornerShade = new int[6 * 4096 * 4];
	private final Map<Appearance, Integer> lookup = new HashMap<>();
	private final List<Appearance> looks = new ArrayList<>();
	private final List<BakedQuad> shapes = new ArrayList<>();
	private final GreedyMesher mesher = new GreedyMesher();
	private final float[] corners = new float[12];
	private final int[] colors = new int[4], lights = new int[4], shades = new int[4];
	private boolean empty = true;

	void reset() {
		if (empty)
			return;
		for (int[] faceIds : ids)
			Arrays.fill(faceIds, 0);
		lookup.clear();
		looks.clear();
		shapes.clear();
		empty = true;
	}

	/**
	 * @param shape the block's own quad for this face, its corner order is reused for the merged quad
	 */
	void add(int index, Appearance look, BakedQuad shape, int[] color, int[] light, int[] shade) {
		Integer id = lookup.get(look);
		if (id == null) {
			looks.add(look);
			shapes.add(shape);
			id = looks.size();
			lookup.put(look, id);
		}
		ids[look.face()][index] = id;
		int slot = (look.face() * 4096 + index) * 4;
		System.arraycopy(color, 0, cornerColor, slot, 4);
		System.arraycopy(light, 0, cornerLight, slot, 4);
		System.arraycopy(shade, 0, cornerShade, slot, 4);
		empty = false;
	}

	/**
	 * @return how many quads were written
	 */
	int mesh(Sink sink) {
		if (empty)
			return 0;
		int[] count = {0};
		mesher.mesh(ids, (face, layer, u, v, w, h, id) -> {
			BakedQuad shape = shapes.get(id - 1);
			for (int i = 0; i < 4; i++) {
				Vector3fc c = shape.position(i);
				float a, b;
				int o = i * 3;
				switch (face) {
					case Faces.DOWN, Faces.UP -> {
						a = c.x();
						b = c.z();
						corners[o] = u + a * w;
						corners[o + 1] = layer + c.y();
						corners[o + 2] = v + b * h;
					}
					case Faces.NORTH, Faces.SOUTH -> {
						a = c.x();
						b = c.y();
						corners[o] = u + a * w;
						corners[o + 1] = v + b * h;
						corners[o + 2] = layer + c.z();
					}
					default -> {
						a = c.z();
						b = c.y();
						corners[o] = layer + c.x();
						corners[o + 1] = v + b * h;
						corners[o + 2] = u + a * w;
					}
				}
				// the block in this corner of the rectangle lends its own value for the same corner
				int block = Faces.blockIndex(face, layer, a > 0 ? u + w - 1 : u, b > 0 ? v + h - 1 : v);
				int slot = (face * 4096 + block) * 4 + i;
				colors[i] = cornerColor[slot];
				lights[i] = cornerLight[slot];
				shades[i] = cornerShade[slot];
			}
			sink.quad(looks.get(id - 1), corners, colors, lights, shades);
			count[0]++;
		});
		return count[0];
	}
}
