package link.infra.patchouliweb.render;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import link.infra.patchouliweb.PatchouliWeb;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ResourceProvider {
	
	private Map<String, ItemStack> stacks = new HashMap<String, ItemStack>();
	private Set<ResourceLocation> images = new HashSet<ResourceLocation>();
	private Map<String, Entity> entities = new HashMap<String, Entity>();
	
	// TODO: support more efficient formats?
	private static final String FILE_FORMAT = ".png";

	public String requestItemStack(ItemStack stack) {
		// Check stack doesn't already exist
		for (Entry<String, ItemStack> entry : stacks.entrySet()) {
			if (ItemStack.areItemStacksEqual(entry.getValue(), stack)) {
				return entry.getKey() + FILE_FORMAT;
			}
		}
		
		String fileName = "stack_" + stack.getItem().getRegistryName().toString().replaceAll("[^A-Za-z0-9-_ ]", "_");
		if (stacks.containsKey(fileName)) { // ensure no duplicates
			int i = 1; // start at _2
			do {
				i++;
			} while (stacks.containsKey(fileName + "_" + i));
			fileName = fileName + "_" + i;
		}
		
		stacks.put(fileName, stack.copy());
		return fileName + FILE_FORMAT;
	}
	
	private String getImageFileName(ResourceLocation loc) {
		String locString = loc.toString();
		int extIndex = locString.lastIndexOf('.');
		String ext = locString.substring(extIndex);
		locString = locString.substring(0, extIndex);
		// ResourceLocations are unique, no need to add "_n"
		return "res_" + locString.replaceAll("[^A-Za-z0-9-_ ]", "_") + ext;
	}
	
	public String requestImage(ResourceLocation loc) {
		images.add(loc);
		
		return getImageFileName(loc);
	}
	
	public String requestEntity(Entity entity) {
		// Check stack doesn't already exist
		for (Entry<String, Entity> entry : entities.entrySet()) {
			if (entity.isEntityEqual(entry.getValue())) {
				return entry.getKey() + FILE_FORMAT;
			}
		}
		
		String fileName = "entity_" + entity.getName().replaceAll("[^A-Za-z0-9-_ ]", "_");
		if (entities.containsKey(fileName)) { // ensure no duplicates
			int i = 1; // start at _2
			do {
				i++;
			} while (entities.containsKey(fileName + "_" + i));
			fileName = fileName + "_" + i;
		}
		
		entities.put(fileName, entity);
		return fileName + FILE_FORMAT;
	}
	
	public void renderAll(File outputFolder) {
		ItemStackRenderer stackRenderer = new ItemStackRenderer();
		stackRenderer.setup();
		for (Entry<String, ItemStack> entry : stacks.entrySet()) {
			stackRenderer.render(entry.getValue(), outputFolder, entry.getKey());
		}
		stackRenderer.tearDown();
		
		for (ResourceLocation image : images) {
			String fileName = getImageFileName(image);
			try {
				InputStream stream = Minecraft.getMinecraft().getResourceManager().getResource(image).getInputStream();
				Path target = outputFolder.toPath().resolve(fileName);
				Files.createDirectories(target.getParent());
				Files.copy(stream, target, StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				PatchouliWeb.logger.catching(e);
			}
		}
		
		EntityRenderer entityRenderer = new EntityRenderer();
		entityRenderer.setup();
		for (Entry<String, Entity> entry : entities.entrySet()) {
			entityRenderer.render(entry.getValue(), outputFolder, entry.getKey());
		}
		entityRenderer.tearDown();
	}
	
}
