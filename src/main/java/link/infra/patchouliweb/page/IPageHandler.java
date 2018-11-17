package link.infra.patchouliweb.page;

import vazkii.patchouli.client.book.BookPage;

public interface IPageHandler<T extends BookPage> {

	//TODO: add resource provider support
	public String processPage(T page);
	
}
