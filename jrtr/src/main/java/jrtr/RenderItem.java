package jrtr;

import javax.vecmath.*;

/**
 * A data structure that contains a reference to a 3D object 
 * of class {@label Shape} and its transformation {@link Matrix4f}.
 * Its purpose is to pass data from the scene manager to the 
 * renderer via the {@link SceneManagerIterator}.
 */
public class RenderItem {

	public RenderItem(Shape shape, Matrix4f t)
	{
		this.shape = shape;
		this.t = t;
	}
	
	public Shape getShape()
	{
		return shape;
	}
	
	public Matrix4f getT()
	{
		return t;
	}
	
	private Shape shape;
	private Matrix4f t;
}
