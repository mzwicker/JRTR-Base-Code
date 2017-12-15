package jrtr.glrenderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ListIterator;
import java.util.Iterator;

import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import javax.vecmath.*;
import jrtr.*;
import jopenvr.JOpenVRLibrary;
import jopenvr.Texture_t;
import jopenvr.VR_IVRCompositor_FnTable;
import jrtr.gldeferredrenderer.FrameBuffer;

/**
 * This class implements a {@link RenderContext} (a renderer) using OpenGL
 * version 3 (or later).
 */
public class VRRenderContext implements RenderContext {

	private SceneManagerInterface sceneManager;
	private GL3 gl;

	/**
	 * The buffer containing the data that is passed to the HMD. 
	 */
	protected FrameBuffer vrBuffer;
	
	/**
	 * The default shader for this render context, will be used for items that
	 * do not have their own shader.
	 */
	private GLShader defaultShader;

	/**
	 * The id of the currently active shader (you should always
	 * useuseShader(GLShader) and useDefaultShader() to switch between the
	 * shaders!).
	 */
	private int activeShaderID;

	private static VR_IVRCompositor_FnTable vrcompositorFunctions;
	private Texture_t texType;
	private VRRenderPanel renderPanel;
	
	public void setOpenVR(VR_IVRCompositor_FnTable _vrcompositorFunctions, VRRenderPanel renderPanel)
	{
		vrcompositorFunctions = _vrcompositorFunctions;
		this.renderPanel = renderPanel;
		
		texType = new Texture_t();
		texType.eColorSpace = JOpenVRLibrary.EColorSpace.EColorSpace_ColorSpace_Gamma;
        texType.eType = JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL;
        texType.setAutoSynch(false);
        texType.setAutoRead(false);
        texType.setAutoWrite(false);
        texType.handle = -1;
        
        //we need to disable vertical synchronisation on the mirrored monitor on screen
        //The screen has 60 Hz and the HMD 90 Hz, thus vsync at 60Hz will block frames on the HMD periodically!
		gl.setSwapInterval(0);
		
	}
	
