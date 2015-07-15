package jrtr.gldeferredrenderer;

import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.ListIterator;

import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import jrtr.glrenderer.*;
import jrtr.*;

/**
 * An OpenGL rendering context for rendering with deferred shading, followed by
 * optional post-processing.
 * @author Heinrich Reich
 *
 */
public class GLDeferredRenderContext implements RenderContext{
	
	/**
	 * Global GL context.
	 */
	public static GL3 gl;
	
	/**
	 * The default shader for this render context.
	 */
	public GLShader defaultGBufferShader, debugShader;
	
	private Quad quad;
	
	/**
	 * Our g-buffer.
	 */
	private GBuffer gBuffer;
	
	/**
	 * The buffer, containing the final texture. 
	 */
	private FrameBuffer finalBuffer;
	
	/**
	 * Temp. variables for re-doing the camera mode.
	 */
	private Vector3f tempPos = new Vector3f(), tempLookAt = new Vector3f(),
			orthoPos = new Vector3f(.5f,.5f,1f), orthoLookAT = new Vector3f(.5f,.5f,0f);
	private Matrix4f tempCameraMatrix = new Matrix4f();
	private Matrix4f tempProj = new Matrix4f();
	private boolean changedCameraMode = false;
	
	/**
	 * Debugging flag.
	 */
	public boolean debugGeometry = true;
	
	private SecondPassDrawer secondPassDrawer;
	ArrayList<PostProcessor> postProcessors;
	private ArrayList<GLVertexArrayObject> vertexArrayObjects = new ArrayList<GLVertexArrayObject>();
	
	private GLShader prevUsedShader = null;
	
	private int activeShaderID;
	protected SceneManagerInterface sceneManager;
	
	
	protected void init(int width, int height){
		gl.glEnable(GL3.GL_DEPTH_TEST);
		this.postProcessors = new ArrayList<PostProcessor>();
	
		// Initialize shaders
		this.defaultGBufferShader = GLUtils.loadShader("../jrtr/shaders/gBufferShaders/gBuffer");
		this.debugShader = GLUtils.loadShader("../jrtr/shaders/deferredShaders/default");
		
		// Initialize g-buffer and buffer for final image
		this.gBuffer = new GBuffer(gl, width, height);
		this.finalBuffer = new FrameBuffer(gl, width, height, false);
		
		// The quad to be drawn for deferred shading
		this.quad = new Quad();
		this.quad.setPosition(0f, 0f);
		this.quad.setScale(1f, 1f);
		
		// The deferred shading logic
		secondPassDrawer = new DirectionalDiffuseSecondPassDrawer(this);		
//		secondPassDrawer = new DefaultSecondPassDrawer(this);		

	}
		
	/**
	 * Resizes all render targets.
	 * @param drawable
	 */
	public void resize(GLAutoDrawable drawable){
		this.gBuffer.resize(drawable.getWidth(), drawable.getHeight());
		this.finalBuffer.resize(drawable.getWidth(), drawable.getHeight());
		for(PostProcessor proc: this.postProcessors)
			proc.resize(drawable.getWidth(), drawable.getHeight());
	}

	/**
	 * Call-back function to draw the scene. Performs deferred shading in two steps.
	 * First draw the scene to the g-buffer. Then let the post processors and the 
	 * second pass drawer manage the perspective. Set the camera into orthogonal mode.
	 * Start the second pass drawer. Start the post processors. Draw the final image 
	 * to the screen. Redo the camera mode.
	 * @param drawable
	 */
	public void display(GLAutoDrawable drawable){

		// Render to g-buffer
		this.renderToGBuffer(drawable);
	
		
		// Store the current camera for later use by deferred shader and post processors
		this.secondPassDrawer.managePerspective(gl, this.sceneManager.getCamera(), this.sceneManager.getFrustum());
		
		for(PostProcessor processor: this.postProcessors)
			processor.managePerspective(this.sceneManager.getCamera(), this.sceneManager.getFrustum());
						
		// Render a quad over the screen
		this.changeCameraMode();
		
		// Manage the lights, that is, pass the lights to the deferred shading logic
		this.secondPassDrawer.manageLights(gl, sceneManager.lightIterator());
		// Prepare the shader for deferred shading
		this.secondPassDrawer.manageShader(this);
		
		// Do the second drawing step using deferred shading
		this.finalBuffer.beginWrite();
		this.beginFrame();
		// Draw full screen quad to perform deferred shading on entire screen
		this.drawFullScreenQuad();
		this.endFrame();
		this.finalBuffer.endWrite();
		
		// Post process
		for(PostProcessor processor: this.postProcessors)
			processor.process();
		
		// Draw the result to the screen using a bit of OpenGL hacking
		this.beginFrame();
		this.finalBuffer.beginRead(0);
		gl.glBlitFramebuffer(0, 0, this.finalBuffer.getWidth(), this.finalBuffer.getHeight(),  0, 0, 
			drawable.getWidth(), drawable.getHeight(), GL3.GL_COLOR_BUFFER_BIT, GL3.GL_LINEAR);
		this.finalBuffer.endRead();
		
		// This draws the g-buffer to the screen for debugging
		if(this.debugGeometry)
			this.debugDraw();
		
		this.endFrame();
				
		// Restore the camera
		this.redoCameraMode();
	}

