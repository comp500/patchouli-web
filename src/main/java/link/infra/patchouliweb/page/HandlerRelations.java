package link.infra.patchouliweb.page;

import java.util.List;

import link.infra.patchouliweb.PatchouliWeb;
import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import vazkii.patchouli.client.book.BookEntry;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageRelations;
import vazkii.patchouli.client.book.page.abstr.PageWithText;

public class HandlerRelations implements IHandlerPage {

	@SuppressWarnings("unchecked")
	@Override
	public String processPage(BookPage page, TextParser parser, ResourceProvider provider) {
		PageRelations pageRelations = (PageRelations) page;
		StringBuilder builder = new StringBuilder();
		String title = null, text = null;
		List<BookEntry> entries = null;
		try {
			title = (String) ReflectionHelper.getPrivateValue(PageRelations.class, pageRelations, "title");
			text = (String) ReflectionHelper.getPrivateValue(PageWithText.class, pageRelations, "text");
			entries = (List<BookEntry>) ReflectionHelper.getPrivateValue(PageRelations.class, pageRelations, "entryObjs");
		} catch (Exception e) {
			PatchouliWeb.logger.warn(e);
		}
		
		if (title == null || title.length() == 0) {
			title = "Related Chapters";
		}
		builder.append("# ");
		builder.append(title);
		builder.append("\n\n");
		if (entries != null && entries.size() > 0) {
			for (BookEntry entry : entries) {
				builder.append("- ");
				// horrible
				// TODO: maybe refactor this to be less horrible?
				builder.append(parser.processText("$(l:" + entry.getResource() + ")" + entry.getName() + "$()"));
				builder.append('\n');
			}
			builder.append('\n');
		}
		if (text != null && text.length() > 0) {
			builder.append(parser.processText(text));
		}
		
		return builder.toString();
	}

	@Override
	public boolean isSupported(BookPage page) {
		return page instanceof PageRelations;
	}
}
