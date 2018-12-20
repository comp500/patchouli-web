package link.infra.patchouliweb.page;

import java.lang.reflect.Constructor;

import link.infra.patchouliweb.PatchouliWeb;
import link.infra.patchouliweb.render.ResourceProvider;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import vazkii.patchouli.client.book.BookPage;
import vazkii.patchouli.client.book.page.PageEntity;
import vazkii.patchouli.client.book.page.abstr.PageWithText;

// This class does not currently work, see line 43
public class HandlerEntity implements IHandlerPage {

	@SuppressWarnings("unchecked")
	@Override
	public String processPage(BookPage page, TextParser parser, ResourceProvider provider) {
		PageEntity pageEntity = (PageEntity) page;
		StringBuilder builder = new StringBuilder();
		String title = null, text = null;
		Constructor<Entity> constructor = null;
		NBTTagCompound nbt = null;
		try {
			title = (String) ReflectionHelper.getPrivateValue(PageEntity.class, pageEntity, "name");
			text = (String) ReflectionHelper.getPrivateValue(PageWithText.class, pageEntity, "text");
			constructor = (Constructor<Entity>) ReflectionHelper.getPrivateValue(PageEntity.class, pageEntity, "constructor");
			nbt = (NBTTagCompound) ReflectionHelper.getPrivateValue(PageEntity.class, pageEntity, "nbt");
		} catch (Exception e) {
			PatchouliWeb.logger.warn(e);
		}
		
		if (title != null && title.length() > 0) {
			// TODO: make depth configurable
			builder.append("## ");
			builder.append(title);
			builder.append("\n");
		}
		
		if (constructor != null) {
			// TODO: Create a FakeWorld class, as most entity functions do not work with null (e.g. checking world.isRemote)
			World nullWorld = null; // For type checking
			Entity entity = null;
			try {
				entity = constructor.newInstance(nullWorld);
			} catch (Exception e) {
				PatchouliWeb.logger.error("Error initialising entity", e);
			}
			if (entity != null) {
				if (nbt != null) {
					try {
						entity.readFromNBT(nbt);
					} catch (Exception e) {
						PatchouliWeb.logger.error("Error reading entity NBT", e);
					}
				}
				// TODO: auto-sizing?
				
				if (title == null || title.length() == 0) {
					// TODO: make depth configurable
					builder.append("## ");
					builder.append(entity.getName());
					builder.append("\n");
				}
				
				builder.append("{{< image/container border=\"true\" >}}");
				builder.append("{{< image/item \"");
				builder.append(provider.requestEntity(entity));
				builder.append("\" >}}");
				builder.append("{{< /image/container >}}\n\n");
			}
		}
		
		if (text != null && text.length() > 0) {
			builder.append(parser.processText(text));
		}
		
		return builder.toString();
	}

	@Override
	public boolean isSupported(BookPage page) {
		return page instanceof PageEntity;
	}
}
