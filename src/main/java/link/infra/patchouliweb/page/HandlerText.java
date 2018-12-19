package link.infra.patchouliweb.page;

import link.infra.patchouliweb.PatchouliWeb;
import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageText;
import vazkii.patchouli.client.book.page.abstr.PageWithText;

public class HandlerText implements IHandlerPage {

	@Override
	public String processPage(BookPage page, TextParser parser, ResourceProvider provider) {
		PageText pageText = (PageText) page;
		StringBuilder builder = new StringBuilder();
		String title = null, text = null;
		try {
			// yikes, disgusting reflection
			title = (String) ReflectionHelper.getPrivateValue(PageText.class, pageText, "title");
			text = (String) ReflectionHelper.getPrivateValue(PageWithText.class, pageText, "text");
		} catch (Exception e) {
			PatchouliWeb.logger.warn(e);
		}
		
		if (title != null && title.length() > 0) {
			// TODO: make depth configurable
			builder.append("## ");
			builder.append(title);
			builder.append("\n\n");
		}
		if (text != null && text.length() > 0) {
			builder.append(parser.processText(text));
		}
		
		return builder.toString();
	}

	@Override
	public boolean isSupported(BookPage page) {
		return page instanceof PageText;
	}

}
