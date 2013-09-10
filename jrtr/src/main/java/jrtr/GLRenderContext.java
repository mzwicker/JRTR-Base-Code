package jrtr;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ListIterator;

import javax.media.opengl.GL3;
import javax.media.opengl.GLAutoDrawable;
import javax.vecmath.*;


/**
 * This class implements a {@link RenderContext} (a renderer) using OpenGL
 * version 3 (or later).
 */
public class GLRenderContext implements RenderContext {

	private SceneManagerInterface sceneManager;
	private GL3 gl;

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

	/**
	 * This constructor is called by {@link GLRenderPanel}.
	 * 
	 * @param drawable
	 *            the OpenGL rendering context. All OpenGL calls are directed to
	 *            this object.
	 */
	public GLRenderContext(GLAutoDrawable drawable) {
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
		gl = drawable.getGL().getGL3();

		beginFrame();

		SceneManagerIterator iterator = sceneManager.iterator();
		while (iterator.hasNext()) {
			RenderItem r = iterator.next();
			if (r.getShape() != null) {
				draw(r);
			}
		}

		endFrame();
	}

	/**
	 * This method is called at the beginning of each frame, i.e., before scene
	 * drawing starts.
	 */
	private void beginFrame() {
		setLights();

		gl.glClear(GL3.GL_COLOR_BUFFER_BIT);
		gl.glClear(GL3.GL_DEPTH_BUFFER_BIT);
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

		GLVertexData vertexData = (GLVertexData) renderItem.getShape()
				.getVertexData();

		// In the first pass the object has to be given to the buffer (on the
		// GPU) and the renderItem has to store the handle, so we do not have to
		// send the object to the GPU in each pass.
		if (vertexData.getVAO() == null) {
			initArrayBuffer(vertexData);
		}

		// Set modelview and projection matrices in shader (has to be done in
		// every step, since they usually have changed)
		setTransformation(renderItem.getT());

		// bind the VAO of this shape (all the vertex data are already on the
		// GPU, we do not have to send them again)
		vertexData.getVAO().bind();

		// Render the vertex buffer objects
		gl.glDrawElements(GL3.GL_TRIANGLES, renderItem.getShape()
				.getVertexData().getIndices().length, GL3.GL_UNSIGNED_INT, 0);

		// we are done with this shape, bind the default vertex array
		gl.glBindVertexArray(0);

		cleanMaterial(renderItem.getShape().getMaterial());
	}

	private void initArrayBuffer(GLVertexData data) {
		// Make a vertex array object (VAO) for this shape
		data.setVAO(new GLVertexArrayObject(gl, data.getElements().size() + 1));

		// Store all vertex attributes in the buffers
		ListIterator<VertexData.VertexElement> itr = data.getElements()
				.listIterator(0);
		while (itr.hasNext()) {
			VertexData.VertexElement e = itr.next();
			int dim = e.getNumberOfComponents();

			// Bind the next vertex buffer object
			gl.glBindBuffer(GL3.GL_ARRAY_BUFFER, data.getVAO().getNextVBO());
			// Upload vertex data
			gl.glBufferData(GL3.GL_ARRAY_BUFFER, e.getData().length * 4,
					FloatBuffer.wrap(e.getData()), GL3.GL_DYNAMIC_DRAW);

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
	 * Pass the light properties to OpenGL. This assumes the list of lights in
	 * the scene manager is accessible via a method Iterator<Light>
	 * lightIterator().
	 * 
	 * To be implemented in the "Textures and Shading" project.
	 */
	private void setLights() {
	}

	/**
	 * Pass the material properties to OpenGL, including textures and shaders.
	 * 
	 * To be implemented in the "Textures and Shading" project.
	 */
	private void setMaterial(Material m) {
	}

	/**
	 * Disable a material.
	 * 
	 * To be implemented in the "Textures and Shading" project.
	 */
	private void cleanMaterial(Material m) {
	}

	private void useShader(GLShader s) {
		if (s != null) {
			activeShaderID = s.programId();
			s.use();
		}
	}

	private void useDefaultShader() {
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