	Vector3f vTemp = new Vector3f();
	/**
	 * Renders the scene to the g-buffer.
	 * Just set the g-buffer as rendering target and 
	 * do the rendering process with the g-buffer shaders.
	 * @param drawable
	 */
	protected void renderToGBuffer(GLAutoDrawable drawable){
		// Bind the g-buffer and start writing into it
		this.gBuffer.beginWrite();		
		this.beginFrame();
		this.useShader(defaultGBufferShader);
		
		// Iterate over the scene and draw all objects
		SceneManagerIterator iterator = sceneManager.iterator();
		while (iterator.hasNext()) {
			RenderItem r = iterator.next();
			if (r.getShape() != null) {
				
				// Pass material properties to g-buffer shader
				// Note we are not activating the material shader, 
				// since we just want to draw into the g-buffer at 
				// this point. If the material doesn't have its own
				// texture, make sure to pass the default OpenGL 
				// texture 0, which is black
				Material m = r.getShape().getMaterial();
				if(m != null && m.diffuseMap != null)
					this.bindTexture(0, ((GLTexture)m.diffuseMap).getId(), "diffuseMap", (GLShader) this.defaultGBufferShader);			
				else
					this.bindTexture(0, 0, "diffuseMap", (GLShader) this.defaultGBufferShader);			

				draw(r);
			}
		}
		
		// Un-bind the g-buffer
		this.endFrame();
		this.gBuffer.endWrite();
	}
	
	/**
	 * Changes the camera mode, so we can draw a quad over the whole screen.
	 */
	private void changeCameraMode(){
		if(!this.changedCameraMode){
			this.changedCameraMode = true;
			// Save previous camera
			this.tempPos.set(this.sceneManager.getCamera().getCenterOfProjection());
			this.tempLookAt.set(this.sceneManager.getCamera().getLookAtPoint());
//			this.tempCameraMatrix = this.sceneManager.getCamera().getCameraMatrix();
			this.tempProj.set(this.sceneManager.getFrustum().getProjectionMatrix());
	
			// Make camera for second pass
			this.sceneManager.getCamera().setCenterOfProjection(this.orthoPos);
			this.sceneManager.getCamera().setLookAtPoint(this.orthoLookAT);
			Matrix4fUtils.setOrtho(this.sceneManager.getFrustum().getProjectionMatrix(), -.5f, .5f, -.5f, .5f, 0, 2);
		}
	}
	
	/**
	 * Restores the previous camera mode.
	 */
	private void redoCameraMode(){
		if(this.changedCameraMode){
			this.changedCameraMode = false;
			// Reset camera matrix
			this.sceneManager.getCamera().setCenterOfProjection(tempPos);
			this.sceneManager.getCamera().setLookAtPoint(tempLookAt);
//			this.sceneManager.getCamera().setCameraMatrix(this.tempCameraMatrix);
			this.sceneManager.getFrustum().getProjectionMatrix().set(tempProj);
		}
	}
	
