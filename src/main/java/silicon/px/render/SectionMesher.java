package silicon.px.render;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import silicon.px.SiliconConfig;
import silicon.px.compat.ShaderPacks;
import silicon.px.debug.Stats;
import silicon.px.mesh.Faces;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

/**
 * The vanilla path. Collects the plain cubes of one section while vanilla walks it, then draws them merged.
 * Each block is first drawn into a recorder by whatever block renderer is active (vanilla or Indigo), so the
 * light and tint of every face are exactly what that renderer produces. Faces that come out evenly lit merge,
 * everything else is copied through untouched. One instance per chunk builder thread.
 */
public final class SectionMesher {
	private final BlockState[] states = new BlockState[4096];
	private final CubeModel[] models = new CubeModel[4096];
	private final int[] captured = new int[4096];
	private int count;

	private final FaceMerger merger = new FaceMerger();
	private final QuadRecorder recorder = new QuadRecorder();
	private final PoseStack pose = new PoseStack();
	private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
	private final RandomSource random = RandomSource.create();
	private final List<BlockModelPart> parts = new ArrayList<>();
	private final float[] xs = new float[4], ys = new float[4], zs = new float[4], us = new float[4], vs = new float[4];
	private final int[] order = new int[4], colors = new int[4], lights = new int[4];
	private static final int[] NO_SHADE = new int[4];

	private boolean active;
	private long section;
	private int ox, oy, oz;

	public void begin(SectionPos sectionPos) {
		for (int i = 0; i < count; i++) {
			states[captured[i]] = null;
			models[captured[i]] = null;
		}
		count = 0;
		section = sectionPos.asLong();
		ox = sectionPos.minBlockX();
		oy = sectionPos.minBlockY();
		oz = sectionPos.minBlockZ();
		active = SiliconConfig.enabled && !ShaderPacks.active();
	}

	public boolean capture(BlockPos blockPos, BlockState state) {
		if (!active)
			return false;
		CubeModel model = CubeModels.get(state);
		if (model == null)
			return false;
		int index = Faces.index(blockPos.getX() - ox, blockPos.getY() - oy, blockPos.getZ() - oz);
		states[index] = state;
		models[index] = model;
		captured[count++] = index;
		return true;
	}

	public boolean hasWork() {
		return count > 0;
	}

	public void forget() {
		Stats.forget(section);
	}

	public void finish(BlockAndTintGetter level, Function<ChunkSectionLayer, ? extends VertexConsumer> buffers) {
		merger.reset();
		boolean fast = SiliconConfig.fastMode;
		int step = SiliconConfig.tolerance + 1;
		int faces = 0;
		int copied = 0;
		for (int i = 0; i < count; i++) {
			int index = captured[i];
			int x = index & 15, y = index >> 8, z = (index >> 4) & 15;
			CubeModel model = models[index];
			VertexConsumer out = buffers.apply(model.layer());
			// blending shade across see-through blocks lights up the faces inside them, so cutout always merges exactly
			boolean fastHere = fast && model.layer() == ChunkSectionLayer.SOLID;

			recorder.reset();
			render(level, states[index], x, y, z, recorder);
			if (recorder.overflow) {
				render(level, states[index], x, y, z, out);
				continue;
			}

			for (int q = 0; q < recorder.quads(); q++) {
				faces++;
				int v = q * 4;
				copy(v);
				int face = FaceCheck.faceOf(xs, ys, zs, x, y, z);
				BakedQuad shape = face >= 0 ? model.faces()[face][0] : null;
				if (face >= 0 && model.mergeable()[face] && (fastHere || flat(v, step)) && sameOverlay(v)
						&& FaceCheck.matchesModel(us, vs, model, face) && FaceCheck.cornerOrder(shape, xs, ys, zs, x, y, z, order)) {
					for (int c = 0; c < 4; c++) {
						colors[c] = recorder.color[v + order[c]];
						lights[c] = recorder.light[v + order[c]];
					}
					// the key only holds the rounded shading (none at all in fast mode), every corner keeps its real value
					Appearance look = new Appearance(model.layer(), shape.sprite(), face, model.transform()[face], model.log2W()[face],
							model.log2H()[face], fastHere ? 0 : Shade.color(colors[0], step), fastHere ? 0 : Shade.light(lights[0], step), 0, 0);
					merger.add(index, look, shape, colors, lights, NO_SHADE);
				} else {
					recorder.replay(out, q);
					copied++;
				}
			}
		}

		int merged = merger.mesh((look, corners, colors, lights, shades) ->
				QuadEmitter.merged(buffers.apply((ChunkSectionLayer) look.target()), look, corners, colors, lights));
		Stats.record(section, faces, copied + merged, DefaultVertexFormat.BLOCK.getVertexSize());
	}

	private void copy(int v) {
		System.arraycopy(recorder.x, v, xs, 0, 4);
		System.arraycopy(recorder.y, v, ys, 0, 4);
		System.arraycopy(recorder.z, v, zs, 0, 4);
		System.arraycopy(recorder.u, v, us, 0, 4);
		System.arraycopy(recorder.v, v, vs, 0, 4);
	}

	// all four corners lit and tinted the same (within the tolerance), so stretching the quad changes nothing visible
	private boolean flat(int v, int step) {
		int color = Shade.color(recorder.color[v], step), light = Shade.light(recorder.light[v], step);
		for (int i = v + 1; i < v + 4; i++) {
			if (Shade.color(recorder.color[i], step) != color || Shade.light(recorder.light[i], step) != light)
				return false;
		}
		return true;
	}

	private boolean sameOverlay(int v) {
		for (int i = v + 1; i < v + 4; i++) {
			if (recorder.overlay[i] != recorder.overlay[v])
				return false;
		}
		return true;
	}

	private void render(BlockAndTintGetter level, BlockState state, int x, int y, int z, VertexConsumer target) {
		BlockRenderDispatcher dispatcher = Minecraft.getInstance().getBlockRenderer();
		pos.set(ox + x, oy + y, oz + z);
		random.setSeed(state.getSeed(pos));
		parts.clear();
		dispatcher.getBlockModel(state).collectParts(random, parts);
		pose.pushPose();
		pose.translate(x, y, z);
		dispatcher.renderBatched(state, pos, level, pose, target, true, parts);
		pose.popPose();
	}

	/**
	 * Fallback if finish throws: hand the captured blocks back to the normal renderer so the section still renders.
	 */
	public void renderVanilla(BlockAndTintGetter level, Function<ChunkSectionLayer, ? extends VertexConsumer> buffers) {
		for (int i = 0; i < count; i++) {
			int index = captured[i];
			render(level, states[index], index & 15, index >> 8, (index >> 4) & 15, buffers.apply(models[index].layer()));
		}
		Stats.forget(section);
	}
}
