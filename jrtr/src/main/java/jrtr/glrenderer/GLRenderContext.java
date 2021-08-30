package jrtr.glrenderer;

import java.util.*;
import static org.lwjgl.opengl.GL45.*;
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

/**
 * Implements a {@link RenderContext} (a renderer) using OpenGL
 * version 3 (or later). 
 */
public class GLRenderContext implements RenderContext {

	private SceneManagerInterface sceneManager;

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
	 */
	public GLRenderContext() {
		
		// Some OpenGL initialization
		glEnable(GL_DEPTH_TEST);
		glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

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
	public void display() {
		
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
		glUseProgram(activeShaderID);
		
		// Clear color and depth buffer for the new frame
		glClear(GL_COLOR_BUFFER_BIT);
		glClear(GL_DEPTH_BUFFER_BIT);
	}

	/**
	 * This method is called at the end of each frame, i.e., after scene drawing
	 * is complete.
	 */
	private void endFrame() {
		// Flush the OpenGL pipeline
		glFlush();
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
			glBindBuffer(GL_ARRAY_BUFFER, vertexData.getVAO().getNextVBO());

			// Tell OpenGL which "in" variable in the vertex shader corresponds
			// to the current vertex buffer object.
			// We use our own convention to name the variables, i.e.,
			// "position", "normal", "color", "texcoord", or others if
			// necessary.
			int attribIndex = -1;
			switch (e.getSemantic()) {
			case POSITION:
				attribIndex = glGetAttribLocation(activeShaderID, "position");
				break;
			case NORMAL:
				attribIndex = glGetAttribLocation(activeShaderID, "normal");
				break;
			case COLOR:
				attribIndex = glGetAttribLocation(activeShaderID, "color");
				break;
			case TEXCOORD:
				attribIndex = glGetAttribLocation(activeShaderID, "texcoord");
				break;
			}

			glVertexAttribPointer(attribIndex, dim, GL_FLOAT, false, 0,
					0);
			glEnableVertexAttribArray(attribIndex);
		}

		// Render the vertex buffer objects
		glDrawElements(GL_TRIANGLES, renderItem.getShape()
				.getVertexData().getIndices().length, GL_UNSIGNED_INT, 0);

		// We are done with this shape, bind the default vertex array
		glBindVertexArray(0);

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
		GLVertexArrayObject vao = new GLVertexArrayObject(data.getElements().size() + 1);
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
			glBindBuffer(GL_ARRAY_BUFFER, data.getVAO().getNextVBO());
			// Upload vertex data
			glBufferData(GL_ARRAY_BUFFER, e.getData(), GL_DYNAMIC_DRAW);
		}

		// Bind the default vertex buffer objects
		glBindBuffer(GL_ARRAY_BUFFER, 0);

		// Store the vertex data indices into the last vertex buffer
		glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, data.getVAO().getNextVBO());
		glBufferData(GL_ELEMENT_ARRAY_BUFFER, data.getIndices(), GL_DYNAMIC_DRAW);

		// Bind the default vertex array object. This "deactivates" the VAO
		// of the vertex data
		glBindVertexArray(0);		
	}

	private void setTransformation(Matrix4f transformation) {
		// Compute the modelview matrix by multiplying the camera matrix and
		// the transformation matrix of the object
		Matrix4f modelview = new Matrix4f(sceneManager.getCamera()
				.getCameraMatrix());
		modelview.mul(transformation);

		// Set modelview and projection matrices in shader
		glUniformMatrix4fv(
				glGetUniformLocation(activeShaderID, "modelview"), false,
				transformationToFloat16(modelview));
		glUniformMatrix4fv(glGetUniformLocation(activeShaderID,
				"projection"), false, transformationToFloat16(sceneManager
				.getFrustum().getProjectionMatrix()));


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
				glActiveTexture(GL_TEXTURE0);	// Work with texture unit 0
				glEnable(GL_TEXTURE_2D);
				glBindTexture(GL_TEXTURE_2D, ((GLTexture)m.diffuseMap).getId());
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
				glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
				// We assume the texture in the shader is called "myTexture"
				id = glGetUniformLocation(activeShaderID, "myTexture");
				glUniform1i(id, 0);	// The variable in the shader needs to be set to the desired texture unit, i.e., 0
			}
			
			// Pass a default light source to shader
			String lightString = "lightDirection[" + 0 + "]";			
			id = glGetUniformLocation(activeShaderID, lightString);
			if(id!=-1)
				glUniform4f(id, 0, 0, 1, 0.f);		// Set light direction
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
					id = glGetUniformLocation(activeShaderID, lightString);
					if(id!=-1)
						glUniform4f(id, l.direction.x, l.direction.y, l.direction.z, 0.f);		// Set light direction
					else
						System.out.print("Could not get location of uniform variable " + lightString + "\n");
					
					nLights++;
				}
				
				// Pass number of lights to shader, we assume this is in a variable "nLights" in the shader
				id = glGetUniformLocation(activeShaderID, "nLights");
				if(id!=-1)
					glUniform1i(id, nLights);		// Set number of lightrs
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
			glUseProgram(activeShaderID);
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
		return new GLShader();
	}

	public Texture makeTexture() {
		return new GLTexture();
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
