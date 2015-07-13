package jrtr.gldeferredrenderer;

import jrtr.Camera;
import jrtr.Frustum;

/**
 * An abstraction of a post processor for deferred rendering.
 * @author Heinrich Reich
 */
public abstract class PostProcessor {
	
	protected final GLDeferredRenderContext context;
	
	public PostProcessor(GLDeferredRenderContext context){
		this.context = context;
		this.add();
	}
	
	/**
	 * Add this processor to the deferred context.
	 */
	public void add(){
		if(!this.context.containsProcessor(this))
			this.context.postProcessors.add(this);
	}
	
	/**
	 * Removes this processor from the deferred context.
	 */
	public void remove(){
		if(this.context.containsProcessor(this))
			this.context.postProcessors.remove(this);
	}
	
	public abstract void process();
	
	/**
	 * Gets called when the window gets resized.
	 * @param width new window width.
	 * @param height new window height.
	 */
	public abstract void resize(int width, int height);
	
	/**
	 * Has to be implemented if you want to cache the current camera position and frustum,
	 * since the render context switches the camera settings when rendering the whole image to the screen.
	 * @param cam
	 * @param frust
	 */
	public abstract void managePerspective(Camera cam, Frustum frust);

}