	/**
	 * Draws the g-buffer to the screen for debugging.
	 */
	protected void debugDraw(){
		this.bindTexture(0, this.gBuffer.getDepthBufferTexture(), "myTexture", this.debugShader);
		this.drawScreenAlignedQuad(0, 0, .2f, .2f);
		this.bindTexture(0, this.gBuffer.getColorBufferTexture(), "myTexture", this.debugShader);
		this.drawScreenAlignedQuad(.2f, 0, .2f, .2f);
		this.bindTexture(0, this.gBuffer.getNormalBufferTexture(), "myTexture", this.debugShader);
		this.drawScreenAlignedQuad(.4f, 0, .2f, .2f);
		this.bindTexture(0, this.gBuffer.getUVBufferTexture(), "myTexture", this.debugShader);
		this.drawScreenAlignedQuad(.6f, 0, .2f, .2f);
	}
	
	/**
	 * Activates a given texture unit, binds a texture to it, and assigns the texture unit 
	 * to a specified texture in a shader.
	 * @param textureLocation the OpenGL texture unit that should be used for this texture.
	 * @param textureId the OpenGL reference to the texture to be bound.
	 * @param sampler2DName name of the texture in the shader program.
	 * @param shader the shader program object.
	 */
	public void bindTexture(int textureLocation, int textureId, String sampler2DName, GLShader shader){
		this.useShader(shader);
		gl.glActiveTexture(GL3.GL_TEXTURE0+textureLocation);
		gl.glEnable(GL3.GL_TEXTURE_2D);
		gl.glBindTexture(GL3.GL_TEXTURE_2D, textureId);
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
		gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
		gl.glUniform1i(gl.glGetUniformLocation(shader.programId(), sampler2DName), textureLocation);
	}
	
	/**
	 * Draws a screen aligned quad in the specified rectangular area (the whole
	 * screen is (0,0,1,1).
	 * @param x see {@link #drawScreenAlignedQuad(float, float, float, float)}
	 * @param y see {@link #drawScreenAlignedQuad(float, float, float, float)}
	 * @param width see {@link #drawScreenAlignedQuad(float, float, float, float)}
	 * @param height see {@link #drawScreenAlignedQuad(float, float, float, float)}
	 */
	public void drawScreenAlignedQuad(float x, float y, float width, float height){
		this.changeCameraMode();
		this.quad.setTransformation(x, y, width, height);
		this.draw(this.quad.getRenderItem());
		this.redoCameraMode();
	}
	
	/**
	 * Draws a quad on the whole screen.
	 */
	public void drawFullScreenQuad(){
		this.drawScreenAlignedQuad(0f, 0f, 1f, 1f);
	}
	
	/**
	 * Disposes all disposables
	 */
	public void dispose(){
		this.gBuffer.dispose();
		this.finalBuffer.dispose();
		for(GLVertexArrayObject vd: vertexArrayObjects){
			vd.dispose();
		}
	}
	
	/**
	 * @return our g-buffer object
	 */
	public GBuffer getGBuffer(){
		return this.gBuffer;
	}
	
	/**
	 * @return the final buffer containing the final texture.
	 */
	public FrameBuffer getFinalBuffer(){
		return this.finalBuffer;
	}
	
	/**
	 * Sets the second pass drawer for this context.
	 * Should only be set once.
	 * @param drawer
	 */
	public void setSecondPassDrawer(SecondPassDrawer drawer){
		this.secondPassDrawer = drawer;
	}
	
	/**
	 * Uses the given shader
	 * @param s
	 */
	public void useShader(Shader s) {
		if (s != null) {
			this.prevUsedShader = (GLShader) s;
			activeShaderID = ((GLShader)s).programId();
			gl.glUseProgram(activeShaderID);
		}
	}
	
	/**
	 * Uses the default shader.
	 */
	public void useDefaultShader() {
	}
	
	/**
	 * @param proc
	 * @return whether the given post processor has been posted already.
	 */
	public boolean containsProcessor(PostProcessor proc){
		return this.postProcessors.contains(proc);
	}

	/**
	 * Set the scene manager. The scene manager contains the 3D scene that will
	 * be rendered. The scene includes geometry as well as the camera and
	 * viewing frustum.
	 */
	public void setSceneManager(SceneManagerInterface sceneManager) {
		this.sceneManager = sceneManager;
	}

