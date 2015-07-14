package jrtr.glrenderer;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;

import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.vecmath.*;

import jrtr.Light;
import jrtr.Material;
import jrtr.RenderContext;
import jrtr.RenderItem;
import jrtr.SceneManagerInterface;
import jrtr.SceneManagerIterator;
import jrtr.Shader;
import jrtr.Texture;
import jrtr.VertexData;
import jrtr.VertexData.VertexElement;


/**
 * Implements a {@link RenderContext} (a renderer) using OpenGL
 * version 3 (or later).
 */
public class GLRenderContext implements RenderContext {

	private SceneManagerInterface sceneManager;
	private GL3 gl;

	/**
	 * The default shader for this render context.
	 */
	private GLShader defaultShader;

	/**
	 * The id of the currently active shader. Call useShader(Shader) and 
	 * useDefaultShader() to switch between shaders.
	 */
	private int activeShaderID;

	/**
	 * This constructor is called by {@link GLRenderPanel}.
	 * 
	 * @param drawable
	 *            the OpenGL rendering context. All OpenGL calls are directed to
	 *            this object.
	 */
	public GLRenderContext(GLAutoDrawable drawable) {
		
		// Some OpenGL initialization
		gl = drawable.getGL().getGL3();
		gl.glEnable(GL3.GL_DEPTH_TEST);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

		// Load and use the default shader
		defaultShader = (GLShader) makeShader();
		try {
			defaultShader.load("../jrtr/shaders/default.vert", "../jrtr/shaders/default.frag");
		} catch (Exception e) {
			System.out.print("Problem with shader:\n");
			System.out.print(e.getMessage());
		}
		useDefaultShader();
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
		
		// Get reference to the OpenGL rendering context
		gl = drawable.getGL().getGL3();

		// Do some processing at the beginning of the frame
		beginFrame();

		// Traverse scene manager and draw everything
		SceneManagerIterator iterator = sceneManager.iterator();
		while (iterator.hasNext()) {
			RenderItem r = iterator.next();
			if (r.getShape() != null) {
				draw(r);
			}
		}

		// Do some processing at the end of the frame
		endFrame();
	}

	/**
	 * This method is called at the beginning of each frame, i.e., before scene
	 * drawing starts.
	 */
	private void beginFrame() {
		// Set the active shader as default for this frame
		gl.glUseProgram(activeShaderID);
		
		// Clear color and depth buffer for the new frame
		gl.glClear(GL3.GL_COLOR_BUFFER_BIT);
		gl.glClear(GL3.GL_DEPTH_BUFFER_BIT);
	}

	/**
	 * This method is called at the end of each frame, i.e., after scene drawing
	 * is complete.
	 */
	private void endFrame() {
		// Flush the OpenGL pipeline
		gl.glFlush();
	}

	/**
	 * The main rendering method.
	 * 
	 * @param renderItem
	 *            the object that needs to be drawn
	 */
	private void draw(RenderItem renderItem) {
		
		// Set the material of the shape to be rendered
		setMaterial(renderItem.getShape().getMaterial());
		
		// Get reference to the vertex data of the render item to be rendered
		GLVertexData vertexData = (GLVertexData) renderItem.getShape()
				.getVertexData();

		// Check if the vertex data has been uploaded to OpenGL via a
		// "vertex array object" (VAO). The VAO will store the vertex data
		// in several "vertex buffer objects" (VBOs) on the GPU. We do this
		// only once for performance reasons. Once the data is in the VBOs
		// asscociated with a VAO, it is stored on the GPU and rendered more 
		// efficiently.
		if (vertexData.getVAO() == null) {
			initArrayBuffer(vertexData);
		}

		// Set modelview and projection matrices in shader (has to be done in
		// every step, since they usually have changed)
		setTransformation(renderItem.getT());

		// Bind the VAO of this shape. This activates the VBOs that we 
		// associated with the VAO. We already loaded the vertex data into the
		// VBOs on the GPU, so we do not have to send them again.
		vertexData.getVAO().bind();
		
		// Try to connect the vertex buffers to the corresponding variables 
		// in the current vertex shader.
		// Note: This is not part of the vertex array object, because the active
		// shader may have changed since the vertex array object was initialized. 
		// We need to make sure the vertex buffers are connected to the right
		// variables in the shader
		ListIterator<VertexData.VertexElement> itr = vertexData.getElements()
				.listIterator(0);
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
				attribIndex = gl
						.glGetAttribLocation(activeShaderID, "position");
				break;
			case NORMAL:
				attribIndex = gl.glGetAttribLocation(activeShaderID, "normal");
				break;
			case COLOR:
				attribIndex = gl.glGetAttribLocation(activeShaderID, "color");
				break;
			case TEXCOORD:
				attribIndex = gl
						.glGetAttribLocation(activeShaderID, "texcoord");
				break;
			}

