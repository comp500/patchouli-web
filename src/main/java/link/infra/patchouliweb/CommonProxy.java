package link.infra.patchouliweb;

import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

// Dummy class, Patchouli Web only works on the client (does nothing on server)
public class CommonProxy {

	public void preInit(FMLPreInitializationEvent e) {
		PatchouliWeb.logger.info(PatchouliWeb.MODNAME + " is disabled, as it only works on the client");
	}

	public void init(FMLInitializationEvent e) {
	}

	public void postInit(FMLPostInitializationEvent e) {
	}
}
