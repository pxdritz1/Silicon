package silicon.px.compat;

import net.fabricmc.loader.api.FabricLoader;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Shader packs replace the terrain shader, so merged quads would show one stretched texture.
 * Iris is only reached through reflection to keep it optional.
 */
public final class ShaderPacks {
	private static final MethodHandle IN_USE = find();
	private static boolean failed;

	private ShaderPacks() {
	}

	public static boolean active() {
		if (IN_USE == null || failed)
			return false;
		try {
			return (boolean) IN_USE.invoke();
		} catch (Throwable t) {
			failed = true;
			return false;
		}
	}

	private static MethodHandle find() {
		if (!FabricLoader.getInstance().isModLoaded("iris"))
			return null;
		try {
			Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
			MethodHandles.Lookup lookup = MethodHandles.publicLookup();
			Object instance = lookup.findStatic(api, "getInstance", MethodType.methodType(api)).invoke();
			return lookup.findVirtual(api, "isShaderPackInUse", MethodType.methodType(boolean.class)).bindTo(instance);
		} catch (Throwable t) {
			return null;
		}
	}
}
