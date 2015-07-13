package jrtr.gldeferredrenderer;

import java.util.HashMap;
import javax.media.opengl.GL3;
import jrtr.gldeferredrenderer.FrameBuffer;
import jrtr.gldeferredrenderer.GLDeferredRenderContext;

/**
 * A simple manager class for managing frame buffer objects which where created via a {@link GLDeferredRenderContext} instance.
 *
 * @author Heinrich Reich 
 */
public class FrameBufferManager {
	
	private HashMap<FrameBuffer, Boolean> fbos;
	
	public FrameBufferManager(){
		this.fbos = new HashMap<FrameBuffer, Boolean>();
	}
	
	public void dispose(GL3 gl){
	    gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
		for(FrameBuffer fbo: this.fbos.keySet())
			fbo.dispose();
	}
	
	public void resize(int width, int height){
		FrameBuffer[] f = new FrameBuffer[this.fbos.size()];
		this.fbos.keySet().toArray(f);
		for(FrameBuffer fbo: f){
			if(this.fbos.get(fbo)){
				fbo.resize(width, height);
			}
		}
		
	}
	
	public void addFrameBuffer(FrameBuffer fbo){
		this.addFrameBuffer(fbo, true);
	}
	
	/**
	 * Adds a frame buffer object to this manager.
	 * @param fbo frame buffer instance.
	 * @param resizeAuomatically if true the manager takes care of resizing the stored frame buffer objects.
	 * Otherwise you have to take care on resizing the fbo.
	 */
	public void addFrameBuffer(FrameBuffer fbo, boolean resizeAuomatically){
		this.fbos.put(fbo, resizeAuomatically);
	}
	
	public void removeFrameBuffer(FrameBuffer fbo){
		this.fbos.remove(fbo);
	}

}
