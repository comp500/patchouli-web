package link.infra.patchouliweb;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import link.infra.patchouliweb.page.IPageHandler;
import link.infra.patchouliweb.page.TextHandler;
import link.infra.patchouliweb.page.TextParser;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookContents;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageText;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

public class ClientProxy extends CommonProxy {
	protected File outputFolder;
	
	@Override
	public void preInit(FMLPreInitializationEvent e) {
		outputFolder = new File(e.getSuggestedConfigurationFile().getParentFile().getParentFile(), "patchouli_web_output");
		if (!outputFolder.exists()) {
			outputFolder.mkdir();
		}
		if (!outputFolder.isDirectory()) {
			throw new RuntimeException("patchouli_web_output should be a directory! Delete the file now if it exists.");
		}
	}
	
	@Override
	public void postInit(FMLPostInitializationEvent e) {
		if (!isEnabled()) {
			PatchouliWeb.logger.info("Patchouli Web is not enabled.");
			return;
		}
		PatchouliWeb.logger.info("Patchouli Web is enabled, starting compilation of books...");
		
		BookRegistry reg = BookRegistry.INSTANCE;
		for (Book book : reg.books.values()) {
			PatchouliWeb.logger.info("Book \"" + I18n.format(book.name) + "\" found, compiling...");
			doBook(book);
		}
		// Our work is done, quit the game
		FMLCommonHandler.instance().exitJava(0, false);
	}
	
	public boolean isEnabled() {
		return true;
	}
	
	private Path resolveEntryPath(ResourceLocation bookRes, ResourceLocation entryRes) {
		int slashIndex = entryRes.getResourcePath().lastIndexOf('/');
		String entryName = entryRes.getResourcePath();
		String entryPath = "";
		if (slashIndex != -1) {
			entryName = entryRes.getResourcePath().substring(slashIndex + 1);
			entryPath = entryRes.getResourcePath().substring(0, slashIndex);
		}
		Path folderPath = Paths.get(outputFolder.toString(), "content/page/", bookRes.getResourcePath(), entryPath);
		folderPath.toFile().mkdirs();
		return folderPath.resolve(entryName + ".md");
	}
	
	private Path resolveShortcodePath(String shortcodeName) {
		Path folderPath = Paths.get(outputFolder.toString(), "layouts/shortcodes/");
		folderPath.toFile().mkdirs();
		return folderPath.resolve(shortcodeName + ".html");
	}
	
	private String buildEntryFrontMatter(Book book, BookEntry entry, int entryIndex) {
		JsonObject json = new JsonObject();
		json.addProperty("title", entry.getName());
		json.addProperty("weight", entryIndex);
		return json.toString();
	}
	
	public void doBook(Book book) {
		BookContents contents = book.contents;
		if (contents == null || contents.entries == null || contents.categories == null) {
			PatchouliWeb.logger.warn("Book not loaded yet!");
			return;
		}
		
		TextParser parser = new TextParser(book.macros, book.resourceLoc.getResourcePath());
		
		Map<BookEntry, Integer> orderMap = new HashMap<BookEntry, Integer>();
		Map<BookCategory, Integer> categoryOrderMap = new HashMap<BookCategory, Integer>();
		for (BookCategory category : contents.categories.values()) {
			List<BookEntry> entries = contents.entries.values().stream()
					.filter((e) -> e.getCategory() != null)
					.filter((e) -> e.getCategory().equals(category))
					.collect(Collectors.toList());
			Collections.sort(entries);
			// TODO: check shouldHide? locked?
			List<BookCategory> subCategories = contents.categories.values().stream()
					.filter((c) -> c.getParentCategory() != null)
					.filter((c) -> c.getParentCategory().equals(category))
					.collect(Collectors.toList());
			Collections.sort(subCategories);
			
			int i = 1001; // Start at 1001, as categories should be at the top?
			// Give w=1000 to unordered entries
			for (BookEntry entry : entries) {
				orderMap.put(entry, i);
				i++;
			}
			
			i = 0;
			for (BookCategory subCategory : subCategories) {
				categoryOrderMap.put(subCategory, i);
				i++;
			}
		}
		
		for (BookEntry entry : contents.entries.values()) {
			doEntry(book, entry, orderMap.getOrDefault(entry, 1000), parser);
		}
		
		for (Entry<String, String> entry : parser.getAllTemplates().entrySet()) {
			doShortcode(entry.getKey(), entry.getValue());
		}
	}
	
	public void doEntry(Book book, BookEntry entry, int index, TextParser parser) {
		if (entry == null || book == null) {
			PatchouliWeb.logger.warn("Could not load BookEntry, book or entry is null!");
			return;
		}
		PatchouliWeb.logger.info("Entry \"" + I18n.format(entry.getName()) + "\" found \"" + entry.getResource().toString() + "\", compiling...");
		StringBuilder sb = new StringBuilder();
		sb.append(buildEntryFrontMatter(book, entry, index));
		sb.append("\n\n");
		for (BookPage page : entry.getPages()) {
			sb.append(doPage(page, parser));
			sb.append("\n\n");
		}
		try {
			Path path = resolveEntryPath(book.resourceLoc, entry.getResource());
			List<String> lines = Arrays.asList(sb.toString().split("\n"));
			Files.write(path, lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	// yikes big generic
	public static final Map<Class<? extends BookPage>, IPageHandler> pageHandlers = new HashMap<Class<? extends BookPage>, IPageHandler>();
	
	static {
		pageHandlers.put(PageText.class, new TextHandler());
	}
	
	public String doPage(BookPage page, TextParser parser) {
		// TODO: page anchors?
		
		for (Entry<Class<? extends BookPage>, IPageHandler> entry : pageHandlers.entrySet()) {
			if (entry.getKey().isInstance(page)) {
				return entry.getValue().processPage(page, parser);
			}
		}
		String type = page.sourceObject.get("type").getAsString();
		PatchouliWeb.logger.info("Page type unsupported! Type: " + type);
		return "Page type unsupported! Type: " + type;
	}
	
	public void doShortcode(String name, String contents) {
		try {
			Path path = resolveShortcodePath(name);
			List<String> lines = Arrays.asList(contents.split("\n"));
			Files.write(path, lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
