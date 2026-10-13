package silicon.px.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.SectionBufferBuilderPack;
import net.minecraft.client.renderer.chunk.ChunkSectionLayer;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silicon.px.Silicon;
import silicon.px.render.SectionMesher;

import java.util.Map;
import java.util.function.Function;

@Mixin(SectionCompiler.class)
abstract class SectionCompilerMixin {
	@Unique
	private static final ThreadLocal<SectionMesher> MESHER = ThreadLocal.withInitial(SectionMesher::new);
	@Unique
	private static boolean warned;

	@Shadow
	protected abstract BufferBuilder getOrBeginLayer(Map<ChunkSectionLayer, BufferBuilder> map, SectionBufferBuilderPack pack, ChunkSectionLayer layer);

	@Inject(method = "compile", at = @At("HEAD"))
	private void silicon$begin(SectionPos sectionPos, RenderSectionRegion region, VertexSorting sorting, SectionBufferBuilderPack pack,
			CallbackInfoReturnable<SectionCompiler.Results> cir) {
		MESHER.get().begin(sectionPos);
	}

	@WrapOperation(method = "compile", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/world/level/block/state/BlockState;getRenderShape()Lnet/minecraft/world/level/block/RenderShape;"))
	private RenderShape silicon$capture(BlockState state, Operation<RenderShape> original, @Local(ordinal = 2) BlockPos pos) {
		// captured blocks are drawn later by SectionMesher, so vanilla (or Indigo) has to skip them
		return MESHER.get().capture(pos, state) ? RenderShape.INVISIBLE : original.call(state);
	}

	@Inject(method = "compile", at = @At(value = "INVOKE", target = "Ljava/util/Map;entrySet()Ljava/util/Set;"))
	private void silicon$emit(SectionPos sectionPos, RenderSectionRegion region, VertexSorting sorting, SectionBufferBuilderPack pack,
			CallbackInfoReturnable<SectionCompiler.Results> cir, @Local Map<ChunkSectionLayer, BufferBuilder> map) {
		SectionMesher mesher = MESHER.get();
		if (!mesher.hasWork()) {
			mesher.forget();
			return;
		}
		Function<ChunkSectionLayer, BufferBuilder> buffers = layer -> getOrBeginLayer(map, pack, layer);
		try {
			mesher.finish(region, buffers);
		} catch (RuntimeException e) {
			if (!warned) {
				warned = true;
				Silicon.LOGGER.error("Merging failed for section {}, drawing it the normal way", sectionPos, e);
			}
			mesher.renderVanilla(region, buffers);
		}
	}
}
