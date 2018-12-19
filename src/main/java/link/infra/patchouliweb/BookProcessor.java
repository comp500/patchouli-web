package link.infra.patchouliweb;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;

import link.infra.patchouliweb.page.HandlerCrafting;
import link.infra.patchouliweb.page.HandlerEmpty;
import link.infra.patchouliweb.page.HandlerImage;
import link.infra.patchouliweb.page.HandlerLink;
import link.infra.patchouliweb.page.HandlerRelations;
import link.infra.patchouliweb.page.HandlerSmelting;
import link.infra.patchouliweb.page.HandlerSpotlight;
import link.infra.patchouliweb.page.HandlerText;
import link.infra.patchouliweb.page.IHandlerPage;
import link.infra.patchouliweb.page.TextParser;
import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import vazkii.patchouli.client.book.BookCategory;
import vazkii.patchouli.client.book.BookContents;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

public class BookProcessor {
	TemplateLoader templateLoader;
	ResourceProvider provider = new ResourceProvider();
	
	public BookProcessor(TemplateLoader loader) {
		templateLoader = loader;
	}
	
	public void processBooks() {
		// Make sure that handlers added last are tested first
		Collections.reverse(pageHandlers);

		BookRegistry reg = BookRegistry.INSTANCE;
		for (Book book : reg.books.values()) {
			PatchouliWeb.logger.info("Book \"" + I18n.format(book.name) + "\" found, compiling...");
			doBook(book, provider);
		}
	}
	
	public void renderImages() {
		// Render ItemStacks and copy images
		provider.renderAll(templateLoader.outputFolder.toPath().resolve("static/images").toFile());
	}
	
	public static String traverseCategoryPath(BookCategory category) {
		String categoryName = category.getResource().getResourcePath();
		int slashIndex = categoryName.lastIndexOf('/');
		if (slashIndex != -1) {
			categoryName = categoryName.substring(slashIndex + 1);
		}
		
		if (category.isRootCategory()) {
			return categoryName;
		} else {
			return traverseCategoryPath(category.getParentCategory()) + "/" + categoryName;
		}
	}
	
	public static String traverseEntryPath(BookEntry entry) {
		String entryName = entry.getResource().getResourcePath();
		int slashIndex = entryName.lastIndexOf('/');
		if (slashIndex != -1) {
			entryName = entryName.substring(slashIndex + 1);
		}
		
		return traverseCategoryPath(entry.getCategory()) + "/" + entryName;
	}
	
	private Path resolveEntryPath(BookEntry entry) {
		String entryPath = traverseEntryPath(entry);
		Path path = Paths.get(templateLoader.outputFolder.toString(), "content/", entry.getBook().resourceLoc.getResourcePath(), entryPath + ".md");
		path.getParent().toFile().mkdirs(); // Make parent directories
		return path;
	}
	
	private Path resolveCategoryIndexPath(BookCategory category) {
		String categoryPath = traverseCategoryPath(category);
		Path path = Paths.get(templateLoader.outputFolder.toString(), "content/", category.getBook().resourceLoc.getResourcePath(), categoryPath);
		path.toFile().mkdirs(); // Make directories
		return path.resolve("_index.md");
	}
	
	private void doBook(Book book, ResourceProvider provider) {
		BookContents contents = book.contents;
		if (contents == null || contents.entries == null || contents.categories == null) {
			PatchouliWeb.logger.warn("Book not loaded yet!");
			return;
		}
		
		TextParser parser = new TextParser(book.macros, book);
		
		Map<BookEntry, Integer> orderMap = new HashMap<BookEntry, Integer>();
		Map<BookCategory, Integer> categoryOrderMap = new HashMap<BookCategory, Integer>();
		int j = 1;
		List<BookCategory> categories = contents.categories.values().stream().collect(Collectors.toList());
		Collections.sort(categories);
		for (BookCategory category : categories) {
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
			
			int i = 1;
			// Give w=0 to unordered entries
			for (BookEntry entry : entries) {
				orderMap.put(entry, i);
				i++;
			}
			
			i = 1;
			for (BookCategory subCategory : subCategories) {
				categoryOrderMap.put(subCategory, i);
				i++;
			}
			
			categoryOrderMap.put(category, j);
			j++;
		}
		
		for (BookEntry entry : contents.entries.values()) {
			doEntry(book, entry, orderMap.getOrDefault(entry, 0), parser, provider);
		}
		
		for (BookCategory category : contents.categories.values()) {
			createCategoryPage(category, book, parser, categoryOrderMap.getOrDefault(category, 0));
		}
		
		// Index page
		createIndexPage(book, parser);
	}
	
