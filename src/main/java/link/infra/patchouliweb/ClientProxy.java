package link.infra.patchouliweb;

import link.infra.patchouliweb.page.TextHandler;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import vazkii.patchouli.client.book.BookContents;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageText;
import vazkii.patchouli.common.book.Book;
import vazkii.patchouli.common.book.BookRegistry;

public class ClientProxy extends CommonProxy {
	
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
	
	public void doPage(BookPage page) {
		if (page instanceof PageText) {
			new TextHandler().processPage((PageText) page);
		}
	}
	
	public void doCategory() {
		
	}

}
