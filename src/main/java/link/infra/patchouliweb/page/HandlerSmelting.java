package link.infra.patchouliweb.page;

import link.infra.patchouliweb.PatchouliWeb;
import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Tuple;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageSmelting;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipe;
import vazkii.patchouli.client.book.page.abstr.PageWithText;

public class HandlerSmelting implements IHandlerPage {

	@SuppressWarnings("unchecked")
	@Override
	public String processPage(BookPage page, TextParser parser, ResourceProvider provider) {
		PageSmelting pageSmelting = (PageSmelting) page;
		StringBuilder builder = new StringBuilder();
		String title1 = null, title2 = null, text = null;
		Tuple<ItemStack, ItemStack> recipe1 = null, recipe2 = null;
		try {
			recipe1 = (Tuple<ItemStack, ItemStack>) ReflectionHelper.getPrivateValue(PageDoubleRecipe.class, pageSmelting, "recipe1");
			recipe2 = (Tuple<ItemStack, ItemStack>) ReflectionHelper.getPrivateValue(PageDoubleRecipe.class, pageSmelting, "recipe2");
			title1 = (String) ReflectionHelper.getPrivateValue(PageDoubleRecipe.class, pageSmelting, "title1");
			title2 = (String) ReflectionHelper.getPrivateValue(PageDoubleRecipe.class, pageSmelting, "title2");
			text = (String) ReflectionHelper.getPrivateValue(PageWithText.class, pageSmelting, "text");
		} catch (Exception e) {
			PatchouliWeb.logger.warn(e);
		}
		
		if (title1 != null && title1.length() > 0) {
			// TODO: make depth configurable
			builder.append("## ");
			builder.append(title1);
			builder.append("\n");
		}
		
		if (recipe1 != null) {
			addRecipe(recipe1, builder, provider, parser);
		}
		
		if (title2 != null && title2.length() > 0 && !title2.equals("-")) {
			// TODO: make depth configurable
			builder.append("## ");
			builder.append(title2);
			builder.append("\n");
		}
		
		if (recipe2 != null) {
			addRecipe(recipe2, builder, provider, parser);
		}
		
		if (text != null && text.length() > 0) {
			builder.append(parser.processText(text));
		}
		
		return builder.toString();
	}
	
	private void addRecipe(Tuple<ItemStack, ItemStack> recipe, StringBuilder builder, ResourceProvider provider, TextParser parser) {
		builder.append("{{< items/smeltingrecipe >}}");
		ItemStackUtils.addItemStack(recipe.getFirst(), provider, builder, parser);
		ItemStackUtils.addItemStack(recipe.getSecond(), provider, builder, parser);
		builder.append("{{< /items/smeltingrecipe >}}\n\n");
	}

	@Override
	public boolean isSupported(BookPage page) {
		return page instanceof PageSmelting;
	}
}