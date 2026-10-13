package silicon.px.render;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.builders.UVPair;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3fc;
import silicon.px.SiliconConfig;
import silicon.px.mesh.UvTransform;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CubeModels {
	private static final Direction[] DIRECTIONS = Direction.values();
	// enough to catch the random rotations vanilla gives stone, sand, dirt and friends
	private static final int VARIANT_SAMPLES = 16;

	// for modpacks and other mods: blocks in this tag are always drawn the normal way
	public static final TagKey<Block> NO_GREEDY = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("silicon", "no_greedy"));

	private static final Map<BlockState, Optional<CubeModel>> CACHE = new ConcurrentHashMap<>();

	private CubeModels() {
	}

	public static CubeModel get(BlockState state) {
		Optional<CubeModel> cached = CACHE.get(state);
		if (cached == null) {
			cached = Optional.ofNullable(build(state));
			CACHE.put(state, cached);
		}
		return cached.orElse(null);
	}

	public static void clear() {
		CACHE.clear();
	}

	private static CubeModel build(BlockState state) {
		if (state.is(NO_GREEDY) || SiliconConfig.exclude.contains(BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString()))
			return null;
		if (state.getRenderShape() != RenderShape.MODEL || state.hasBlockEntity() || !state.getFluidState().isEmpty())
			return null;
		// Translucent faces get depth sorted per quad, so stretching them would break the sort. Cutout is
		// fine: leaves, glass and grass overlays just discard pixels.
		ChunkSectionLayer layer = ItemBlockRenderTypes.getChunkRenderType(state);
		if (layer != ChunkSectionLayer.SOLID && layer != ChunkSectionLayer.CUTOUT)
			return null;

		BlockStateModel model = Minecraft.getInstance().getBlockRenderer().getBlockModel(state);
		BlockModelPart part = part(model, state, 0);
		BakedQuad[][] faces = cubeFaces(part);
		if (faces == null)
			return null;

		boolean[] varies = new boolean[6];
		for (int i = 1; i < VARIANT_SAMPLES; i++) {
			BakedQuad[][] other = cubeFaces(part(model, state, i));
			if (other == null)
				return null;
			for (int f = 0; f < 6; f++) {
				if (!sameLook(faces[f], other[f]))
					varies[f] = true;
			}
		}

		int[] transform = new int[6];
		int[] log2W = new int[6];
		int[] log2H = new int[6];
		boolean[] mergeable = new boolean[6];
		for (int f = 0; f < 6; f++) {
			// layered faces, like grass sides with their overlay, are drawn one block at a time
			if (faces[f].length != 1)
				continue;
			BakedQuad q = faces[f][0];
			TextureAtlasSprite sprite = q.sprite();
			log2W[f] = log2(sprite.contents().width());
			log2H[f] = log2(sprite.contents().height());
			transform[f] = UvTransform.detect(f, positions(q), spriteUvs(q));
			mergeable[f] = transform[f] >= 0 && log2W[f] >= 0 && log2H[f] >= 0
					&& (!varies[f] || SiliconConfig.mergeRandomTextures);
		}
		return new CubeModel(model, layer, faces, varies, transform, log2W, log2H, mergeable);
	}

	// The seed has to come from a real position: java.util.Random gives almost the same first value for small seeds.
	private static BlockModelPart part(BlockStateModel model, BlockState state, int sample) {
		List<BlockModelPart> parts = model.collectParts(RandomSource.create(state.getSeed(new BlockPos(sample * 37, 64, sample * 101))));
		return parts.size() == 1 ? parts.getFirst() : null;
	}

	private static BakedQuad[][] cubeFaces(BlockModelPart part) {
		if (part == null || !part.getQuads(null).isEmpty())
			return null;
		BakedQuad[][] faces = new BakedQuad[6][];
		for (Direction dir : DIRECTIONS) {
			List<BakedQuad> list = part.getQuads(dir);
			if (list.isEmpty())
				return null;
			for (BakedQuad q : list) {
				if (!isFullFace(q, dir))
					return null;
			}
			faces[dir.ordinal()] = list.toArray(new BakedQuad[0]);
		}
		return faces;
	}

	private static boolean isFullFace(BakedQuad q, Direction dir) {
		if (q.direction() != dir || q.lightEmission() != 0)
			return false;
		int axis = dir.getAxis().ordinal();
		float plane = dir.getAxisDirection() == Direction.AxisDirection.POSITIVE ? 1 : 0;
		float minA = 1, maxA = 0, minB = 1, maxB = 0;
		for (int i = 0; i < 4; i++) {
			Vector3fc p = q.position(i);
			float[] c = {p.x(), p.y(), p.z()};
			if (c[axis] != plane)
				return false;
			float a = c[(axis + 1) % 3], b = c[(axis + 2) % 3];
			if ((a != 0 && a != 1) || (b != 0 && b != 1))
				return false;
			minA = Math.min(minA, a);
			maxA = Math.max(maxA, a);
			minB = Math.min(minB, b);
			maxB = Math.max(maxB, b);
		}
		return minA == 0 && maxA == 1 && minB == 0 && maxB == 1;
	}

	private static boolean sameLook(BakedQuad[] a, BakedQuad[] b) {
		if (a.length != b.length)
			return false;
		for (int i = 0; i < a.length; i++) {
			if (!sameLook(a[i], b[i]))
				return false;
		}
		return true;
	}

	private static boolean sameLook(BakedQuad a, BakedQuad b) {
		if (a.sprite() != b.sprite() || a.tintIndex() != b.tintIndex() || a.shade() != b.shade())
			return false;
		for (int i = 0; i < 4; i++) {
			if (!a.position(i).equals(b.position(i)) || a.packedUV(i) != b.packedUV(i))
				return false;
		}
		return true;
	}

	private static float[] positions(BakedQuad q) {
		float[] out = new float[12];
		for (int i = 0; i < 4; i++) {
			Vector3fc p = q.position(i);
			out[i * 3] = p.x();
			out[i * 3 + 1] = p.y();
			out[i * 3 + 2] = p.z();
		}
		return out;
	}

	private static float[] spriteUvs(BakedQuad q) {
		TextureAtlasSprite s = q.sprite();
		float du = s.getU1() - s.getU0();
		float dv = s.getV1() - s.getV0();
		float[] out = new float[8];
		for (int i = 0; i < 4; i++) {
			long uv = q.packedUV(i);
			out[i * 2] = (UVPair.unpackU(uv) - s.getU0()) / du;
			out[i * 2 + 1] = (UVPair.unpackV(uv) - s.getV0()) / dv;
		}
		return out;
	}

	// -1 unless n is a power of two the shader can express (1..128)
	private static int log2(int n) {
		if (n <= 0 || n > 128 || Integer.bitCount(n) != 1)
			return -1;
		return Integer.numberOfTrailingZeros(n);
	}
}
