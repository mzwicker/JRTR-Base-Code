package jrtr;

import java.nio.IntBuffer;

import javax.media.opengl.GL3;

/**
 * TODO
 * 
 */
public class GLVertexArrayObject {

	private IntBuffer vao;
	private IntBuffer vbo;

	private GL3 gl;

	public GLVertexArrayObject(GL3 gl, int numberOfVBOs) {
		this.gl = gl;

		// For all vertex attributes, make vertex buffer objects
		vbo = IntBuffer.allocate(numberOfVBOs);
		gl.glGenBuffers(numberOfVBOs, vbo);

		// Make a vertex array object for this shape
		vao = IntBuffer.allocate(1);
		gl.glGenVertexArrays(1, vao);

		// bind the new (current) vertex array object
		bind();

	}

	public int getNextVBO() {
		return vbo.get();
	}

	public void bind() {
		gl.glBindVertexArray(vao.get(0));
	}

}