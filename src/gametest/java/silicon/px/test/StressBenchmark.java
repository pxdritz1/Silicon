package silicon.px.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.InactivityFpsLimit;
import silicon.px.SiliconConfig;
import silicon.px.debug.Stats;

// stresstest made by claude. Everything else in mod made by humand hand.
/**
 * Fps on a generated world with silicon off, exact, default tolerance and fast mode, once normally and once with
 * vertex work made 8 times more exxpensive (see StressMixin). Only runs with -Dsilicon.stressBench=true.
 */
public class StressBenchmark implements FabricClientGameTest {
	public static volatile int factor = 1;

	private static final int[] FACTORS = {1, 8};
	// name, enabled, tolerance, fast
	private static final Object[][] MODES = {
			{"off     ", false, 0, false},
			{"exact   ", true, 0, false},
			{"tol 2   ", true, 2, false},
			{"fast    ", true, 2, true},
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		if (!Boolean.getBoolean("silicon.stressBench"))
			return;
		try (TestSingleplayerContext world = context.worldBuilder().setUseConsistentSettings(false)
				.adjustSettings(s -> s.setSeed("silicon")).create()) {
			var server = world.getServer();
			server.runCommand("gamemode spectator @a");
			server.runCommand("time set noon");
			server.runCommand("gamerule doDaylightCycle false");
			server.runCommand("weather clear");
			context.runOnClient(mc -> {
				mc.options.hideGui = true;
				mc.options.renderDistance().set(12);
				mc.options.framerateLimit().set(260);
				mc.options.enableVsync().set(false);
				mc.options.inactivityFpsLimit().set(InactivityFpsLimit.MINIMIZED);
				if (!mc.getWindow().isFullscreen())
					mc.getWindow().toggleFullScreen();
			});
			server.runCommand("execute as @a at @s run tp @s ~ ~60 ~ 0 15");
			context.waitTicks(1200);

			for (int f : FACTORS) {
				factor = f;
				for (int round = 0; round < 2; round++) {
					for (Object[] mode : MODES) {
						SiliconConfig.enabled = (Boolean) mode[1];
						SiliconConfig.tolerance = (Integer) mode[2];
						SiliconConfig.fastMode = (Boolean) mode[3];
						context.runOnClient(mc -> mc.levelRenderer.allChanged());
						context.waitTicks(400);
						long total = 0;
						for (int s = 0; s < 15; s++) {
							context.waitTicks(10);
							total += context.computeOnClient(mc -> mc.getFps());
						}
						long[] t = Stats.totals();
						String size = context.computeOnClient(mc -> mc.getWindow().getWidth() + "x" + mc.getWindow().getHeight());
						System.out.println("[silicon-stress] x" + f + " | " + size + " | " + mode[0] + " | " + total / 15 + " fps"
								+ ((Boolean) mode[1] ? " | " + t[0] + " faces -> " + t[1] + " quads" : ""));
					}
				}
			}
			factor = 1;
			context.runOnClient(mc -> {
				if (mc.getWindow().isFullscreen())
					mc.getWindow().toggleFullScreen();
			});
			SiliconConfig.load();
		}
	}
}
