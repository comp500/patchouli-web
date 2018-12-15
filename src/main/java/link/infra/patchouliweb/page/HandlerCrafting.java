package link.infra.patchouliweb.page;

import link.infra.patchouliweb.PatchouliWeb;
import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageCrafting;
import vazkii.patchouli.client.book.page.abstr.PageDoubleRecipe;
import vazkii.patchouli.client.book.page.abstr.PageWithText;

public class HandlerCrafting implements IHandlerPage {

	@Override
	public String processPage(BookPage page, TextParser parser, ResourceProvider provider) {
		PageCrafting pageCrafting = (PageCrafting) page;
		StringBuilder builder = new StringBuilder();
		String title1 = null, title2 = null, text = null;
		IRecipe recipe1 = null, recipe2 = null;
		try {
			recipe1 = (IRecipe) ReflectionHelper.getPrivateValue(PageDoubleRecipe.class, pageCrafting, "recipe1");
			recipe2 = (IRecipe) ReflectionHelper.getPrivateValue(PageDoubleRecipe.class, pageCrafting, "recipe2");
			title1 = (String) ReflectionHelper.getPrivateValue(PageDoubleRecipe.class, pageCrafting, "title1");
			title2 = (String) ReflectionHelper.getPrivateValue(PageDoubleRecipe.class, pageCrafting, "title2");
			text = (String) ReflectionHelper.getPrivateValue(PageWithText.class, pageCrafting, "text");
		} catch (Exception e) {
			PatchouliWeb.logger.warn(e);
		}
		
		if (title1 != null && title1.length() > 0) {
			builder.append("# ");
			builder.append(title1);
			builder.append("\n");
		}
		
		if (recipe1 != null) {
			addRecipe(recipe1, builder, provider);
		}
		
		if (title2 != null && title2.length() > 0 && !title2.equals("-")) {
			builder.append("# ");
			builder.append(title2);
			builder.append("\n");
		}
		
		if (recipe2 != null) {
			addRecipe(recipe2, builder, provider);
		}
		
		if (text != null && text.length() > 0) {
			builder.append(parser.processText(text));
		}
		
		return builder.toString();
	}
	
	// TODO: rewrite to show items in correct spaces
	private void addRecipe(IRecipe recipe, StringBuilder builder, ResourceProvider provider) {
		builder.append("{{< crafting/recipe \"");
		builder.append(provider.requestItemStack(recipe.getRecipeOutput()));
		builder.append("\" >}}");
		for (Ingredient ing : recipe.getIngredients()) {
			builder.append("{{< crafting/ingredient ");
			for (ItemStack stack : ing.getMatchingStacks()) {
				builder.append('"');
				builder.append(provider.requestItemStack(stack));
				builder.append("\" ");
			}
			builder.append(">}}");
		}
		builder.append("{{< /crafting/recipe >}}\n\n");
	}

	@Override
	public boolean isSupported(BookPage page) {
		return page instanceof PageCrafting;
	}
}
