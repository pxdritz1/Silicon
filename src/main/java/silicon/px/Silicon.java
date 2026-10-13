package silicon.px;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.components.debug.DebugScreenEntries;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import silicon.px.debug.SiliconDebugEntry;

public class Silicon implements ClientModInitializer {
	public static final String MOD_ID = "silicon";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		SiliconConfig.load();
		DebugScreenEntries.register(Identifier.fromNamespaceAndPath(MOD_ID, "stats"), new SiliconDebugEntry());
	}
}
