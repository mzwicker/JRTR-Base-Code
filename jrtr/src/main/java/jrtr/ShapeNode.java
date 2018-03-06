package jrtr;

public class ShapeNode implements SceneNode {
	
	private Shape shape;
	
	public ShapeNode(Shape shape)
	{
		this.shape = shape;
	}
	
	public Shape getShape()
	{
		return shape;
	}
}