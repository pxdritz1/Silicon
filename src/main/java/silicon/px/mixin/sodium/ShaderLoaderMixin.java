package silicon.px.mixin.sodium;

import net.caffeinemc.mods.sodium.client.gl.shader.ShaderLoader;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import silicon.px.compat.SodiumShaders;

@Mixin(value = ShaderLoader.class, remap = false)
abstract class ShaderLoaderMixin {
	@Inject(method = "getShaderSource", at = @At("RETURN"), cancellable = true)
	private static void silicon$patch(Identifier name, CallbackInfoReturnable<String> cir) {
		cir.setReturnValue(SodiumShaders.patch(name.getPath(), cir.getReturnValue()));
	}
}
