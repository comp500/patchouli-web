package link.infra.patchouliweb.render;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import link.infra.patchouliweb.PatchouliWeb;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.item.ItemStack;

/**
 * Portions of this code copied from https://github.com/elytra/BlockRenderer/
 * Licensed as follows:
	The MIT License (MIT)
	
	Copyright (c) 2016-2017 Una Thompson (unascribed) and contributors
	
	Permission is hereby granted, free of charge, to any person obtaining a copy of 
	this software and associated documentation files (the "Software"), to deal in 
	the Software without restriction, including without limitation the rights to 
	use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies 
	of the Software, and to permit persons to whom the Software is furnished to do 
	so, subject to the following conditions:
	
	The above copyright notice and this permission notice shall be included in all 
	copies or substantial portions of the Software.
	
	THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR 
	IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, 
	FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE 
	AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER 
	LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
	OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE 
	SOFTWARE.
 */

public class ItemStackRenderer {

	protected boolean render(ItemStack is, File folder, String fileName) {
		Minecraft mc = Minecraft.getMinecraft();
		GlStateManager.pushMatrix();
		GlStateManager.clearColor(0, 0, 0, 0);
		GlStateManager.clear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
		mc.getRenderItem().renderItemAndEffectIntoGUI(is, 0, 0);
		GlStateManager.popMatrix();
		try {
			/*
			 * We need to flip the image over here, because again, GL Y-zero is the bottom,
			 * so it's "Y-up". Minecraft's Y-zero is the top, so it's "Y-down". Since
			 * readPixels is Y-up, our Y-down render is flipped. It's easier to do this
			 * operation on the resulting image than to do it with GL transforms. Not
			 * faster, just easier.
			 */
			BufferedImage img = createFlipped(readPixels(size, size));

			File f = new File(folder, fileName + ".png");
			f.getParentFile().mkdirs();
			f.createNewFile();
			return ImageIO.write(img, "PNG", f);
		} catch (Exception ex) {
			PatchouliWeb.logger.catching(ex);
			return false;
		}
	}

	private int size;
	private float oldZLevel;

	protected void setUpRenderState(int desiredSize) {
		Minecraft mc = Minecraft.getMinecraft();
		ScaledResolution res = new ScaledResolution(mc);
		
		// --- NOTE: Maybe move to before/after rendering any progress bar thing?
		mc.getFramebuffer().unbindFramebuffer();
		GlStateManager.pushMatrix();
		/*
		 * As we render to the back-buffer, we need to cap our render size to be within
		 * the window's bounds. If we didn't do this, the results of our readPixels up
		 * ahead would be undefined. And nobody likes undefined behavior.
		 */
		size = Math.min(Math.min(mc.displayHeight, mc.displayWidth), desiredSize);

		// Switches from 3D to 2D
		mc.entityRenderer.setupOverlayRendering();
		RenderHelper.enableGUIStandardItemLighting();
		/*
		 * The GUI scale affects us due to the call to setupOverlayRendering above. As
		 * such, we need to counteract this to always get a 512x512 render. We could
		 * manually switch to orthogonal mode, but it's just more convenient to leverage
		 * setupOverlayRendering.
		 */
		float scale = size / (16f * res.getScaleFactor());
		GlStateManager.translate(0, 0, -(scale * 100));

		GlStateManager.scale(scale, scale, scale);

		oldZLevel = mc.getRenderItem().zLevel;
		mc.getRenderItem().zLevel = -50;

		GlStateManager.enableRescaleNormal();
		GlStateManager.enableColorMaterial();
		GlStateManager.enableDepth();
		GlStateManager.enableBlend();
		GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA,
				GL11.GL_ONE);
		GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		GlStateManager.disableAlpha();
	}

	protected void tearDownRenderState() {
		GlStateManager.disableLighting();
		GlStateManager.disableColorMaterial();
		GlStateManager.disableDepth();
		GlStateManager.disableBlend();

		Minecraft mc = Minecraft.getMinecraft();
		
		mc.getRenderItem().zLevel = oldZLevel;
		
		// --- NOTE: Maybe move to before/after rendering any progress bar thing?
		GlStateManager.popMatrix();
		mc.updateDisplay();
		/*
		 * While OpenGL itself is double-buffered, Minecraft is actually *triple*-buffered.
		 * This is to allow shaders to work, as shaders are only available in "modern" GL.
		 * Minecraft uses "legacy" GL, so it renders using a separate GL context to this
		 * third buffer, which is then flipped to the back buffer with this call.
		 */
		mc.getFramebuffer().bindFramebuffer(false);
	}

	private BufferedImage readPixels(int width, int height) throws InterruptedException {
		/*
		 * Make sure we're reading from the back buffer, not the front buffer. The front
		 * buffer is what is currently on-screen, and is useful for screenshots.
		 */
		GL11.glReadBuffer(GL11.GL_BACK);
		// Allocate a native data array to fit our pixels
		ByteBuffer buf = BufferUtils.createByteBuffer(width * height * 4);
		// And finally read the pixel data from the GPU...
		GL11.glReadPixels(0, Minecraft.getMinecraft().displayHeight - height, width, height, GL12.GL_BGRA,
				GL11.GL_UNSIGNED_BYTE, buf);
		// ...and turn it into a Java object we can do things to.
		BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		int[] pixels = new int[width * height];
		buf.asIntBuffer().get(pixels);
		img.setRGB(0, 0, width, height, pixels, 0, width);
		return img;
	}

	private static BufferedImage createFlipped(BufferedImage image) {
		AffineTransform at = new AffineTransform();
		/*
		 * Creates a compound affine transform, instead of just one, as we need to
		 * perform two transformations.
		 * 
		 * The first one is to scale the image to 100% width, and -100% height. (That's
		 * *negative* 100%.)
		 */
		at.concatenate(AffineTransform.getScaleInstance(1, -1));
		/**
		 * We then need to translate the image back up by it's height, as flipping it
		 * over moves it off the bottom of the canvas.
		 */
		at.concatenate(AffineTransform.getTranslateInstance(0, -image.getHeight()));
		return createTransformed(image, at);
	}

	private static BufferedImage createTransformed(BufferedImage image, AffineTransform at) {
		// Create a blank image with the same dimensions as the old one...
		BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
		// ...get it's renderer...
		Graphics2D g = newImage.createGraphics();
		/// ...and draw the old image on top of it with our transform.
		g.transform(at);
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return newImage;
	}

}
