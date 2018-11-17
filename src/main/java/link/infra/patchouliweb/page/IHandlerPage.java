package link.infra.patchouliweb.page;

import link.infra.patchouliweb.render.ResourceProvider;
import vazkii.patchouli.client.book.BookPage;

public interface IHandlerPage {
	
	public String processPage(BookPage page, TextParser parser, ResourceProvider provider);
	
	public boolean isSupported(BookPage page);
	
}
