package jrtr;

import java.nio.IntBuffer;

import javax.media.opengl.GL3;

/**
 * A utility class to encapsulate an OpenGL "vertex array object" (VAO).
 */
public class GLVertexArrayObject {

	private IntBuffer vao;
	private IntBuffer vbo;

	private GL3 gl;

	/**
	 * Make an OpenGL "vertex array object" (VAO) with a desired number of
	 * "vertex buffer objects" (VBOs). Each VBO refers to a buffer 
	 * with one vertex attribute, like vertex positions, normals, or 
	 * texture coordinates.
	 * 
	 * @param gl
	 * 		the OpenGL rendering context to store the VAO
	 * 
	 * @param numberOfVBOs
	 * 		the number of VBOs to be stored in the VAO
	 */
	public GLVertexArrayObject(GL3 gl, int numberOfVBOs) {
		this.gl = gl;

		// For all vertex attributes, make vertex buffer objects.
		// References to the VBOs are stored in the array vbo.
		vbo = IntBuffer.allocate(numberOfVBOs);
		gl.glGenBuffers(numberOfVBOs, vbo);

		// Make a vertex array object. A reference to the VAO
		// is stored in the array vao.
		vao = IntBuffer.allocate(1);
		gl.glGenVertexArrays(1, vao);

	}

	/**
	 * Rewind the {@link IntBuffer} storing the references to the VBOs.
	 */
	public void rewindVBO() {
		vbo.rewind();
	}

	/**
	 * Get reference to next VBO stored in the {@link IntBuffer}.
	 */
	public int getNextVBO() {
		return vbo.get();
	}

	/**
	 * Bind the VAO. This means all the information associated
	 * with the VAO becomes active in OpenGL.
	 */
	public void bind() {
		gl.glBindVertexArray(vao.get(0));
	}

}