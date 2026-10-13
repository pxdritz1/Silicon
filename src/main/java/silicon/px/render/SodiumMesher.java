package silicon.px.render;

import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.state.BlockState;
import silicon.px.SiliconConfig;
import silicon.px.compat.ShaderPacks;
import silicon.px.compat.SodiumShaders;
import silicon.px.debug.Stats;
import silicon.px.mesh.Faces;

/**
 * The Sodium path. Sodium renders every block itself; while it renders a plain cube, the quads it pushes
 * are offered here first. Evenly lit faces are kept back and merged at the end of the section, the rest go
 * through untouched. Sodium's vertex format has no spare normal, so the shader info rides in unused
 * material bits instead (see SodiumShaders). One instance per chunk builder thread.
 */
public final class SodiumMesher {
	private static final ThreadLocal<SodiumMesher> INSTANCE = ThreadLocal.withInitial(SodiumMesher::new);

	// Sodium uses the low three material bits, the shader reads ours from bit 3 up
	private static final int MATERIAL_SHIFT = 3;

	private final FaceMerger merger = new FaceMerger();
	private final ChunkVertexEncoder.Vertex[] quad = ChunkVertexEncoder.Vertex.uninitializedQuad();
	private final float[] xs = new float[4], ys = new float[4], zs = new float[4], us = new float[4], vs = new float[4];
	private final int[] order = new int[4], colors = new int[4], lights = new int[4], shades = new int[4];

	private boolean active;
	private long section;
	private CubeModel model;
	private int index, bx, by, bz;
	private int faces, copied;

	public static SodiumMesher get() {
		return INSTANCE.get();
	}

	public void begin(SectionPos sectionPos) {
		merger.reset();
		model = null;
		faces = 0;
		copied = 0;
		section = sectionPos.asLong();
		active = SiliconConfig.enabled && SodiumShaders.patched() && !ShaderPacks.active();
	}

	/**
	 * @param local block position inside the section
	 */
	public void startBlock(BlockState state, BlockPos local) {
		model = active ? CubeModels.get(state) : null;
		if (model == null)
			return;
		bx = local.getX();
		by = local.getY();
		bz = local.getZ();
		index = Faces.index(bx, by, bz);
	}

	public void endBlock() {
		model = null;
	}

	/**
	 * @return true if the quad was kept for merging, false if Sodium should write it as usual
	 */
	public boolean offer(ChunkMeshBufferBuilder target, ChunkVertexEncoder.Vertex[] vertices, int material) {
		if (model == null)
			return false;
		faces++;
		for (int i = 0; i < 4; i++) {
			ChunkVertexEncoder.Vertex v = vertices[i];
			xs[i] = v.x;
			ys[i] = v.y;
			zs[i] = v.z;
			us[i] = v.u;
			vs[i] = v.v;
		}
		int face = FaceCheck.faceOf(xs, ys, zs, bx, by, bz);
		int size = face >= 0 ? sizeCode(face) : -1;
		BakedQuad shape = face >= 0 ? model.faces()[face][0] : null;
		// blending shade across see-through blocks lights up the faces inside them, so cutout always merges exactly
		boolean fast = SiliconConfig.fastMode && model.layer() == ChunkSectionLayer.SOLID;
		int step = SiliconConfig.tolerance + 1;
		if (size < 0 || !model.mergeable()[face] || !(fast || flat(vertices, step)) || !FaceCheck.matchesModel(us, vs, model, face)
				|| !FaceCheck.cornerOrder(shape, xs, ys, zs, bx, by, bz, order)) {
			copied++;
			return false;
		}

		for (int c = 0; c < 4; c++) {
			ChunkVertexEncoder.Vertex v = vertices[order[c]];
			colors[c] = v.color;
			lights[c] = v.light;
			shades[c] = Float.floatToIntBits(v.ao);
		}
		int code = 1 + size * 8 + model.transform()[face];
		Appearance look = new Appearance(target, shape.sprite(), face, model.transform()[face], model.log2W()[face], model.log2H()[face],
				fast ? 0 : Shade.color(colors[0], step), fast ? 0 : Shade.light(lights[0], step),
				fast ? 0 : Shade.ao(Float.intBitsToFloat(shades[0]), step), material | (code << MATERIAL_SHIFT));
		merger.add(index, look, shape, colors, lights, shades);
		return true;
	}

	public void finish() {
		int merged = merger.mesh(this::write);
		if (faces > 0)
			Stats.record(section, faces, copied + merged, CompactChunkVertex.STRIDE);
		else
			Stats.forget(section);
		merger.reset();
	}

	private void write(Appearance look, float[] corners, int[] colors, int[] lights, int[] shades) {
		TextureAtlasSprite sprite = look.sprite();
		float cu = (sprite.getU0() + sprite.getU1()) * 0.5f;
		float cv = (sprite.getV0() + sprite.getV1()) * 0.5f;
		for (int i = 0; i < 4; i++)
			ChunkVertexEncoder.Vertex.writeVertex(quad[i], corners[i * 3], corners[i * 3 + 1], corners[i * 3 + 2], colors[i],
					Float.intBitsToFloat(shades[i]), cu, cv, lights[i]);
		((ChunkMeshBufferBuilder) look.target()).push(quad, look.material());
	}

	// the shader knows 16, 32 and 64 pixel square sprites; anything else stays one block per quad
	private int sizeCode(int face) {
		int w = model.log2W()[face], h = model.log2H()[face];
		return w == h && w >= 4 && w <= 6 ? w - 4 : -1;
	}

	private static boolean flat(ChunkVertexEncoder.Vertex[] q, int step) {
		ChunkVertexEncoder.Vertex a = q[0];
		for (int i = 1; i < 4; i++) {
			ChunkVertexEncoder.Vertex b = q[i];
			if (Shade.color(b.color, step) != Shade.color(a.color, step) || Shade.light(b.light, step) != Shade.light(a.light, step)
					|| (step <= 1 ? Float.floatToIntBits(b.ao) != Float.floatToIntBits(a.ao) : Shade.ao(b.ao, step) != Shade.ao(a.ao, step)))
				return false;
		}
		return true;
	}
}
