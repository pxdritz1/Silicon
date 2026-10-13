package silicon.px;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class SiliconConfig {
	public static boolean enabled = true;
	public static boolean mergeRandomTextures = true;
	public static boolean fastMode = false;
	// how many colour or light steps (out of 255) two faces may differ by and still merge; 0 is exact
	public static int tolerance = 0;
	// block ids that never merge, e.g. "minecraft:stone"
	public static List<String> exclude = List.of();

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

	private SiliconConfig() {
	}

	private static Path path() {
		return FabricLoader.getInstance().getConfigDir().resolve("silicon.json");
	}

	public static void load() {
		Path path = path();
		if (Files.exists(path)) {
			try (Reader reader = Files.newBufferedReader(path)) {
				Data data = GSON.fromJson(reader, Data.class);
				if (data != null) {
					enabled = data.enabled;
					mergeRandomTextures = data.mergeRandomTextures;
					fastMode = data.fastMode;
					tolerance = Math.clamp(data.tolerance, 0, 16);
					exclude = data.exclude != null ? List.copyOf(data.exclude) : List.of();
				}
			} catch (IOException | JsonParseException e) {
				Silicon.LOGGER.warn("Couldn't read {}, using defaults", path, e);
			}
		}
		save();
	}

	public static void save() {
		Data data = new Data();
		data.enabled = enabled;
		data.mergeRandomTextures = mergeRandomTextures;
		data.fastMode = fastMode;
		data.tolerance = tolerance;
		data.exclude = exclude;
		try {
			Files.createDirectories(path().getParent());
			try (Writer writer = Files.newBufferedWriter(path())) {
				GSON.toJson(data, writer);
			}
		} catch (IOException e) {
			Silicon.LOGGER.warn("Couldn't save {}", path(), e);
		}
	}

	private static class Data {
		boolean enabled = true;
		boolean mergeRandomTextures = true;
		boolean fastMode = false;
		int tolerance = 0;
		List<String> exclude = List.of();
	}
}
