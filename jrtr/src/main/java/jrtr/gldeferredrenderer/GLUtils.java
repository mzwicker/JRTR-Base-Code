package jrtr.gldeferredrenderer;

import static javax.media.opengl.GL2ES2.*;
import static javax.media.opengl.GL2ES3.*;

import javax.media.opengl.GL3;
import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;
import javax.vecmath.Vector4f;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import jrtr.glrenderer.GLShader;
import jrtr.glrenderer.GLTexture;
import jrtr.glrenderer.GLVertexData;
import jrtr.Light;
import jrtr.Shader;
import jrtr.Texture;
import jrtr.VertexData;
import jrtr.gldeferredrenderer.GLDeferredRenderContext;
import static jrtr.gldeferredrenderer.GLDeferredRenderContext.gl;

/**
 * Simple helper class for setting uniform variables in a shader.
 * This class also provides methods for passing multiple lights to a shader.
 * The methods for setting have the following patterm: setUniform*f(GLDeferredRenderContext, GLShader, String, value_1, value_2, ...., value_n)
 * @author Heinrich Reich
 *
 */
public class GLUtils {
	
	private final static float[] lightColor = new float[3000], 
			lightPos = new float[3000],
			lightAtt = new float[3000],
			lightDirection = new float[3000],
			lightAngle = new float[1000];
	private final static Vector3f temp3 = new Vector3f();
	private final static Vector4f temp4 = new Vector4f();
	private final static Matrix4f cam = new Matrix4f();
	
	public static void setCameraMatrix(Matrix4f c){
		cam.set(c);
	}
	
	private static void transformVector(Vector3f v, float w){
		temp4.set(v); temp4.w =w;
		cam.transform(temp4);
		temp3.set(temp4.x, temp4.y, temp4.z);
	}
	
	public static void passPointLightToShader(GL3 gl, GLShader shader, Light l){
		transformVector(l.position, 1f);
		gl.glUniform3f(gl.glGetUniformLocation(shader.programId(), "lightPosition"), temp3.x, temp3.y, temp3.z);
		gl.glUniform3f(gl.glGetUniformLocation(shader.programId(), "lightAttenuation"), l.attenuation.x, l.attenuation.y, l.attenuation.z);
	}
	
	public static void passPointLightsToShader(GL3 gl, GLShader shader, ArrayList<Light> lights){
		if(lights.size() == 0) return;
		for(int i = 0; i < lights.size(); i++){
			Light l = lights.get(i);
			vector3ToFloatArray(l.diffuse, lightColor, i);
			transformVector(l.position, 1f);
			vector3ToFloatArray(temp3, lightPos, i);
			vector3ToFloatArray(l.attenuation, lightAtt, i);
		}
		gl.glUniform3fv(gl.glGetUniformLocation(shader.programId(), "positionLightColor"), lights.size(), lightColor, 0);
		gl.glUniform3fv(gl.glGetUniformLocation(shader.programId(), "positionLightPosition"), lights.size(), lightPos, 0);
		gl.glUniform3fv(gl.glGetUniformLocation(shader.programId(), "positionLightAttenuation"), lights.size(), lightAtt, 0);
	}
	
	public static void passDirectionalLightsToShader(GL3 gl, GLShader shader, ArrayList<Light> lights){
		if(lights.size() == 0) return;
		for(int i = 0; i < lights.size(); i++){
			Light l = lights.get(i);
			vector3ToFloatArray(l.diffuse, lightColor, i);
			transformVector(l.direction, 0f);
			vector3ToFloatArray(temp3, lightDirection, i);
		}
		gl.glUniform3fv(gl.glGetUniformLocation(shader.programId(), "dirLightColor"), lights.size(), lightColor, 0);
		gl.glUniform3fv(gl.glGetUniformLocation(shader.programId(), "dirLightDirection"), lights.size(), lightDirection, 0);
	}
	
	public static void passSpotLightsToShader(GL3 gl, GLShader shader, ArrayList<Light> lights){
		if(lights.size() == 0) return;
		for(int i = 0; i < lights.size(); i++){
			Light l = lights.get(i);
			transformVector(l.position, 1f);
			vector3ToFloatArray(temp3, lightPos, i);
			transformVector(l.direction, 0f);
			vector3ToFloatArray(temp3, lightDirection, i);
			vector3ToFloatArray(l.diffuse, lightColor, i);
			vector3ToFloatArray(l.attenuation, lightAtt, i);
			lightAngle[i] = l.spotCutoff/2f;
		}
		gl.glUniform3fv(gl.glGetUniformLocation(shader.programId(), "spotLightPosition"), lights.size(), lightPos, 0);
		gl.glUniform3fv(gl.glGetUniformLocation(shader.programId(), "spotLightDirection"), lights.size(), lightDirection, 0);
		gl.glUniform3fv(gl.glGetUniformLocation(shader.programId(), "spotLightColor"), lights.size(), lightColor, 0);
		gl.glUniform3fv(gl.glGetUniformLocation(shader.programId(), "spotLightAttenuation"), lights.size(), lightAtt, 0);
		gl.glUniform1fv(gl.glGetUniformLocation(shader.programId(), "spotLightAngle"), lights.size(), lightAngle, 0);
	}
	
