package link.infra.patchouliweb.render;

import java.io.File;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;

public class EntityRenderer {
	
	private RenderUtils utils = new RenderUtils();

	protected boolean render(Entity entity, File folder, String fileName) {
		//TODO: scale
		//TODO: rotate
		//TODO: offset
		
		Minecraft mc = Minecraft.getMinecraft();
		GlStateManager.pushMatrix();
		GlStateManager.clearColor(0, 0, 0, 0);
		GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		mc.getRenderManager().renderEntity(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F, false);
		GlStateManager.popMatrix();
		return RenderUtils.saveRegion(size, folder, fileName);
	}

	private int size;
	
	public void setup() {
		size = utils.setUpRenderState(512);
	}
	
	public void tearDown() {
		utils.tearDownRenderState();
	}

}
