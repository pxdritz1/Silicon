package silicon.px.debug;

import net.minecraft.client.gui.components.debug.DebugScreenDisplayer;
import net.minecraft.client.gui.components.debug.DebugScreenEntry;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import org.jspecify.annotations.Nullable;
import silicon.px.SiliconConfig;
import silicon.px.compat.ShaderPacks;

import java.util.Locale;

public class SiliconDebugEntry implements DebugScreenEntry {
	@Override
	public void display(DebugScreenDisplayer out, @Nullable Level level, @Nullable LevelChunk clientChunk, @Nullable LevelChunk serverChunk) {
		if (!SiliconConfig.enabled) {
			out.addLine("Silicon: off");
			return;
		}
		if (ShaderPacks.active()) {
			out.addLine("Silicon: off (shader pack)");
			return;
		}
		long[] t = Stats.totals();
		double saved = t[0] == 0 ? 0 : 100.0 * (t[0] - t[1]) / t[0];
		out.addLine(String.format(Locale.ROOT, "Silicon: %,d faces -> %,d quads (%.1f%% fewer)", t[0], t[1], saved));
		out.addLine(String.format(Locale.ROOT, "Silicon: block meshes %.1f MB -> %.1f MB", t[2] / 1048576.0, t[3] / 1048576.0));
	}

	@Override
	public boolean isAllowed(boolean reducedDebugInfo) {
		return true;
	}
}
