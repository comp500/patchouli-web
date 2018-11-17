package link.infra.patchouliweb.page;

import java.lang.reflect.Field;

import link.infra.patchouliweb.PatchouliWeb;
import vazkii.patchouli.client.book.page.PageText;
import vazkii.patchouli.client.book.page.abstr.PageWithText;

public class TextHandler implements IPageHandler<PageText> {

	@Override
	public String processPage(PageText page, TextParser parser) {
		StringBuilder builder = new StringBuilder();
		String title = "", text = "";
		try {
			// yikes, disgusting reflection
			Field titleField = PageText.class.getDeclaredField("title");
			titleField.setAccessible(true);
			title = (String) titleField.get(page);
			Field textField = PageWithText.class.getDeclaredField("text");
			textField.setAccessible(true);
			text = (String) textField.get(page);
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			PatchouliWeb.logger.warn(e);
		}
		
		if (title != null && title.length() > 0) {
			builder.append("# ");
			builder.append(parser.processText(title));
			builder.append("\n\n");
		}
		if (text != null && text.length() > 0) {
			builder.append(parser.processText(text));
		}
		
		return builder.toString();
	}

}
