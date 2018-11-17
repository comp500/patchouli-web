package link.infra.patchouliweb.page;

import vazkii.patchouli.client.book.BookPage;

public interface IPageHandler {

	//TODO: add resource provider support
	public String processPage(BookPage page, TextParser parser);
	
}
