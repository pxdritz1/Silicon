package silicon.px.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import silicon.px.debug.Stats;
import silicon.px.render.CubeModels;

// allChanged runs after resource reloads and graphics changes, the only times baked models can change
@Mixin(LevelRenderer.class)
abstract class LevelRendererMixin {
	@Inject(method = "allChanged", at = @At("HEAD"))
	private void silicon$reset(CallbackInfo ci) {
		CubeModels.clear();
		Stats.clear();
	}
}
