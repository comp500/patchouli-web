package link.infra.patchouliweb;

import java.io.IOException;

import link.infra.patchouliweb.config.ConfigHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class ClientProxy extends CommonProxy {
	TemplateLoader templateLoader;
	boolean shouldProcess = false;
	public static ClientProxy INSTANCE = null;
	
	public ClientProxy() {
		ClientProxy.INSTANCE = this;
	}
	
	@Override
	public void preInit(FMLPreInitializationEvent e) {
		if (ConfigHandler.runOnStart) {
			startProcessing();
		}
		templateLoader = new TemplateLoader();
		templateLoader.loadTemplates(e);
	}
	
	@SubscribeEvent(priority = EventPriority.HIGHEST)
	public void onFrameStart(RenderTickEvent e) {
		if (e.phase == Phase.START && shouldProcess) {
			shouldProcess = false;
			
			PatchouliWeb.logger.info("Patchouli Web is enabled, starting compilation of books...");
			
			PatchouliWeb.logger.info("Cleaning output directory...");
			try {
				templateLoader.cleanOutput();
			} catch (IOException ex) {
				PatchouliWeb.logger.error("Error cleaning output directory: ", ex);
			}
			
			PatchouliWeb.logger.info("Copying template files...");
			try {
				templateLoader.outputTemplates();
			} catch (IOException ex) {
				PatchouliWeb.logger.error("Error writing template files: ", ex);
			}
			
			BookProcessor processor = new BookProcessor(templateLoader);
			PatchouliWeb.logger.info("Processing books...");
			processor.processBooks();

			if (ConfigHandler.renderImages) {
				PatchouliWeb.logger.info("Rendering images...");
				processor.renderImages();
			}
			
			PatchouliWeb.logger.info("Book rendering complete!");
			if (ConfigHandler.quitGame) {
				// Our work is done, quit the game
				PatchouliWeb.successExit_NOT_AN_ERROR();
				return;
			}
			
			// If the game hasn't exited, prevent onFrameStart from being called again and wasting CPU cycles checking shouldProcess
			MinecraftForge.EVENT_BUS.unregister(this);
		}
	}
	
	public void startProcessing() {
		if (!shouldProcess) {
			shouldProcess = true;
			MinecraftForge.EVENT_BUS.register(this);
		}
	}

}
