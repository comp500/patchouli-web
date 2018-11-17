package link.infra.patchouliweb;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

import link.infra.patchouliweb.page.TextHandler;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import scala.actors.threadpool.Arrays;
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
	
	public void doBook(Book book) {
		PatchouliWeb.logger.info(I18n.format(book.subtitle));
		BookContents contents = book.contents;
		if (contents == null || contents.entries == null) {
			PatchouliWeb.logger.warn("Book not loaded yet!");
			return;
		}
		for (BookEntry entry : contents.entries.values()) {
			PatchouliWeb.logger.info("Entry \"" + I18n.format(entry.getName()) + "\" found, compiling...");
			for (BookPage page : entry.getPages()) {
				doPage(page);
			}
		}
	}
	
	int pageNum = 0;
	
	public void doPage(BookPage page) {
		if (page instanceof PageText) {
			String pageOutput = new TextHandler().processPage((PageText) page);
			try {
				Files.write(Paths.get(outputFolder.toString(), pageNum + ".md"), Arrays.asList(pageOutput.split("\n")), Charset.forName("UTF-8"));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pageNum++;
		}
	}
	
	public void doCategory() {
		
	}

}
