package link.infra.patchouliweb.render;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import link.infra.patchouliweb.PatchouliWeb;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ResourceProvider {
	
	private Map<String, ItemStack> stacks = new HashMap<String, ItemStack>();
	private Set<ResourceLocation> images = new HashSet<ResourceLocation>();
	
	// TODO: support more efficient formats?
	private static final String FILE_FORMAT = ".png";

	public String requestItemStack(ItemStack stack) {
		// Check stack doesn't already exist
		for (Entry<String, ItemStack> entry : stacks.entrySet()) {
			if (entry.getValue().equals(stack)) {
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
	
	public String requestImage(ResourceLocation loc) {
		images.add(loc);
		
		// ResourceLocations are unique, no need to add "_n"
		return "res_" + loc.toString().replaceAll("[^A-Za-z0-9-_ ]", "_");
	}
	
	public void renderAll(File outputFolder) {
		PatchouliWeb.logger.info("hello there");
		
		ItemStackRenderer renderer = new ItemStackRenderer();
		renderer.setUpRenderState(64);
		for (Entry<String, ItemStack> entry : stacks.entrySet()) {
			renderer.render(entry.getValue(), outputFolder, entry.getKey());
		}
		renderer.tearDownRenderState();
		
		// TODO: copy images
	}
	
}
