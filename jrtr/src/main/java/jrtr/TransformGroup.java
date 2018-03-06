package jrtr;

import javax.vecmath.Matrix4f;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

/*
 * Implements a group node that stores a transformation. The transformation 
 * applies to the whole subtree, at whose root the node sits.
 */
public class TransformGroup implements SceneNode {

	public Matrix4f transformation;
	public Collection<SceneNode> children;
	
	public TransformGroup()
	{
		transformation = new Matrix4f();
		transformation.setIdentity();
		children = new LinkedList<SceneNode>();
	}
	
	public Matrix4f getTransformation()
	{
		return transformation;
	}
	
	public Iterator<SceneNode> getChildrenIterator()
	{
		return children.iterator();
	}
}