			gl.glVertexAttribPointer(attribIndex, dim, GL3.GL_FLOAT, false, 0,
					0);
			gl.glEnableVertexAttribArray(attribIndex);
		}

		// Render the vertex buffer objects
		gl.glDrawElements(GL3.GL_TRIANGLES, renderItem.getShape()
				.getVertexData().getIndices().length, GL3.GL_UNSIGNED_INT, 0);

		// We are done with this shape, bind the default vertex array
		gl.glBindVertexArray(0);

		cleanMaterial(renderItem.getShape().getMaterial());
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
		
		// Make a vertex array object (VAO) for this vertex data
		// and store a reference to it
		GLVertexArrayObject vao = new GLVertexArrayObject(gl, data.getElements().size() + 1);
		data.setVAO(vao);
		
		// Bind (activate) the VAO for the vertex data in OpenGL.
		// The subsequent OpenGL operations on VBOs will be recorded (stored)
		// in the VAO.
		vao.bind();

		// Store all vertex attributes in vertex buffer objects (VBOs)
		ListIterator<VertexData.VertexElement> itr = data.getElements()
				.listIterator(0);
		data.getVAO().rewindVBO();
		while (itr.hasNext()) {
			VertexData.VertexElement e = itr.next();

			// Bind the vertex buffer object (VBO)
			gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, data.getVAO().getNextVBO());
			// Upload vertex data
			gl.glBufferData(GL3.GL_ARRAY_BUFFER, e.getData().length * 4,
					FloatBuffer.wrap(e.getData()), GL3.GL_DYNAMIC_DRAW);

		}

		// Bind the default vertex buffer objects
		gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, 0);

		// Store the vertex data indices into the last vertex buffer
		gl.glBindBuffer(GL3.GL_ELEMENT_ARRAY_BUFFER, data.getVAO().getNextVBO());
		gl.glBufferData(GL3.GL_ELEMENT_ARRAY_BUFFER,
				data.getIndices().length * 4,
				IntBuffer.wrap(data.getIndices()), GL3.GL_DYNAMIC_DRAW);

		// Bind the default vertex array object. This "deactivates" the VAO
		// of the vertex data
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
	 * Set up a material for rendering. Activate its shader, and pass the 
	 * material properties, textures, and light sources to the shader.
	 * 
	 * @param m
	 * 		the material to be set up for rendering
	 */
	private void setMaterial(Material m) {
		
		// Set up the shader for the material, if it has one
		if(m != null && m.shader != null) {
			
			// Identifier for shader variables
			int id;
			
			// Activate the shader
			useShader(m.shader);
			
			// Activate the diffuse texture, if the material has one
			if(m.diffuseMap != null) {
				// OpenGL calls to activate the texture 
				gl.glActiveTexture(GL3.GL_TEXTURE0);	// Work with texture unit 0
				gl.glEnable(GL3.GL_TEXTURE_2D);
				gl.glBindTexture(GL3.GL_TEXTURE_2D, ((GLTexture)m.diffuseMap).getId());
				gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MAG_FILTER, GL3.GL_LINEAR);
				gl.glTexParameteri(GL3.GL_TEXTURE_2D, GL3.GL_TEXTURE_MIN_FILTER, GL3.GL_LINEAR);
				// We assume the texture in the shader is called "myTexture"
				id = gl.glGetUniformLocation(activeShaderID, "myTexture");
				gl.glUniform1i(id, 0);	// The variable in the shader needs to be set to the desired texture unit, i.e., 0
			}
			
			// Pass a default light source to shader
			String lightString = "lightDirection[" + 0 + "]";			
			id = gl.glGetUniformLocation(activeShaderID, lightString);
			if(id!=-1)
				gl.glUniform4f(id, 0, 0, 1, 0.f);		// Set light direction
			else
				System.out.print("Could not get location of uniform variable " + lightString + "\n");
			int nLights = 1;
			
			// Iterate over all light sources in scene manager (overwriting the default light source)
			Iterator<Light> iter = sceneManager.lightIterator();			
			
			Light l;
			if(iter != null) {
				nLights = 0;
				while(iter.hasNext() && nLights<8)
				{
					l = iter.next(); 
					
					// Pass light direction to shader, we assume the shader stores it in an array "lightDirection[]"
					lightString = "lightDirection[" + nLights + "]";			
					id = gl.glGetUniformLocation(activeShaderID, lightString);
					if(id!=-1)
						gl.glUniform4f(id, l.direction.x, l.direction.y, l.direction.z, 0.f);		// Set light direction
					else
						System.out.print("Could not get location of uniform variable " + lightString + "\n");
					
					nLights++;
				}
				
				// Pass number of lights to shader, we assume this is in a variable "nLights" in the shader
				id = gl.glGetUniformLocation(activeShaderID, "nLights");
				if(id!=-1)
					gl.glUniform1i(id, nLights);		// Set number of lightrs
// Only for debugging				
//				else
//					System.out.print("Could not get location of uniform variable nLights\n");
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

	/**
	 * Activate and use a given shader.
	 * 
	 * @param s
	 * 		the shader to be activated
	 */
	public void useShader(Shader s) {
		if (s != null) {
			activeShaderID = ((GLShader)s).programId();
			gl.glUseProgram(activeShaderID);
		}
	}

	/**
	 * Activate the default shader.
	 * 
	 */
	public void useDefaultShader() {
		useShader(defaultShader);
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
}