	private void createIndexPage(Book book, TextParser parser) {
		StringBuilder sb = new StringBuilder();
		
		JsonObject json = new JsonObject();
		json.addProperty("title", I18n.format(book.name));
		sb.append(json.toString());
		sb.append("\n\n");
		
		sb.append(parser.processText(I18n.format(book.landingText)));
		
		try {
			Path folderPath = Paths.get(templateLoader.outputFolder.toString(), "content/", book.resourceLoc.getResourcePath());
			folderPath.toFile().mkdirs();
			Path indexFilePath = folderPath.resolve("_index.md");
			List<String> lines = Arrays.asList(sb.toString().split("\n"));
			Files.write(indexFilePath, lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			PatchouliWeb.logger.error("Error saving book index page", e);
		}
	}
	
	private void createCategoryPage(BookCategory category, Book book, TextParser parser, int index) {
		StringBuilder sb = new StringBuilder();
		
		JsonObject json = new JsonObject();
		json.addProperty("title", I18n.format(category.getName()));
		json.addProperty("weight", index);
		sb.append(json.toString());
		sb.append("\n\n");
		
		sb.append(parser.processText(I18n.format(category.getDescription())));
		
		try {
			Path indexFilePath = resolveCategoryIndexPath(category);
			List<String> lines = Arrays.asList(sb.toString().split("\n"));
			Files.write(indexFilePath, lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			PatchouliWeb.logger.error("Error saving book category index page", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private void doEntry(Book book, BookEntry entry, int index, TextParser parser, ResourceProvider provider) {
		if (entry == null || book == null) {
			PatchouliWeb.logger.warn("Could not load BookEntry, book or entry is null!");
			return;
		}
		PatchouliWeb.logger.info("Entry \"" + I18n.format(entry.getName()) + "\" found \"" + entry.getResource().toString() + "\", compiling...");
		
		StringBuilder sb = new StringBuilder();
		JsonObject json = new JsonObject();
		json.addProperty("title", entry.getName());
		json.addProperty("weight", index);
		sb.append(json.toString());
		sb.append("\n\n");
		
		// entry.getPages() only provides unlocked pages
		List<BookPage> pages = (List<BookPage>) ReflectionHelper.getPrivateValue(BookEntry.class, entry, "realPages");
		for (BookPage page : pages) {
			sb.append(doPage(page, parser, provider));
			sb.append("\n\n");
		}
		try {
			Path path = resolveEntryPath(entry);
			List<String> lines = Arrays.asList(sb.toString().split("\n"));
			Files.write(path, lines, Charset.forName("UTF-8"));
		} catch (IOException e) {
			PatchouliWeb.logger.error("Error saving book entry", e);
		}
	}
	
	// yikes big generic
	public static final List<IHandlerPage> pageHandlers = new ArrayList<IHandlerPage>();
	
	static {
		pageHandlers.add(new HandlerText());
		pageHandlers.add(new HandlerImage());
		pageHandlers.add(new HandlerCrafting());
		pageHandlers.add(new HandlerSmelting());
		// TODO: multiblock page
		// TODO: entity page
		pageHandlers.add(new HandlerSpotlight());
		pageHandlers.add(new HandlerLink());
		pageHandlers.add(new HandlerRelations());
		pageHandlers.add(new HandlerEmpty());
	}
	
	private String doPage(BookPage page, TextParser parser, ResourceProvider provider) {
		// TODO: page anchors?
		
		for (IHandlerPage handler : pageHandlers) {
			if (handler.isSupported(page)) {
				return handler.processPage(page, parser, provider);
			}
		}
		String type = page.sourceObject.get("type").getAsString();
		PatchouliWeb.logger.info("Page type unsupported! Type: " + type);
		return "Page type unsupported! Type: " + type;
	}

}
