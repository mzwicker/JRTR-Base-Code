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
		
	public DefaultSecondPassDrawer(GLDeferredRenderContext context){
		this.shader = GLUtils.loadShader("../jrtr/shaders/deferredShaders/default");
		this.renderContext = context;
	}
	
	@Override
	public void manageShader(GLDeferredRenderContext context) {
		context.useShader(this.shader);
		// Bind the color buffer from the g-buffer to texture unit 0, and pass
		// this to shader
		context.bindTexture(0, context.getGBuffer().getColorBufferTexture(), "myTexture", shader);
	}

	@Override
	public void manageLights(GL3 gl, Iterator<Light> iterator) {
	}

	@Override
	public void managePerspective(GL3 gl, Camera cam, Frustum frust) {
	}
}
