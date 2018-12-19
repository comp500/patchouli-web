package link.infra.patchouliweb.gui;

import link.infra.patchouliweb.ClientProxy;
import link.infra.patchouliweb.PatchouliWeb;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.client.config.GuiButtonExt;
import net.minecraftforge.fml.client.config.GuiConfig;

public class OptionsGui extends GuiConfig {
	
	private boolean queueProcessing = false;

	public OptionsGui(GuiScreen parentScreen) {
		super(parentScreen, PatchouliWeb.MODID, PatchouliWeb.MODNAME);
	}
	
	@Override
	public void initGui() {
		super.initGui();
		int buttonWidth = mc.fontRenderer.getStringWidth(I18n.format("label.patchouliweb.renderbutton.name")) + 20;
		this.buttonList.add(new GuiButtonExt(0, 4, 2, buttonWidth, 20, I18n.format("label.patchouliweb.renderbutton.name")));
	}
	
	@Override
	protected void actionPerformed(GuiButton button) {
		super.actionPerformed(button);
		if (button.id == 0) {
			queueProcessing = true;
		}
	}
	
	@Override
	public void drawScreen(int mouseX, int mouseY, float partialTicks) {
		if (queueProcessing) {
			queueProcessing = false;
			// Draw background before hijacking the render to render items
			this.drawDefaultBackground();
			this.drawCenteredString(this.fontRenderer, I18n.format("label.patchouliweb.renderprogress.name"), this.width / 2, this.height / 2, 16777215);
			ClientProxy.INSTANCE.startProcessing();
		} else {
			super.drawScreen(mouseX, mouseY, partialTicks);
		}
	}
}
