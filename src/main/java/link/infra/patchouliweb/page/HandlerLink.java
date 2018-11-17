package link.infra.patchouliweb.page;

import link.infra.patchouliweb.PatchouliWeb;
import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageLink;
import vazkii.patchouli.client.book.page.PageText;

public class HandlerLink extends HandlerText {
	
	@Override
	public String processPage(BookPage page, TextParser parser, ResourceProvider provider) {
		String existingText = super.processPage(page, parser, provider);
	
		PageLink pageLink = (PageLink) page;
		StringBuilder builder = new StringBuilder(existingText);
		String url = null, text = null;
		try {
			url = (String) ReflectionHelper.getPrivateValue(PageLink.class, pageLink, "url");
			text = (String) ReflectionHelper.getPrivateValue(PageLink.class, pageLink, "link_text");
		} catch (Exception e) {
			PatchouliWeb.logger.warn(e);
		}
		
		if (url != null && url.length() > 0 && text != null && text.length() > 0) {
			builder.append("\n\n### [");
			builder.append(text);
			builder.append("](");
			builder.append(url);
			builder.append(")\n\n");
		}
		
		return "";
	}
	
	@Override
	public boolean isSupported(BookPage page) {
		return page instanceof PageText;
	}
}
