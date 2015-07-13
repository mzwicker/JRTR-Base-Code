package jrtr.gldeferredrenderer;

import java.nio.IntBuffer;

import javax.media.opengl.GL3;

/**
 * A simple class for creating, reading and writing to an
 * OpenGL buffer.
 * @author Heinrich Reich
 *
 */
public abstract class GLBuffer {
	
	// references to the different OpenGL objects
	protected IntBuffer frameBuffer, drawBuffers, textures, depthBuffer;
	// format and render target index
	public final int format, renderTargets;
	// temporary indices of read and written fbo
	// width and height of the textures in this buffer
	private int prevWriteFBO, prevReadFBO, width, height;
	// whether to use depth buffer or not
	public final boolean useDepthBuffer;
	// referenc to the OpenGL context
	private GL3 gl;
	
	/**
	 * Creates a new OpenGL buffer.
	 * @param gl the OpenGL context
	 * @param width the width of the textures
	 * @param height the height of the texture
	 * @param renderTargets number of textures/render targets
	 * @param useDepthBuffer whether to use a depth buffer
	 * @param format the format
	 */
	public GLBuffer(GL3 gl, int width, int height, int renderTargets, boolean useDepthBuffer, int format){
		this.gl = gl;
		this.renderTargets = renderTargets;
		this.width = width;
		this.height = height;
		this.format = format;
		this.useDepthBuffer = useDepthBuffer;
		this.frameBuffer = IntBuffer.allocate(1);
		this.textures = IntBuffer.allocate(renderTargets);
		this.drawBuffers = IntBuffer.allocate(renderTargets);
		this.init();
	}
	
	/**
	 * Creates a new OpenGL buffer.
	 * @param gl
	 * @param width
	 * @param height
	 * @param renderTargets
	 * @param useDepthBuffer
	 */
	public GLBuffer(GL3 gl, int width, int height, int renderTargets, boolean useDepthBuffer){
		this(gl, width, height, renderTargets, useDepthBuffer, GL3.GL_RGB8);
	}
	
	/**
	 * Does the whole initialization step in OpenGL.
	 * First we generate a frame buffer and bind it as the current buffer.
	 * Then we create as much textures as were given in the constructor.
	 * If needed, we also create a depth buffer for the render target.
	 * The abstract method {@link GLBuffer#handleCreationError} gets called here.
	 */
	private void init(){
		gl.glGenFramebuffers(1, frameBuffer);
	    gl.glBindFramebuffer(GL3.GL_DRAW_FRAMEBUFFER, frameBuffer.get(0));
	    
	    // Create the for the fbo textures
	    gl.glGenTextures(textures.capacity(), textures);
	    
	    //Attach textures to fbo
	    for (int i = 0 ; i < textures.capacity() ; i++) {
	    	gl.glBindTexture(GL3.GL_TEXTURE_2D, this.textures.get(i));
	    	gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, /*GL3.GL_RGB32F*/format, width, height, 0, GL3.GL_RGB, GL3.GL_FLOAT, null);
	    	gl.glFramebufferTexture2D(GL3.GL_DRAW_FRAMEBUFFER, GL3.GL_COLOR_ATTACHMENT0 + i, GL3.GL_TEXTURE_2D, this.textures.get(i), 0);
	    }
	    
	    //depth texture
	    if(this.useDepthBuffer) this.createDepthBuffer();
	    
		for(int i = 0; i< drawBuffers.capacity(); i++)
	    	drawBuffers.array()[i] = GL3.GL_COLOR_ATTACHMENT0+i;
	    gl.glDrawBuffers(drawBuffers.capacity(), drawBuffers);
	    
	    this.handleCreationError(gl.glCheckFramebufferStatus(GL3.GL_FRAMEBUFFER) != GL3.GL_FRAMEBUFFER_COMPLETE);
	    
