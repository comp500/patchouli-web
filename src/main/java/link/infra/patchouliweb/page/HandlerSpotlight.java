package link.infra.patchouliweb.page;

import link.infra.patchouliweb.PatchouliWeb;
import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageSpotlight;
import vazkii.patchouli.client.book.page.abstr.PageWithText;

public class HandlerSpotlight implements IHandlerPage {

	@Override
	public String processPage(BookPage page, TextParser parser, ResourceProvider provider) {
		PageSpotlight pageSpotlight = (PageSpotlight) page;
		StringBuilder builder = new StringBuilder();
		String title = null, text = null;
		ItemStack item = null;
		try {
			title = (String) ReflectionHelper.getPrivateValue(PageSpotlight.class, pageSpotlight, "title");
			text = (String) ReflectionHelper.getPrivateValue(PageWithText.class, pageSpotlight, "text");
			item = (ItemStack) ReflectionHelper.getPrivateValue(PageSpotlight.class, pageSpotlight, "itemStack");
			// TODO: handle link_recipe
		} catch (Exception e) {
			PatchouliWeb.logger.warn(e);
		}
		
		if (title != null && title.length() > 0) {
			builder.append("# ");
			builder.append(title);
			builder.append("\n\n");
		} else if (item != null) {
			builder.append("# ");
			builder.append(item.getDisplayName());
			builder.append("\n\n");
		}
		
		if (item != null) {
			builder.append("{{< spotlight \"");
			builder.append(provider.requestItemStack(item));
			builder.append("\" >}}");
		}
		
		if (text != null && text.length() > 0) {
			builder.append(parser.processText(text));
		}
		
		return builder.toString();
	}

	@Override
	public boolean isSupported(BookPage page) {
		return page instanceof PageSpotlight;
	}
}
