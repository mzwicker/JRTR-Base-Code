package jrtr;

import javax.media.opengl.GL3;

/**
 * An implementation of {@link VertexData} which adds the handle to the OpenGL
 * vertex array object (VAO) of this vertex data to it.
 */
public class GLVertexData extends VertexData {

	/**
	 * The handle to the OpenGL VAO of this data.
	 */
	private GLVertexArrayObject vertexArrayObject;

	public GLVertexData(int n) {
		super(n);
		vertexArrayObject = null;
	}

	public GLVertexArrayObject getVAO() {
		return vertexArrayObject;
	}

	public void setVAO(GLVertexArrayObject vertexArrayObject) {
		this.vertexArrayObject = vertexArrayObject;
	}
}
