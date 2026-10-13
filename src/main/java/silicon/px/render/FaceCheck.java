package silicon.px.render;

import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector3fc;
import silicon.px.mesh.Faces;

/**
 * Checks on a quad a renderer produced for a captured block. Coordinates are section local.
 */
final class FaceCheck {
	private static final float EPS = 1e-4f;

	private FaceCheck() {
	}

	/**
	 * Which side of the block at (x, y, z) the quad lies on, or -1 if it's somewhere inside.
	 */
	static int faceOf(float[] xs, float[] ys, float[] zs, int x, int y, int z) {
		if (same(ys, y)) return Faces.DOWN;
		if (same(ys, y + 1)) return Faces.UP;
		if (same(zs, z)) return Faces.NORTH;
		if (same(zs, z + 1)) return Faces.SOUTH;
		if (same(xs, x)) return Faces.WEST;
		if (same(xs, x + 1)) return Faces.EAST;
		return -1;
	}

	/**
	 * Finds, for each corner of the model's quad, the recorded vertex at the same spot (renderers may rotate the
	 * vertex order). Fills order[corner] = recorded index and returns false if a corner has no match.
	 */
	static boolean cornerOrder(BakedQuad shape, float[] xs, float[] ys, float[] zs, int x, int y, int z, int[] order) {
		for (int i = 0; i < 4; i++) {
			Vector3fc c = shape.position(i);
			order[i] = -1;
			for (int j = 0; j < 4; j++) {
				if (Math.abs(xs[j] - x - c.x()) < EPS && Math.abs(ys[j] - y - c.y()) < EPS && Math.abs(zs[j] - z - c.z()) < EPS) {
					order[i] = j;
					break;
				}
			}
			if (order[i] < 0)
				return false;
		}
		return true;
	}

	private static boolean same(float[] coords, float value) {
		for (int i = 0; i < 4; i++) {
			if (Math.abs(coords[i] - value) > EPS)
				return false;
		}
		return true;
	}

	/**
	 * Guards against renderers or mods that swap textures per block (connected textures and the like):
	 * the face only counts as the model's face if it shows the model's sprite the way the model lays it out.
	 * Faces with random variants may use any layout of the sprite.
	 */
	static boolean matchesModel(float[] us, float[] vs, CubeModel model, int face) {
		BakedQuad quad = model.faces()[face][0];
		TextureAtlasSprite sprite = quad.sprite();
		boolean anyLayout = model.varies()[face];
		for (int i = 0; i < 4; i++) {
			float u = us[i], v = vs[i];
			if (u < sprite.getU0() - EPS || u > sprite.getU1() + EPS || v < sprite.getV0() - EPS || v > sprite.getV1() + EPS)
				return false;
			if (!anyLayout && !hasUv(quad, u, v))
				return false;
		}
		return true;
	}

	private static boolean hasUv(BakedQuad quad, float u, float v) {
		for (int i = 0; i < 4; i++) {
			long uv = quad.packedUV(i);
			if (Math.abs(UVPair.unpackU(uv) - u) < 1e-6f && Math.abs(UVPair.unpackV(uv) - v) < 1e-6f)
				return true;
		}
		return false;
	}
}
