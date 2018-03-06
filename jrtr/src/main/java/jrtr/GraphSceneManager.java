package jrtr;

import java.util.Iterator;
import java.util.LinkedList;
import javax.vecmath.*;

public class GraphSceneManager implements SceneManagerInterface {

	private SceneNode root;
	private LinkedList<Light> lights;
	private Camera camera;
	private Frustum frustum;
	
	/**
	 * Implement the iterative graph traversal here. 
	 */
	private class GraphSceneManagerItr implements SceneManagerIterator {

		// Dummy implementation.
		public GraphSceneManagerItr(GraphSceneManager sceneManager)
		{
			
		}
		
		// Dummy implementation.		
		public boolean hasNext()
		{
			return false;
		}

		// Dummy implementation.
		public RenderItem next()
		{
			return null;
		}
	}
	
	public GraphSceneManager(SceneNode root)
	{
		this.root = root;
		camera = new Camera();
		frustum = new Frustum();
		lights = new LinkedList<Light>();
	}
	
	public Camera getCamera()
	{
		return camera;
	}
	
	public Frustum getFrustum()
	{
		return frustum;
	}

	public SceneManagerIterator iterator() {
		return new GraphSceneManagerItr(this);
	}
	
	public void addLight(Light light)
	{
		lights.add(light);
	}
	
	public Iterator<Light> lightIterator()
	{
		return lights.iterator();
	}
}