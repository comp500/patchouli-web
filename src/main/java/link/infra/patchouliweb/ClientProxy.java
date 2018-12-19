package link.infra.patchouliweb;

import java.io.IOException;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.EventPriority;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;

public class ClientProxy extends CommonProxy {
	TemplateLoader templateLoader;
	boolean shouldProcess = true;
	public static ClientProxy INSTANCE = null;
	
	public ClientProxy() {
		ClientProxy.INSTANCE = this;
	}
	
	@Override
	public void preInit(FMLPreInitializationEvent e) {
		if (!isEnabled()) {
			PatchouliWeb.logger.info("Patchouli Web is not enabled.");
			return;
		}
		MinecraftForge.EVENT_BUS.register(this);
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
			processor.processBooks();

			if (isRenderEnabled()) {
				processor.renderImages();
			}
			
			if (shouldQuitGame()) {
				// Our work is done, quit the game
				PatchouliWeb.successExit_NOT_AN_ERROR();
				return;
			}
			
			// If the game hasn't exited, prevent onFrameStart from being called again and wasting CPU cycles checking shouldProcess
			MinecraftForge.EVENT_BUS.unregister(this);
		}
	}
	
	// TODO: make these configurations
	public boolean isEnabled() { // merge with isrenderbuttonenabled/shouldquitgame??
		return true;
	}
	
	public boolean isRenderEnabled() {
		return true;
	}
	
	public boolean shouldQuitGame() {
		return false;
	}
	
	public boolean isRenderButtonEnabled() {
		return true;
	}
	
	public void startProcessing() {
		if (!shouldProcess) {
			shouldProcess = true;
			MinecraftForge.EVENT_BUS.register(this);
		}
	}

}