	/**
	 * This constructor is called by {@link GLRenderPanel}.
	 * 
	 * @param drawable
	 *            the OpenGL rendering context. All OpenGL calls are directed to
	 *            this object.
	 */
	public VRRenderContext(GLAutoDrawable drawable) {
		gl = drawable.getGL().getGL3();
		gl.glEnable(GL3.GL_DEPTH_TEST);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// Load and use default shader, will be used for items that do not have
		// their own shader.
		defaultShader = (GLShader) makeShader();
		try {
			defaultShader.load("../jrtr/shaders/default.vert", "../jrtr/shaders/default.frag");
		} catch (Exception e) {
			System.out.print("Problem with shader:\n");
			System.out.print(e.getMessage());
		}
		useShader(defaultShader);
		
		vrBuffer = new FrameBuffer(gl, drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), true);
	}

	public void resize(GLAutoDrawable drawable){
		this.vrBuffer.resize(drawable.getSurfaceWidth(), drawable.getSurfaceHeight());
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
	 * This method is called by the GLRenderPanel to redraw the 3D scene. The
	 * method traverses the scene using the scene manager and passes each object
	 * to the rendering method.
	 */
	public void display(GLAutoDrawable drawable) {
		
		if(!renderPanel.posesReady)
			renderPanel.waitGetPoses();
        
        // Save scene camera and projection matrices
        Matrix4f sceneCamera = new Matrix4f(this.sceneManager.getCamera().getCameraMatrix());
        Matrix4f projectionMatrix = new Matrix4f(this.sceneManager.getFrustum().getProjectionMatrix());
        
        // Render two eyes and pass to OpenVR compositor
        for(int eye=0; eye<2; eye++)
        {
	        // Applying tracking in addition to scene camera
	        Matrix4f worldToHead = new Matrix4f(renderPanel.poseMatrices[0]);
	        // Need to invert to get world-to-head
	        if(worldToHead.determinant()!=0)
	        	worldToHead.invert();
	        else
	        	System.out.println("tracking lost!");
	        worldToHead.mul(sceneCamera);
	        if(eye == 0) {
	        	renderPanel.headToLeftEye.mul(worldToHead);
	        	sceneManager.getCamera().setCameraMatrix(renderPanel.headToLeftEye);
	        } else {
	        	renderPanel.headToRightEye.mul(worldToHead);
	        	sceneManager.getCamera().setCameraMatrix(renderPanel.headToRightEye);
	        }
	        
	        // Set projection matrix
	        if(eye == 0)
	        	sceneManager.getFrustum().setProjectionMatrix(renderPanel.leftProjectionMatrix);
	        else
	        	sceneManager.getFrustum().setProjectionMatrix(renderPanel.rightProjectionMatrix); 
		
			//draw scene into framebuffer
			gl = drawable.getGL().getGL3();		
			vrBuffer.beginWrite();
			beginFrame();
			SceneManagerIterator iterator = sceneManager.iterator();
			while (iterator.hasNext()) {
				RenderItem r = iterator.next();
				if (r.getShape() != null) {
					draw(r);
				}
			}
			endFrame();
			vrBuffer.endWrite();
			
			// Draw the result to the screen using a bit of OpenGL hacking
			beginFrame();
			vrBuffer.beginRead(0);
			gl.glBlitFramebuffer(0, 0, vrBuffer.getWidth(), vrBuffer.getHeight(),  0, 0, 
				drawable.getSurfaceWidth(), drawable.getSurfaceHeight(), GL3.GL_COLOR_BUFFER_BIT, GL3.GL_LINEAR);
			vrBuffer.endRead();
			this.endFrame();
			
			// Pass rendered image to OpenVR compositor
			gl.glBindFramebuffer(GL3.GL_FRAMEBUFFER, vrBuffer.frameBuffer.get(0));
			texType.handle = vrBuffer.textures.get(0);
			texType.write();
	
			// Pass texture to the compositor
			if(eye == 0) {			
				int err	= vrcompositorFunctions.Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Left, texType, null,
		                  JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
				if( err != 0 ) System.out.println("Submit compositor error (left): " + Integer.toString(err));
			} else {
				int err	= vrcompositorFunctions.Submit.apply(JOpenVRLibrary.EVREye.EVREye_Eye_Right, texType, null,
		                JOpenVRLibrary.EVRSubmitFlags.EVRSubmitFlags_Submit_Default);
				if( err != 0 ) System.out.println("Submit compositor error (right): " + Integer.toString(err));
			}
			
			// Un-bind our frame buffer
			gl.glBindFramebuffer(GL3.GL_DRAW_FRAMEBUFFER, GL3.GL_NONE);
        }
		
        // Restore original scene camera and projection matrices
     	sceneManager.getCamera().setCameraMatrix(sceneCamera);
     	sceneManager.getFrustum().setProjectionMatrix(projectionMatrix);
     	
		// Not sure if this is useful...
		vrcompositorFunctions.PostPresentHandoff.apply();
		
		// We consumed the poses, get new ones for next frame
		renderPanel.posesReady = false;
	}

	/**
	 * This method is called at the beginning of each frame, i.e., before scene
	 * drawing starts.
	 */
	private void beginFrame() {
		gl.glClear(GL3.GL_COLOR_BUFFER_BIT | GL3.GL_DEPTH_BUFFER_BIT);
	}

	/**
	 * This method is called at the end of each frame, i.e., after scene drawing
	 * is complete.
	 */
	private void endFrame() {
		gl.glFlush();
	}

	/**
	 * The main rendering method.
	 * 
	 * @param renderItem
	 *            the object that needs to be drawn
	 */
	private void draw(RenderItem renderItem) {
		setMaterial(renderItem.getShape().getMaterial());

		GLVertexData vertexData = (GLVertexData) renderItem.getShape().getVertexData();

		// In the first pass the object has to be given to the buffer (on the
		// GPU) and the renderItem has to store the handle, so we do not have to
		// send the object to the GPU in each pass.
		if (vertexData.getVAO() == null) {
			initArrayBuffer(vertexData);
		}

		// Set modelview and projection matrices in shader (has to be done in
		// every step, since they usually have changed)
		setTransformation(renderItem.getT());

		// Bind the VAO of this shape (all the vertex data are already on the
		// GPU, we do not have to send them again)
		vertexData.getVAO().bind();
							
		// Try to connect the vertex arrays to the corresponding variables 
		// in the current vertex shader.
		// Note: This is not part of the vertex array object, because the active
		// shader may have changed since the vertex array object was initialized. 
		// We need to make sure the vertex buffers are connected to the right
		// variables in the shader
		ListIterator<VertexData.VertexElement> itr = vertexData.getElements().listIterator(0);
		vertexData.getVAO().rewindVBO();
		while (itr.hasNext()) {
			VertexData.VertexElement e = itr.next();
			int dim = e.getNumberOfComponents();

			// Bind the next vertex buffer object
			gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, vertexData.getVAO().getNextVBO());

			// Tell OpenGL which "in" variable in the vertex shader corresponds
			// to the current vertex buffer object.
			// We use our own convention to name the variables, i.e.,
			// "position", "normal", "color", "texcoord", or others if
			// necessary.
			int attribIndex = -1;
			switch (e.getSemantic()) {
			case POSITION:
				attribIndex = gl.glGetAttribLocation(activeShaderID, "position");
				break;
			case NORMAL:
				attribIndex = gl.glGetAttribLocation(activeShaderID, "normal");
				break;
			case COLOR:
				attribIndex = gl.glGetAttribLocation(activeShaderID, "color");
				break;
			case TEXCOORD:
				attribIndex = gl.glGetAttribLocation(activeShaderID, "texcoord");
				break;
			}

			gl.glVertexAttribPointer(attribIndex, dim, GL3.GL_FLOAT, false, 0, 0);
			gl.glEnableVertexAttribArray(attribIndex);
		}

		// Render the vertex buffer objects
		gl.glDrawElements(GL3.GL_TRIANGLES, renderItem.getShape().getVertexData().getIndices().length, GL3.GL_UNSIGNED_INT, 0);

		// we are done with this shape, bind the default vertex array
		gl.glBindVertexArray(0);

		cleanMaterial(renderItem.getShape().getMaterial());
	}

	/**
	 * A utility method to load vertex data into an OpenGL "vertex array object"
	 * (VAO) for efficient rendering.
	 *  
	 * @param data
	 * 			reference to the vertex data to be loaded into a VAO
	 */
	private void initArrayBuffer(GLVertexData data) {
		
		// Make a vertex array object (VAO) for this vertex data
		GLVertexArrayObject vao = new GLVertexArrayObject(gl, data.getElements().size() + 1);
	//	vertexArrayObjects.add(vao);
		data.setVAO(vao);
		
		// Bind (activate) the VAO for the vertex data
		vao.bind();
		
		// Store all vertex attributes in the buffers
		ListIterator<VertexData.VertexElement> itr = data.getElements()
				.listIterator(0);
		while (itr.hasNext()) {
			VertexData.VertexElement e = itr.next();

			// Bind the next vertex buffer object
			gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, data.getVAO().getNextVBO());
			// Upload vertex data
			gl.glBufferData(GL3.GL_ARRAY_BUFFER, e.getData().length * 4,
					FloatBuffer.wrap(e.getData()), GL3.GL_DYNAMIC_DRAW);

		}

		// bind the default vertex buffer objects
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

		// store the indices into the last buffer
		gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, data.getVAO().getNextVBO());
		gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER,
				data.getIndices().length * 4,
				IntBuffer.wrap(data.getIndices()), GL3.GL_DYNAMIC_DRAW);

		// bind the default vertex array object
		gl.glBindVertexArray(0);
	}

	private void setTransformation(Matrix4f transformation) {
		// Compute the modelview matrix by multiplying the camera matrix and
		// the transformation matrix of the object
		Matrix4f modelview = new Matrix4f(sceneManager.getCamera()
				.getCameraMatrix());
		modelview.mul(transformation);

		// Set modelview and projection matrices in shader
		gl.glUniformMatrix4fv(
				gl.glGetUniformLocation(activeShaderID, "modelview"), 1, false,
				transformationToFloat16(modelview), 0);
		gl.glUniformMatrix4fv(gl.glGetUniformLocation(activeShaderID,
				"projection"), 1, false, transformationToFloat16(sceneManager
				.getFrustum().getProjectionMatrix()), 0);

	}

	/**
	 * Pass the material properties to OpenGL, including textures and shaders.
	 * 
	 * Implementation here is incomplete. It's just for demonstration purposes. 
	 */
	private void setMaterial(Material m) {
		
		// Set up the shader for the material, if it has one
		if(m != null && m.shader != null) {
			useShader(m.shader);
			
			// Pass shininess parameter to shader 
			int id = gl.glGetUniformLocation(activeShaderID, "shininess");
			if(id!=-1)
				gl.glUniform1f(id, m.shininess);
			else
				System.out.print("Could not get location of uniform variable shininess\n");
			
			// Activate the texture, if the material has one
			if(m.texture != null) {
				// OpenGL calls to activate the texture 
				gl.glActiveTexture(GL3.GL_TEXTURE0);	// Work with texture unit 0
				gl.glEnable(GL3.GL_TEXTURE_2D);
				gl.glBindTexture(GL3.GL_TEXTURE_2D, ((GLTexture)m.texture).getId());
				gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
				gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
				id = gl.glGetUniformLocation(activeShaderID, "myTexture");
				gl.glUniform1i(id, 0);	// The variable in the shader needs to be set to the desired texture unit, i.e., 0
			}
			
			// Pass light source information to shader, iterate over all light sources
			Iterator<Light> iter = sceneManager.lightIterator();			
			int i=0;
			Light l;
			if(iter != null) {
				
				while(iter.hasNext() && i<8)
				{
					l = iter.next(); 
					
					// Pass light direction to shader
					String lightString = "lightDirection[" + i + "]";			
					id = gl.glGetUniformLocation(activeShaderID, lightString);
					if(id!=-1)
						gl.glUniform4f(id, l.direction.x, l.direction.y, l.direction.z, 0.f);		// Set light direction
					else
						System.out.print("Could not get location of uniform variable " + lightString + "\n");
					
					i++;
				}
				
				// Pass number of lights to shader
				id = gl.glGetUniformLocation(activeShaderID, "nLights");
				if(id!=-1)
					gl.glUniform1i(id, i);		// Set number of lightrs
				else
					System.out.print("Could not get location of uniform variable nLights\n");

			}

		}
	}	
	
	/**
	 * Disable a material.
	 * 
	 * To be implemented in the "Textures and Shading" project.
	 */
	private void cleanMaterial(Material m) {
	}

	public void useShader(Shader s) {
		if (s != null) {
			activeShaderID = ((GLShader)s).programId();
			gl.glUseProgram(activeShaderID);
		}
	}

	public Shader makeShader() {
		return new GLShader(gl);
	}

	public Texture makeTexture() {
		return new GLTexture(gl);
	}

	public VertexData makeVertexData(int n) {
		return new GLVertexData(n);
	}

	/**
	 * Convert a Transformation to a float array in column major ordering, as
	 * used by OpenGL.
	 */
	private static float[] transformationToFloat16(Matrix4f m) {
		float[] f = new float[16];
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
				f[j * 4 + i] = m.getElement(i, j);
		return f;
	}
	
	/**
	 * Disposes all disposables
	 */
	public void dispose(){
		this.vrBuffer.dispose();
	}

	@Override
	public void useDefaultShader() {
		// TODO Auto-generated method stub
		
	}
	
}

