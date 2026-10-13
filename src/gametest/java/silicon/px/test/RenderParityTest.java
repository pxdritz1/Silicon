package silicon.px.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import silicon.px.SiliconConfig;
import silicon.px.debug.Stats;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Renders the same views with Silicon on and off. Rounding in the shader may move a colour channel by one
 * or two steps, and on cutout textures (leaves) a few edge texels can land on the other side of the alpha
 * cutoff. Anything beyond that counts as a failure.
 */
public class RenderParityTest implements FabricClientGameTest {
	private static final int TOLERANCE = 2;
	private static final int ALLOWED_PIXELS = 150;
	// Sodium stores texture coordinates in 15 bits, so its own per-block faces sample slightly off the texel grid
	// while merged faces sample exactly. Near texel edges the two pick neighbouring texels.
	private static final int ALLOWED_PIXELS_SODIUM = 1500;

	private static final String[][] VIEWS = {
			{"floor", "tp @p 8 -50 8 0 60"},
			{"walls", "tp @p 8 -55 -2 0 10"},
			{"logs", "tp @p 30 -56 -2 0 5"},
			{"far", "tp @p 0 -20 0 45 40"},
			{"grass", "tp @p 30 -50 0 0 50"},
			{"leaves", "tp @p -4 -54 12 0 20"},
	};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder().create()) {
			var server = world.getServer();
			server.runCommand("gamemode spectator @a");
			context.runOnClient(mc -> mc.options.hideGui = true);
			server.runCommand("time set noon");
			server.runCommand("gamerule doDaylightCycle false");
			server.runCommand("gamerule doMobSpawning false");
			server.runCommand("kill @e[type=!player]");
			server.runCommand("fill -16 -61 -16 48 -61 48 stone");
			server.runCommand("fill 0 -60 0 15 -55 0 cobblestone");
			server.runCommand("fill 0 -60 4 6 -60 10 dirt");
			server.runCommand("setblock 3 -59 6 torch");
			server.runCommand("fill 8 -58 4 14 -58 10 smooth_stone");
			server.runCommand("fill 24 -60 0 26 -58 0 oak_log[axis=x]");
			server.runCommand("fill 27 -60 0 29 -58 0 oak_log[axis=y]");
			server.runCommand("fill 30 -60 0 32 -58 0 oak_log[axis=z]");
			server.runCommand("fill 33 -60 0 35 -58 0 sandstone");
			server.runCommand("fill 36 -60 0 38 -58 0 crafting_table");
			server.runCommand("fill 39 -60 0 41 -58 0 observer[facing=up]");
			server.runCommand("fill 20 -60 6 40 -60 12 grass_block");
			server.runCommand("fill -12 -60 20 -4 -56 28 oak_leaves[persistent=true]");
			server.runCommand("fill -2 -60 20 4 -56 20 glass");
			world.getClientWorld().waitForChunksRender();

			StringBuilder failures = new StringBuilder();
			for (String[] view : VIEWS) {
				server.runCommand(view[1]);
				context.waitTicks(5);

				Path on = shot(context, world, true, false, view[0] + "_on");
				Path off = shot(context, world, false, false, view[0] + "_off");
				long bad = diff(on, off);
				System.out.println("[silicon] " + view[0] + ": " + bad + " pixels differ");
				if (bad > (FabricLoader.getInstance().isModLoaded("sodium") ? ALLOWED_PIXELS_SODIUM : ALLOWED_PIXELS))
					failures.append(view[0]).append(' ');
			}

			shot(context, world, true, true, "default_settings");
			long[] t = Stats.totals();
			System.out.println("[silicon] " + t[0] + " faces drawn as " + t[1] + " quads");

			SiliconConfig.fastMode = true;
			server.runCommand("tp @p -4 -54 12 0 20");
			shot(context, world, true, true, "fast_mode");
			t = Stats.totals();
			System.out.println("[silicon] fast mode: " + t[0] + " faces drawn as " + t[1] + " quads");

			SiliconConfig.load();
			if (!failures.isEmpty())
				throw new AssertionError("Silicon changes the picture in: " + failures);
		}
	}

	private static Path shot(ClientGameTestContext context, TestSingleplayerContext world, boolean enabled, boolean mergeRandom, String name) {
		SiliconConfig.enabled = enabled;
		SiliconConfig.mergeRandomTextures = mergeRandom;
		context.runOnClient(mc -> mc.levelRenderer.allChanged());
		context.waitTicks(10);
		world.getClientWorld().waitForChunksRender();
		context.waitTicks(20);
		return context.takeScreenshot(name);
	}

	private static long diff(Path a, Path b) {
		try {
			BufferedImage x = ImageIO.read(a.toFile());
			BufferedImage y = ImageIO.read(b.toFile());
			long n = 0;
			for (int i = 0; i < x.getWidth(); i++) {
				for (int j = 0; j < x.getHeight(); j++) {
					int p = x.getRGB(i, j), q = y.getRGB(i, j);
					for (int shift = 0; shift <= 16; shift += 8) {
						if (Math.abs(((p >> shift) & 255) - ((q >> shift) & 255)) > TOLERANCE) {
							n++;
							break;
						}
					}
				}
			}
			return n;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
