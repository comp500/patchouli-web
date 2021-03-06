package link.infra.patchouliweb;

import org.apache.logging.log4j.Logger;

import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

@Mod(modid = PatchouliWeb.MODID, name = PatchouliWeb.MODNAME, version = PatchouliWeb.VERSION, useMetadata = true, dependencies = "required-after:patchouli", clientSideOnly = true, guiFactory = "link.infra." + PatchouliWeb.MODID + ".config.OptionsFactory")
public class PatchouliWeb {

	public static final String MODID = "patchouliweb";
	public static final String MODNAME = "Patchouli Web";
	public static final String VERSION = "1.12.2-1.0.0";
	public static final String UNDERSCORENAME = "patchouli_web";

	@Mod.Instance
	public static PatchouliWeb instance;
	public static Logger logger;
	
	@SidedProxy(clientSide = "link.infra.patchouliweb.ClientProxy", serverSide = "link.infra.patchouliweb.CommonProxy")
	public static CommonProxy proxy;

	@Mod.EventHandler
	public void preInit(FMLPreInitializationEvent e) {
		logger = e.getModLog();
		proxy.preInit(e);
	}

	@Mod.EventHandler
	public void init(FMLInitializationEvent e) {
		proxy.init(e);
	}

	@Mod.EventHandler
	public void postInit(FMLPostInitializationEvent e) {
		proxy.postInit(e);
	}
	
	// Stack trace from "Java has been asked to exit" looks evil
	// This is an attempt to make it look less evil
	protected static void successExit_NOT_AN_ERROR() {
		FMLCommonHandler.instance().exitJava(0, false);
	}
	
}
