package link.infra.patchouliweb.page;

import link.infra.patchouliweb.PatchouliWeb;
import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageImage;
import vazkii.patchouli.client.book.page.abstr.PageWithText;

public class HandlerImage implements IHandlerPage {

	@Override
	public String processPage(BookPage page, TextParser parser, ResourceProvider provider) {
		PageImage pageImage = (PageImage) page;
		StringBuilder builder = new StringBuilder();
		String title = null, text = null;
		String[] images = null;
		Boolean border = null;
		boolean borderValue = false;
		try {
			title = (String) ReflectionHelper.getPrivateValue(PageImage.class, pageImage, "title");
			text = (String) ReflectionHelper.getPrivateValue(PageWithText.class, pageImage, "text");
			images = (String[]) ReflectionHelper.getPrivateValue(PageImage.class, pageImage, "images");
			border = (Boolean) ReflectionHelper.getPrivateValue(PageImage.class, pageImage, "border");
			// TODO: handle link_recipe
		} catch (Exception e) {
			PatchouliWeb.logger.warn(e);
		}
		
		if (border != null) {
			borderValue = border.booleanValue();
		}
		
		if (title != null && title.length() > 0) {
			// TODO: make depth configurable
			builder.append("## ");
			builder.append(title);
			builder.append("\n");
		}
		
		if (images != null) {
			builder.append("{{< image/container border=\"");
			builder.append(Boolean.toString(borderValue));
			builder.append("\" >}}");
			for (String image : images) {
				builder.append("{{< image/item \"");
				builder.append(provider.requestImage(new ResourceLocation(image)));
				builder.append("\" >}}");
			}
			builder.append("{{< /image/container >}}\n\n");
		}
		
		if (text != null && text.length() > 0) {
			builder.append(parser.processText(text));
		}
		
		return builder.toString();
	}

	@Override
	public boolean isSupported(BookPage page) {
		return page instanceof PageImage;
	}
}