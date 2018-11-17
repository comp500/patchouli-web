package link.infra.patchouliweb.page;

import link.infra.patchouliweb.render.ResourceProvider;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageEmpty;

public class HandlerEmpty implements IHandlerPage {

	@Override
	public String processPage(BookPage page, TextParser parser, ResourceProvider provider) {
		// TODO: maybe add "-- this page intentionally left blank --"??
		return ""; // Literally nothing
	}

	@Override
	public boolean isSupported(BookPage page) {
		return page instanceof PageEmpty;
	}

}