	    //Bind default render and frame buffer
	    gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, 0);
	    gl.glBindRenderbuffer(GL3.GL_RENDERBUFFER, 0);
	}
	
	/**
	 * Resizes the frame buffer, i.e. destroys the buffer and its textures,
	 * and re-initializes the buffer with the new dimensions.
	 * @param width
	 * @param height
	 */
	public void resize(int width, int height){
		this.width = width;
		this.height = height;
		this.dispose();
		this.init();
	}
	
	/**
	 * Gets called after the initialization.
	 * @param failed <code>true</code> if an error occurred during initialization step.
	 */
	protected abstract void handleCreationError(boolean failed);
	
	/**
	 * Binds this buffer, so any rendering calls result on it.
	 * Use {@link GLBuffer#endWrite()} if you are finished with writing.
	 */
	public void beginWrite(){
		this.prevWriteFBO = gl.getBoundFramebuffer(GL3.GL_DRAW_FRAMEBUFFER);
	    gl.glBindFramebuffer(GL3.GL_DRAW_FRAMEBUFFER, frameBuffer.get(0));
	    gl.glViewport(0, 0, width, height);
	}
	
	/**
	 * Unbinds this framebuffer, i.e. binds the previous buffer again.
	 * Only use this method if you called {@link GLBuffer#beginWrite()} before.
	 */
	public void endWrite(){
	    gl.glBindFramebuffer(GL3.GL_DRAW_FRAMEBUFFER, this.prevWriteFBO);
	}
	
	/**
	 * Begins reading from a texture from this buffer.
	 * Use {@link GLBuffer#endRead()} if you are done with reading.
	 * @param i the id of the wished texture.
	 */
	public void beginRead(int i){
		if(this.prevReadFBO != this.frameBuffer.get(0)){
			this.prevReadFBO = gl.getBoundFramebuffer(GL3.GL_READ_FRAMEBUFFER);
			gl.glBindFramebuffer(GL3.GL_READ_FRAMEBUFFER, frameBuffer.get(0));
		}
	    gl.glReadBuffer(GL3.GL_COLOR_ATTACHMENT0+i);
	}
	
	/**
	 * Ends reading from this buffer.
	 * Only use this method if you called {@link GLBuffer#beginRead(int)} before.
	 */
	public void endRead(){
	    gl.glBindFramebuffer(GL3.GL_READ_FRAMEBUFFER, this.prevReadFBO);
	}
	
	/**
	 * Does all OpenGL calls for creating a depth buffer for this buffer.
	 */
	private void createDepthBuffer(){
		this.depthBuffer = IntBuffer.allocate(1);
		gl.glGenTextures(1, this.depthBuffer);
		gl.glBindTexture(GL3.GL_TEXTURE_2D, this.depthBuffer.get(0));
		gl.glTexImage2D(GL3.GL_TEXTURE_2D, 0, GL3.GL_DEPTH_COMPONENT32F, width, height, 0, GL3.GL_DEPTH_COMPONENT, GL3.GL_FLOAT,  null);
		gl.glFramebufferTexture2D(GL3.GL_DRAW_FRAMEBUFFER, GL3.GL_DEPTH_ATTACHMENT, GL3.GL_TEXTURE_2D, this.depthBuffer.get(0), 0);
	}
	
	/**
	 * Disposes this buffer and its textures, i.e. releases all memory. 
	 */
	public void dispose(){
	    gl.glBindFramebuffer(GL3.GL_DRAW_FRAMEBUFFER, 0);
		if(this.useDepthBuffer) gl.glDeleteTextures(1, depthBuffer);
		gl.glDeleteTextures(textures.capacity(), textures);
		gl.glDeleteFramebuffers(1, frameBuffer);
	}
	
	/**
	 * @return the current set width.
	 */
	public int getWidth(){
		return this.width;
	}
	
	/**
	 * @return the current set height.
	 */
	public int getHeight(){
		return this.height;
	}
	
	/**
	 * @param index
	 * @return the internal texture index in OpenGL, need for passing textures to a shader.
	 */
	public int getTexture(int index){
		return this.textures.get(index);
	}

}
