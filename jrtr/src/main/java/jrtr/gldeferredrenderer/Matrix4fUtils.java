package jrtr.gldeferredrenderer;

import java.util.ArrayList;
import java.util.Arrays;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

/**
 * Utility class for doing matrix operations to prevent boiler plate code.
 * @author Heinrich Reich
 *
 */
public class Matrix4fUtils {
	
	public static final ArrayList<Matrix4f> xyRotation, xzRotation, xyzRotation, xzyRotation,
	yzRotation, yxRotation, yzxRotation, yxzRotation,
	zxRotation, zyRotation, zxyRotation, zyxRotation;
	public final static Matrix4f xRotation, yRotation, zRotation, identity;
	
	private static float roXAngle = .005f, rotYAngle = .01f, rotZAngle = .05f;
	
	static{
		xRotation = rotatedX((float) (Math.toDegrees(roXAngle)));
		yRotation = rotatedY((float) (Math.toDegrees(rotYAngle)));
		zRotation = rotatedZ((float) (Math.toDegrees(rotZAngle)));
		identity = new Matrix4f(); identity.setIdentity();
		
		
		xyRotation = new ArrayList<Matrix4f>(Arrays.asList(xRotation, yRotation));
		xzRotation = new ArrayList<Matrix4f>(Arrays.asList(xRotation, zRotation));
		xyzRotation = new ArrayList<Matrix4f>(Arrays.asList(xRotation, yRotation, zRotation));
		xzyRotation = new ArrayList<Matrix4f>(Arrays.asList(xRotation, zRotation, yRotation));
		
		yxRotation = new ArrayList<Matrix4f>(Arrays.asList(yRotation, xRotation));
		yzRotation = new ArrayList<Matrix4f>(Arrays.asList(yRotation, zRotation));
		yxzRotation = new ArrayList<Matrix4f>(Arrays.asList(yRotation, xRotation, zRotation));
		yzxRotation = new ArrayList<Matrix4f>(Arrays.asList(yRotation, zRotation, xRotation));
		
		zxRotation = new ArrayList<Matrix4f>(Arrays.asList(zRotation, xRotation));
		zyRotation = new ArrayList<Matrix4f>(Arrays.asList(zRotation, yRotation));
		zxyRotation = new ArrayList<Matrix4f>(Arrays.asList(zRotation, xRotation, yRotation));
		zyxRotation = new ArrayList<Matrix4f>(Arrays.asList(zRotation, yRotation, xRotation));
	}
	
	/**
	 * @param scale
	 * @return scaled identity matrix
	 */
	public static Matrix4f scaled(float scale){
		Matrix4f m = new Matrix4f();
		m.setIdentity();
		m.setScale(scale);
		return m;
	}
	
	/**
	 * @param x
	 * @param y
	 * @param z
	 * @return translation matrix
	 */
	public static Matrix4f translated(float x, float y, float z){
		return translated(new Vector3f(x,y,z));
	}
	
	/**
	 * @param t
	 * @return translated matrix
	 */
	public static Matrix4f translated(Vector3f t){
		Matrix4f m = new Matrix4f();
		m.setIdentity();
		m.setTranslation(new Vector3f(t));
		return m;
	}
	
	/**
	 * @param angle in degrees
	 * @return rotated matrix around the x-axis
	 */
	public static Matrix4f rotatedX(float angle){
		Matrix4f m = new Matrix4f();
		m.setIdentity();
		m.rotX((float) Math.toRadians(angle));
		return m;
	}
	
	/**
	 * @param angle in degrees
	 * @return rotated matrix around the y-axis
	 */
	public static Matrix4f rotatedY(float angle){
		Matrix4f m = new Matrix4f();
		m.setIdentity();
		m.rotY((float) Math.toRadians(angle));
		return m;
	}

	/**
	 * @param angle in degrees
	 * @return rotated matrix around the z-axis
	 */
	public static Matrix4f rotatedZ(float angle){
		Matrix4f m = new Matrix4f();
		m.setIdentity();
		m.rotZ((float) Math.toRadians(angle));
		return m;
	}
	
	/**
	 * @param m1
	 * @param m2
	 * @return multiplied matrix m1*m2
	 */
	public static Matrix4f mul(Matrix4f m1, Matrix4f m2){
		m1.mul(m2);
		return m1;
	}
	
	/**
	 * @param ms
	 * @return multiplied matrix ms[0]*ms[1]*...*ms[n-1]
	 */
	public static Matrix4f mul(Matrix4f... ms){
		Matrix4f m1 = ms[0];
		for(int i = 1; i < ms.length; i++)
			m1.mul(ms[i]);
		return m1;
	}
	
	/**
	 * Saves an orthogonal projection in the given matrix.
	 * @param m
	 * @param left
	 * @param right
	 * @param bottom
	 * @param top
	 * @param near
	 * @param far
	 */
	public static void setOrtho(Matrix4f m, float left, float right, float bottom, float top, float near, float far){
		m.setIdentity();
		float x_orth = 2 / (right - left);
		float y_orth = 2 / (top - bottom);
		float z_orth = -2 / (far - near);

		float tx = -(right + left) / (right - left);
		float ty = -(top + bottom) / (top - bottom);
		float tz = -(far + near) / (far - near);
		m.m00 = x_orth;	m.m10 = 0f; m.m20 = 0f; m.m30 = 0f; m.m01 = 0f; 
		m.m11 = y_orth;	m.m21 = 0f; m.m31 = 0f; m.m02 = 0f; m.m12 = 0f; 
		m.m22 = z_orth;	m.m03 = tx; m.m13 = ty; m.m23 = tz; m.m33 = 1f;
	}
	
	/**
	 * Convert a Transformation to a float array in column major ordering, as
	 * used by OpenGL.
	 */
	public static float[] transformationToFloat16(Matrix4f m) {
		float[] f = new float[16];
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
				f[j * 4 + i] = m.getElement(i, j);
		return f;
	}
	
	/**
	 * Convert a Transformation to a float array in column major ordering, as
	 * used by OpenGL.
	 */
	public static void transformationToFloat16(Matrix4f m, float[] target) {
		for (int i = 0; i < 4; i++)
			for (int j = 0; j < 4; j++)
				target[j * 4 + i] = m.getElement(i, j);
	}

}
