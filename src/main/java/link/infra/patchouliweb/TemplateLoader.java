package link.infra.patchouliweb;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.fml.common.Loader;
import net.minecraftforge.fml.common.ModContainer;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class TemplateLoader {
	public File outputFolder;
	public File templateFolder;
	public static final String TEMPLATE_FOLDER = PatchouliWeb.UNDERSCORENAME + "_template";
	public static final String OUTPUT_FOLDER = PatchouliWeb.UNDERSCORENAME + "_output";
	
	// This is incredibly overcomplicated and bad.
	public static class FileLocation {
		private ResourceLocation loc = null;
		private Path path = null;
		
		public FileLocation(ResourceLocation loc) {
			this.loc = loc;
		}
		
		public FileLocation(Path path) {
			this.path = path;
		}
		
		public InputStream getStream() throws IOException {
			if (loc != null) {
				return Minecraft.getMinecraft().getResourceManager().getResource(loc).getInputStream();
			} else if (path != null) {
				return new FileInputStream(path.toString());
			} else {
				throw new RuntimeException("ohno, ResourceLocation or Path is null.");
			}
		}
	}
	
	Map<String, FileLocation> foundData = new HashMap<String, FileLocation>();
	
	public void loadTemplatesFromMod(ModContainer mod) {
		String id = mod.getModId();
		String templatesPath = "assets/" + id + "/" + TEMPLATE_FOLDER;
		CraftingHelper.findFiles(mod, templatesPath, (path) -> Files.exists(path), (path, file) -> {
			if (Files.isDirectory(file)) {
				// Exclude directories
				return true;
			}
			String fileStr = path.relativize(file).toString().replaceAll("\\\\", "/");
			ResourceLocation resLoc = new ResourceLocation(id, TEMPLATE_FOLDER + "/" + fileStr);
			FileLocation loc = new FileLocation(resLoc);
			foundData.put(fileStr, loc);
			
			return true;
		}, false, true);
	}
	
	public void loadTemplatesFromFolder() {
		try {
			Path templatePath = templateFolder.toPath();
			Files.walk(templatePath).filter(c -> !Files.isDirectory(c)).forEach(c -> {
				FileLocation loc = new FileLocation(c);
				String fileStr = templatePath.relativize(c).toString().replaceAll("\\\\", "/");
				foundData.put(fileStr, loc);
			});
		} catch (IOException e) {
			PatchouliWeb.logger.error("Error loading template from template folder", e);
		}
	}

	public void loadTemplates(FMLPreInitializationEvent e) {
		outputFolder = new File(e.getSuggestedConfigurationFile().getParentFile().getParentFile(), OUTPUT_FOLDER);
		if (!outputFolder.exists()) {
			outputFolder.mkdir();
		}
		if (!outputFolder.isDirectory()) {
			throw new RuntimeException(OUTPUT_FOLDER + " should be a directory! Delete the file now if it exists.");
		}
		templateFolder = new File(e.getSuggestedConfigurationFile().getParentFile().getParentFile(), TEMPLATE_FOLDER);
		if (!templateFolder.exists()) {
			templateFolder.mkdir();
		}
		if (!templateFolder.isDirectory()) {
			throw new RuntimeException(TEMPLATE_FOLDER + " should be a directory! Delete the file now if it exists.");
		}
		
		List<ModContainer> mods = new ArrayList<ModContainer>(Loader.instance().getActiveModList());
		// Use built in templates first
		ModContainer selfContainer = Loader.instance().activeModContainer();
		if (!mods.remove(selfContainer)) {
			PatchouliWeb.logger.error("Cannot remove ModContainer from list, loading templates failed!");
			return;
		}
		// Must be loaded from self first, so that templates can be overridden
		loadTemplatesFromMod(selfContainer);
		for (ModContainer mod : mods) {
			loadTemplatesFromMod(mod);
		}
		// Load from folder last, as it overwrites the previous
		loadTemplatesFromFolder();
	}
	
	public void cleanOutput() throws IOException {
		Files.walkFileTree(outputFolder.toPath(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	public void outputTemplates() throws IOException {
		for (Entry<String, FileLocation> entry : foundData.entrySet()) {
			InputStream stream = entry.getValue().getStream();
			Path target = outputFolder.toPath().resolve(entry.getKey());
			Files.createDirectories(target.getParent());
			Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
		}
	}
}
