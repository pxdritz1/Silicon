package silicon.px.render;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;

/**
 * A block whose model is six full faces, some possibly layered. Arrays are indexed by face.
 * faces holds one sampled variant; varies marks faces that change with the block's random seed.
 * A face that isn't mergeable is still drawn by Silicon, just one block at a time.
 */
public record CubeModel(BlockStateModel model, ChunkSectionLayer layer, BakedQuad[][] faces, boolean[] varies, int[] transform,
		int[] log2W, int[] log2H, boolean[] mergeable) {
}
