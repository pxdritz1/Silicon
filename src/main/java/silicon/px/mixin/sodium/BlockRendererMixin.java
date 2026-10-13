package silicon.px.mixin.sodium;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.caffeinemc.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderer;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.caffeinemc.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import silicon.px.render.SodiumMesher;

@Mixin(value = BlockRenderer.class, remap = false)
abstract class BlockRendererMixin {
	@WrapOperation(method = "bufferQuad", at = @At(value = "INVOKE",
			target = "Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/builder/ChunkMeshBufferBuilder;push([Lnet/caffeinemc/mods/sodium/client/render/chunk/vertex/format/ChunkVertexEncoder$Vertex;I)V"))
	private void silicon$push(ChunkMeshBufferBuilder builder, ChunkVertexEncoder.Vertex[] vertices, int material, Operation<Void> original) {
		if (!SodiumMesher.get().offer(builder, vertices, material))
			original.call(builder, vertices, material);
	}
}
