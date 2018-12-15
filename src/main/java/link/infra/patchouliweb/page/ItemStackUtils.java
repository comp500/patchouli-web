package link.infra.patchouliweb.page;

import java.util.List;

import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;

public class ItemStackUtils {
	
	public static void addItemStack(ItemStack item, ResourceProvider provider, StringBuilder builder, TextParser parser) {
		addItemStack(item, provider, builder, parser, false);
	}
	
	public static void addItemStack(ItemStack item, ResourceProvider provider, StringBuilder builder, TextParser parser, boolean multi) {
		builder.append("{{% items/item \"");
		builder.append(provider.requestItemStack(item));
		if (multi) {
			builder.append("\" true ");
		} else {
			builder.append("\" false ");
		}
		builder.append(item.getCount());
		builder.append(" %}}\n");
		List<String> tooltip = item.getTooltip(null, ITooltipFlag.TooltipFlags.NORMAL);
		String tooltipString = "";
		for (String line : tooltip) {
			// Transform to work with existing text parser
			String transformedLine = line.replaceAll("§([\\da-fklmnor])", "\\$($1)").replace("$(r)", "$()") + "$()$(br)";
			tooltipString += parser.processText(transformedLine);
		}
		builder.append(tooltipString);
		builder.append("\n{{% /items/item %}}");
	}
	
	public static void addMultiItemStack(ItemStack[] items, ResourceProvider provider, StringBuilder builder, TextParser parser) {
		builder.append("{{< items/multi >}}");
		for (ItemStack item : items) {
			addItemStack(item, provider, builder, parser, true);
		}
		builder.append("{{< /items/multi >}}");
	}
	
	public static void addRecipeIngredient(ItemStack[] items, ResourceProvider provider, StringBuilder builder, TextParser parser) {
		if (items.length == 0) {
			builder.append("{{< items/empty >}}");
		} else {
			addMultiItemStack(items, provider, builder, parser);
		}
	}

}
