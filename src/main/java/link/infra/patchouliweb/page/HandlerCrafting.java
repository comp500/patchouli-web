package link.infra.patchouliweb.page;

import link.infra.patchouliweb.PatchouliWeb;
import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraftforge.common.crafting.IShapedRecipe;
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
	
	private void addRecipe(IRecipe recipe, StringBuilder builder, ResourceProvider provider, TextParser parser) {
		builder.append("{{< items/craftingrecipe >}}");
		// Put ingredients into 3x3 grid
		ItemStack[][][] stacks = new ItemStack[3][3][0];
		int width = 3;
		int shiftRow = 0;
		int shiftColumn = 0;
		if (recipe instanceof IShapedRecipe) {
			width = ((IShapedRecipe) recipe).getRecipeWidth();
			if (width == 1) {
				shiftColumn = 1; // Shift to middle so it looks better
			}
			if (((IShapedRecipe) recipe).getRecipeHeight() == 1) {
				shiftRow = 1; // Shift to middle so it looks better
			}
		} else if (recipe.getIngredients().size() == 1) {
			shiftRow = 1;
			shiftColumn = 1;
		}
		int currRow = shiftRow;
		int currColumn = shiftColumn;
		
		for (Ingredient ing : recipe.getIngredients()) {
			stacks[currRow][currColumn] = ing.getMatchingStacks();
			currColumn++;
			if (currColumn >= width) {
				currColumn = shiftColumn;
				currRow++;
			}
		}
		
		for (ItemStack[][] stacks1 : stacks) {
			builder.append("{{< items/reciperow >}}");
			for (ItemStack[] stacks2 : stacks1) {
				ItemStackUtils.addRecipeIngredient(stacks2, provider, builder, parser);
			}
			builder.append("{{< /items/reciperow >}}");
		}
		builder.append("{{< items/craftingrecipeoutput >}}");
		ItemStackUtils.addItemStack(recipe.getRecipeOutput(), provider, builder, parser);
		builder.append("{{< /items/craftingrecipeoutput >}}");
		builder.append("{{< /items/craftingrecipe >}}\n\n");
	}

	@Override
	public boolean isSupported(BookPage page) {
		return page instanceof PageCrafting;
	}
}
