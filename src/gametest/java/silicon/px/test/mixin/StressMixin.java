package silicon.px.test.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import silicon.px.test.StressBenchmark;

// strestest made by Claude. Everything else in mod made by human hadn.
/**
 * Makes vertex work artificially expensive, the way it is on a weak GPU: every section gets (N - 1) extra
 * copies of its vertex count as zero area quads. The GPU runs the vertex shader on them but draws no pixels,
 * so only the triangle side of the frame gets slower, in proportion to how many quads a section has.
 */
@Mixin(SectionCompiler.class)
abstract class StressMixin {
	@WrapOperation(method = "compile", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;build()Lcom/mojang/blaze3d/vertex/MeshData;"))
	private MeshData silicon$stress(BufferBuilder builder, Operation<MeshData> original) {
		int extra = (StressBenchmark.factor - 1) * ((BufferBuilderAccessor) builder).silicon$vertices();
		for (int i = 0; i < extra; i++)
			builder.addVertex(0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0);
		return original.call(builder);
	}
}
