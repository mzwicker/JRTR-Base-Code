package jrtr.gldeferredrenderer;

import java.util.Iterator;
import javax.media.opengl.GL3;
import jrtr.Camera;
import jrtr.Frustum;
import jrtr.Light;

/**
 * An interface for applying a deferred shading pass. 
 * 
 * @author Heinrich Reich
 *
 */
public interface SecondPassDrawer {
	
	/**
	 * Binds all needed textures to a shader.
	 * @param context
	 */
	public void bindTextures(GLDeferredRenderContext context);
	
	/**
	 * Draws the final texture.
	 * @param context
	 */
	public void drawFinalTexture(GLDeferredRenderContext context);
	
	/**
	 * Makes sure that all lights get passed to the shaders. 
	 * @param gl
	 * @param iterator
	 */
	public void manageLights(GL3 gl, Iterator<Light> iterator);
	
	/**
	 * Manages the perspective of the view.
	 * You actually want to cache the camera and frustum coordinates,
	 * since they are overriden by the context when the second image gets rendered.
	 * @param gl
	 * @param cam
	 * @param frust
	 */
	public void managePerspective(GL3 gl, Camera cam, Frustum frust);

}