	public static void setUniform1i(GLDeferredRenderContext context, GLShader shader, String name, int value){
		context.useShader(shader);
		gl.glUniform1i(gl.glGetUniformLocation(shader.programId(), name), value);
	}
	
	public static void setUniform1f(GLDeferredRenderContext context, GLShader shader, String name, float value){
		context.useShader(shader);
		gl.glUniform1f(gl.glGetUniformLocation(shader.programId(), name), value);
	}
	
	public static void setUniform2f(GLDeferredRenderContext context, GLShader shader, String name, float value1, float value2){
		context.useShader(shader);
		gl.glUniform2f(gl.glGetUniformLocation(shader.programId(), name), value1, value2);
	}
	
	public static void setUniform3f(GLDeferredRenderContext context, GLShader shader, String name, float value1, float value2, float value3){
		context.useShader(shader);
		gl.glUniform3f(gl.glGetUniformLocation(shader.programId(), name), value1, value2, value3);
	}
	
	public static void setUniform3f(GLDeferredRenderContext context, GLShader shader, String name, Vector3f vec){
		context.useShader(shader);
		gl.glUniform3f(gl.glGetUniformLocation(shader.programId(), name), vec.x, vec.y, vec.z);
	}
	
	public static void setUniform4f(GLDeferredRenderContext context, GLShader shader, String name, float value1, float value2, float value3, float value4){
		context.useShader(shader);
		gl.glUniform4f(gl.glGetUniformLocation(shader.programId(), name), value1, value2, value3, value4);
	}

	private static final float[] arrayMatrix = new float[16];
	public static void setUniformMatrix4f(GLDeferredRenderContext context, GLShader shader, String name, Matrix4f m){
		context.useShader(shader);
		Matrix4fUtils.transformationToFloat16(m, arrayMatrix);		
		gl.glUniformMatrix4fv(gl.glGetUniformLocation(shader.programId(), name), 1, false, arrayMatrix, 0);
	}
	
	private static void vector3ToFloatArray(Vector3f v, float[] target, int startingIndex){
		target[startingIndex*3] = v.x;
		target[startingIndex*3+1] = v.y;
		target[startingIndex*3+2] = v.z;
	}
	
	public static FloatBuffer getFloatBufferForType(int type){
		switch(type){
		case GL_FLOAT_VEC2: return newFloatBuffer(2);
		case GL_FLOAT_VEC3: return newFloatBuffer(3);
		case GL_FLOAT_VEC4: return newFloatBuffer(4);
		case GL_FLOAT_MAT2: return newFloatBuffer(4);
		case GL_FLOAT_MAT2x3: return newFloatBuffer(6);
		case GL_FLOAT_MAT2x4: return newFloatBuffer(8);
		case GL_FLOAT_MAT3: return newFloatBuffer(9);
		case GL_FLOAT_MAT3x2: return newFloatBuffer(6);
		case GL_FLOAT_MAT3x4: return newFloatBuffer(12); 
		case GL_FLOAT_MAT4: return newFloatBuffer(16);
		case GL_FLOAT_MAT4x2: return newFloatBuffer(8);
		case GL_FLOAT_MAT4x3: return newFloatBuffer(12);
		case GL_INT: return newFloatBuffer(1);
		case GL_INT_VEC2: return newFloatBuffer(2);
		case GL_INT_VEC3: return newFloatBuffer(3);
		case GL_INT_VEC4: return newFloatBuffer(4);
		case GL_BOOL: return newFloatBuffer(1);
		case GL_BOOL_VEC2: return newFloatBuffer(2);
		case GL_BOOL_VEC3: return newFloatBuffer(3);
		case GL_BOOL_VEC4: return newFloatBuffer(4);
		default: return newFloatBuffer(1);
		}
	}
	
	public static void setUniformf(GLShader shader, String name, int type, FloatBuffer buffer, int size){
		setUniformf(gl.glGetUniformLocation(shader.programId(), name), type, buffer, size);
	}
	
