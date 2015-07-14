package jrtr.gldeferredrenderer;

import java.util.Iterator;
import javax.media.opengl.GL3;

import jrtr.Camera;
import jrtr.Frustum;
import jrtr.glrenderer.*;
import jrtr.Light;

/**
 * Sets up a shader and textures for deferred shading. A default implementation
 * that uses a simple default shader, which just passes through a texture from the 
 * framebuffer.
 * 
 * @author Heinrich Reich
 *
 */
public class DefaultSecondPassDrawer implements SecondPassDrawer{
	
	protected GLShader shader;
	protected GLDeferredRenderContext renderContext;
		
	public DefaultSecondPassDrawer(GLShader shader, GLDeferredRenderContext context){
		this.renderContext = context;
		this.shader = shader;
	}
	
	@Override
	public void bindTextures(GLDeferredRenderContext context) {
		// We do not use any extra textures here
	}

	@Override
	public void drawFinalTexture(GLDeferredRenderContext context) {
		context.drawTexture(0, context.getGBuffer().getColorBufferTexture(), "myTexture", shader, 0, 0, 1, 1);
	}

	@Override
	public void manageLights(GL3 gl, Iterator<Light> iterator) {
	}

	@Override
	public void managePerspective(GL3 gl, Camera cam, Frustum frust) {
	}
}
