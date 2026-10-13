package silicon.px.mixin;

import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * The Sodium hooks only load next to the Sodium line they were written against. With any other version
 * Silicon simply stays idle while Sodium renders.
 */
public class MixinPlugin implements IMixinConfigPlugin {
	private static final boolean SODIUM = FabricLoader.getInstance().getModContainer("sodium")
			.map(mod -> mod.getMetadata().getVersion().getFriendlyString().startsWith("0.8."))
			.orElse(false);

	@Override
	public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
		return !mixinClassName.contains(".sodium.") || SODIUM;
	}

	@Override
	public void onLoad(String mixinPackage) {
	}

	@Override
	public String getRefMapperConfig() {
		return null;
	}

	@Override
	public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
	}

	@Override
	public List<String> getMixins() {
		return null;
	}

	@Override
	public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}

	@Override
	public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
	}
}