	public static void setUniformf(int location, int type, FloatBuffer buffer, int size){
		switch(type){
		case GL_FLOAT: gl.glUniform1fv(location, size, buffer); break;
		case GL_FLOAT_VEC2: gl.glUniform2fv(location, size, buffer); break;
		case GL_FLOAT_VEC3: gl.glUniform3fv(location, size, buffer); break;
		case GL_FLOAT_VEC4: gl.glUniform4fv(location, size, buffer); break;
		case GL_FLOAT_MAT2: gl.glUniformMatrix2fv(location, size, false, buffer); break;
		case GL_FLOAT_MAT2x3: gl.glUniformMatrix2x3fv(location, size, false, buffer); break;
		case GL_FLOAT_MAT2x4: gl.glUniformMatrix2x4fv(location, size, false, buffer); break;
		case GL_FLOAT_MAT3: gl.glUniformMatrix3fv(location, size, false, buffer); break;
		case GL_FLOAT_MAT3x2: gl.glUniformMatrix3x2fv(location, size, false, buffer); break;
		case GL_FLOAT_MAT3x4: gl.glUniformMatrix3x4fv(location, size, false, buffer); break;
		case GL_FLOAT_MAT4: gl.glUniformMatrix4fv(location, 1, false, buffer); break;
		case GL_FLOAT_MAT4x2: gl.glUniformMatrix4x2fv(location, size, false, buffer); break;
		case GL_FLOAT_MAT4x3: gl.glUniformMatrix4x3fv(location, size, false, buffer); break;
		case GL_BOOL: gl.glUniform1iv(location, size, toIntBuffer(buffer)); break;
		case GL_BOOL_VEC2: gl.glUniform2iv(location, size, toIntBuffer(buffer)); break;
		case GL_BOOL_VEC3: gl.glUniform3iv(location, size, toIntBuffer(buffer)); break;
		case GL_BOOL_VEC4: gl.glUniform4iv(location, size, toIntBuffer(buffer)); break;
		case GL_INT: gl.glUniform1iv(location, size, toIntBuffer(buffer)); break;
		case GL_INT_VEC2: gl.glUniform2iv(location, size, toIntBuffer(buffer)); break;
		case GL_INT_VEC3: gl.glUniform3iv(location, size, toIntBuffer(buffer)); break;
		case GL_INT_VEC4: gl.glUniform4iv(location, size, toIntBuffer(buffer)); break;
		default: System.out.println("nothing to set");break;
		}
	}
	
	private static IntBuffer toIntBuffer(FloatBuffer buffer){
		IntBuffer newBuf = newIntBuffer(buffer.capacity());
		for(int i = 0; i < buffer.capacity(); i++) newBuf.put((int) buffer.get(i));
		return newBuf;
	}
	
	public static FloatBuffer newFloatBuffer (int numFloats) {
		return FloatBuffer.allocate(numFloats);
	}
	
	public static ByteBuffer newByteBuffer (int numBytes) {
		return ByteBuffer.allocate(numBytes);
	}
	
	public static IntBuffer newIntBuffer (int numInts) {
		return IntBuffer.allocate(numInts);
	}
	
	/**
	 * Creates a new shader object.
	 */
	public static Shader makeShader() {
		return new GLShader(gl);
	}

	/**
	 * Creates a new texture object.
	 */
	public static Texture makeTexture() {
		GLTexture tex = new GLTexture(gl);
		return tex;
	}

	/**
	 * Creates a new vertex data object.
	 */
	public static VertexData makeVertexData(int n) {
		return new GLVertexData(n);
	}
	
	/**
	 * Loads vertex and fragment shader from the given path.
	 * Vertex shader has to own the ending *.vert, fragment shader *.frag.
	 * @param path
	 * @return
	 */
	public static GLShader loadShader(String path){
		return loadShader(path+".vert", path+".frag");
	}
	
	/**
	 * Loads vertex and fragment shader from the given paths.
	 * @return
	 */
	public static GLShader loadShader(String vertexPath, String fragmentPath){
		try {
			GLShader shader = (GLShader) makeShader();
			shader.load(vertexPath, fragmentPath);
			return shader;
		} catch (Exception e) {
			System.err.println("Problem with shader: "+vertexPath+", "+fragmentPath);
			System.err.println(e.getMessage());
			return null;
		}
	}
	
	/**
	 * Loads the texture from the given path
	 * @param path
	 * @return <code>null</code> if the texture couldn't be loaded or the GLTexture reference.
	 */
	public static GLTexture loadTexture(String path){
		GLTexture tex = null;
		try {
			tex = (GLTexture) makeTexture();
			tex.load(path);
		} catch (IOException e) {
			System.err.println("Problem with texture:");
			System.err.println(e.getMessage());
		}
		return tex;
	}

}
