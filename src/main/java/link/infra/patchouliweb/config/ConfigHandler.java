package link.infra.patchouliweb.config;

import link.infra.patchouliweb.PatchouliWeb;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.LangKey;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

@Config(modid = PatchouliWeb.MODID)
public class ConfigHandler {
	
	@Name("Compile books on game start")
	@LangKey("label.patchouliweb.config.runonstart")
	public static boolean runOnStart = true;
	
	@Name("Quit game after compiling books")
	@LangKey("label.patchouliweb.config.quitgame")
	public static boolean quitGame = false;
	
	@Name("Render images and items in books")
	@LangKey("label.patchouliweb.config.renderimages")
	public static boolean renderImages = true;

	@Mod.EventBusSubscriber
	private static class EventHandler {
		// Sync configuration to gui
		@SubscribeEvent
		public static void onConfigChanged(ConfigChangedEvent.OnConfigChangedEvent event) {
			if (event.getModID().equals(PatchouliWeb.MODID)) {
				ConfigManager.sync(PatchouliWeb.MODID, Config.Type.INSTANCE);
			}
		}
	}

}