	/**
	 * This method is called at the beginning of each frame, i.e., before scene
	 * drawing starts. It clears the color and depth buffers.
	 */
	public void beginFrame() {
		//gl.glUseProgram(activeShaderID);
		gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
	}

	/**
	 * This method is called at the end of each frame, i.e., after scene drawing
	 * is complete.
	 */
	public void endFrame() {
		gl.glFlush();
	}

	/**
	 * The main rendering method to draw individual objects. Note that rendering will always
	 * (implicitly) draw into the currently bound framebuffer object (FBO).
	 * 
	 * @param renderItem the object that needs to be drawn
	 */
	protected void draw(RenderItem renderItem) {
		
		GLVertexData vertexData = ((GLVertexData) renderItem.getShape().getVertexData());
		if (vertexData.getVAO() == null) {
			initArrayBuffer(vertexData);
		}
		setTransformation(renderItem.getT());
		vertexData.getVAO().bind();
		ListIterator<VertexData.VertexElement> itr = vertexData.getElements().listIterator(0);
		vertexData.getVAO().rewindVBO();
		while (itr.hasNext()) {
			VertexData.VertexElement e = itr.next();
			int dim = e.getNumberOfComponents();
			gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexData.getVAO().getNextVBO());
			int attribIndex = -1;
			switch (e.getSemantic()) {
			case POSITION: attribIndex = gl.glGetAttribLocation(activeShaderID, "position"); break;
			case NORMAL: attribIndex = gl.glGetAttribLocation(activeShaderID, "normal"); break;
			case COLOR: attribIndex = gl.glGetAttribLocation(activeShaderID, "color"); break;
			case TEXCOORD: attribIndex = gl.glGetAttribLocation(activeShaderID, "texcoord"); break;
			}

			gl.glVertexAttribPointer(attribIndex, dim, GL3.GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(attribIndex);
		}
		gl.glDrawElements(GL3.GL_TRIANGLES, renderItem.getShape()
				.getVertexData().getIndices().length, GL3.GL_UNSIGNED_INT, 0);
		gl.glBindVertexArray(0);
	}
	
	/**
	 * A utility method to load vertex data into an OpenGL "vertex array object"
	 * (VAO) for efficient rendering. The VAO stores several "vertex buffer objects"
	 * (VBOs) that contain the vertex attribute data.
	 *  
	 * @param data
	 * 			reference to the vertex data to be loaded into a VAO
	 */
	private void initArrayBuffer(GLVertexData data) {
		GLVertexArrayObject vao = new GLVertexArrayObject(gl, data.getElements().size() + 1);
		vertexArrayObjects.add(vao);
		data.setVAO(vao);
		vao.bind();
		ListIterator<VertexData.VertexElement> itr = data.getElements().listIterator(0);
		data.getVAO().rewindVBO();
		while (itr.hasNext()) {
			VertexData.VertexElement e = itr.next();
			gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, data.getVAO().getNextVBO());
			gl.glBufferData(GL3.GL_ARRAY_BUFFER, e.getData().length * 4,
					FloatBuffer.wrap(e.getData()), GL3.GL_DYNAMIC_DRAW);

		}
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);
		gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, data.getVAO().getNextVBO());
		gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER,
				data.getIndices().length * 4,
				IntBuffer.wrap(data.getIndices()), GL3.GL_DYNAMIC_DRAW);
		gl.glBindVertexArray(0);
	}

	private final Matrix4f mTemp = new Matrix4f();
	
	/**
	 * Sets the perspective to the given transformation and makes sure that the previous camera settings are cached.
	 * @param transformation
	 */
	private void setTransformation(Matrix4f transformation) {
		this.mTemp.set(sceneManager.getCamera().getCameraMatrix());
		this.mTemp.mul(transformation);

		GLUtils.setUniformMatrix4f(this, this.prevUsedShader, "modelview", this.mTemp);
		GLUtils.setUniformMatrix4f(this, this.prevUsedShader, "projection", this.sceneManager.getFrustum().getProjectionMatrix());
	}

	@Override
	public Shader makeShader() {
		return GLUtils.makeShader();
	}

	@Override
	public Texture makeTexture() {
		return GLUtils.makeTexture();
	}

	@Override
	public VertexData makeVertexData(int n) {
		return GLUtils.makeVertexData(n);
	}
	
}
