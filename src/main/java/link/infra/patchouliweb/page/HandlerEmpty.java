package link.infra.patchouliweb.page;

import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraft.client.resources.I18n;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageEmpty;

public class HandlerEmpty implements IHandlerPage {

	@Override
	public String processPage(BookPage page, TextParser parser, ResourceProvider provider) {
		return I18n.format("label.patchouliweb.blank.name");
	}
	
	@Override
	public boolean isSupported(BookPage page) {
		return page instanceof PageEmpty;
	}

}
