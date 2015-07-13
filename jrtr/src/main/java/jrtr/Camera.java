package jrtr;

import javax.vecmath.*;

/**
 * Stores the specification of a virtual camera. You will extend
 * this class to construct a 4x4 camera matrix, i.e., the world-to-
 * camera transform from intuitive parameters. 
 * 
 * A scene manager (see {@link SceneManagerInterface}, {@link SimpleSceneManager}) 
 * stores a camera.
 */
public class Camera {

	private Matrix4f cameraMatrix;
	private Vector3f centerOfProjection, lookAtPoint, upVector;
	
	
	/**
	 * Construct a camera with a default camera matrix. The camera
	 * matrix corresponds to the world-to-camera transform. This default
	 * matrix places the camera at (0,0,10) in world space, facing towards
	 * the origin (0,0,0) of world space, i.e., towards the negative z-axis.
	 */
	public Camera()
	{
		cameraMatrix = new Matrix4f();
		this.centerOfProjection = new Vector3f(0f,0f,10f);
		this.lookAtPoint = new Vector3f(0f,0f,0f);
		this.upVector = new Vector3f(0f,1f,0f);
		this.update();
		
/*		float f[] = {1.f, 0.f, 0.f, 0.f,
					 0.f, 1.f, 0.f, 0.f,
					 0.f, 0.f, 1.f, -10.f,
					 0.f, 0.f, 0.f, 1.f};
		cameraMatrix.set(f);*/
	}
	
	/**
	 * Return the camera matrix, i.e., the world-to-camera transform. For example, 
	 * this is used by the renderer.
	 * 
	 * @return the 4x4 world-to-camera transform matrix
	 */
	public Matrix4f getCameraMatrix()
	{
		return cameraMatrix;
	}
	
	/**
	 * Set the camera matrix, i.e., the world-to-camera transform.
	 */
	public void setCameraMatrix(Matrix4f m)
	{
		cameraMatrix.set(m);
	}
	
	/**
	 * @return the center of projection
	 */
	public Vector3f getCenterOfProjection() {
		return centerOfProjection;
	}

	/**
	 * @param centerOfProjection the center of projection to set
	 */
	public void setCenterOfProjection(Vector3f centerOfProjection) {
		this.centerOfProjection.set(centerOfProjection);
		this.update();
	}

	/**
	 * @return the look at point
	 */
	public Vector3f getLookAtPoint() {
		return lookAtPoint;
	}

	/**
	 * @param lookAtPoint the look at point to set
	 */
	public void setLookAtPoint(Vector3f lookAtPoint) {
		this.lookAtPoint.set(lookAtPoint);
		this.update();
	}

	/**
	 * @return the Up-Vector
	 */
	public Vector3f getUpVector() {
		return upVector;
	}

	/**
	 * @param upVector the Up-Vector to set
	 */
	public void setUpVector(Vector3f upVector) {
		this.upVector.set(upVector);
		this.update();
	}

	private Vector3f x = new Vector3f(), y = new Vector3f(), z = new Vector3f(), temp = new Vector3f();
	private void update() {
		temp.set(this.centerOfProjection);
		temp.sub(this.lookAtPoint);
		temp.normalize();
		z.set(temp);

		temp.set(this.upVector);
		temp.cross(temp, z);
		temp.normalize();
		x.set(temp);

		temp.cross(z, x);
		y.set(temp);

		this.cameraMatrix.setColumn(0, x.x, x.y, x.z, 0);
		this.cameraMatrix.setColumn(1, y.x, y.y, y.z, 0);
		this.cameraMatrix.setColumn(2, z.x, z.y, z.z, 0);
		this.cameraMatrix.setColumn(3, this.centerOfProjection.x,
				this.centerOfProjection.y,this.centerOfProjection.z, 1);
		try{
			this.cameraMatrix.invert();
		} catch(Exception e){
			System.err.println("Could not invert matrix!");
		}
	}
}
