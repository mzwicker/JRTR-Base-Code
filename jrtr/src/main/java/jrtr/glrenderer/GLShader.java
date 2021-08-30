package jrtr.glrenderer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL45.*;
//import com.jogamp.opengl.GL3;

import jrtr.Shader;

/**
 * Manages OpenGL shaders.
 */
public class GLShader implements Shader {
	
	private int p, vertexHandle, fragmentHandle;	// The shader identifier
	
	public GLShader()
	{
	}
	
	/**
	 * Utility method. Returns the vertex/fragment shader info log as a string. 
	 */
	private String getCompilerOutputShader(int shaderObject) {
		IntBuffer ib = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
		ib.rewind();
		
		glGetShaderiv(shaderObject, GL_INFO_LOG_LENGTH, ib);
		int logLength = ib.get(0);
		if(logLength <= 2)
			return "No compiler output.";
		
		ib.rewind();
		ByteBuffer log = ByteBuffer.allocateDirect(logLength);
		glGetShaderInfoLog(shaderObject, ib, log);
		
		byte[] infoBytes = new byte[logLength-1]; //ignore \0-character of c-string
		log.get(infoBytes);
		return new String(infoBytes);
	}
	
	/**
	 * Utility method. Returns the shader program compile info log as a string. 
	 */
	private String getLinkerOutput(int shaderObject) {
		IntBuffer ib = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
		ib.rewind();
		
		glGetProgramiv(shaderObject, GL_INFO_LOG_LENGTH, ib);
		int logLenght = ib.get(0);
		if(logLenght <= 2)
			return "No linker output.";
		
		ib.rewind();
		ByteBuffer log = ByteBuffer.allocateDirect(logLenght);
		glGetProgramInfoLog(shaderObject, ib, log);
		
		byte[] infoBytes = new byte[logLenght-1]; //ignore \0-character of c-string
		log.get(infoBytes);
		return new String(infoBytes);
	}
	
	/**
	 * Load the vertex and fragment shader programs from a file.
	 */
	public void load(String vertexFileName, String fragmentFileName) throws Exception	
	{			
		String vsrc[] = new String[1];
		String fsrc[] = new String[1];
		
		// Read shader programs from file
		BufferedReader brv = new BufferedReader(new FileReader(vertexFileName));
		vsrc[0]= "";
		String line;
		while ((line=brv.readLine()) != null) {
		  vsrc[0] += line + "\n";
		}

		BufferedReader brf = new BufferedReader(new FileReader(fragmentFileName));
		fsrc[0] = "";
		while ((line=brf.readLine()) != null) {
		  fsrc[0] += line + "\n";
		}
		
		// Close file readers
		brv.close();
		brf.close();

		// Make (compile and link) OpenGL shaders
		vertexHandle = glCreateShader(GL_VERTEX_SHADER);
		fragmentHandle = glCreateShader(GL_FRAGMENT_SHADER);
		
		glShaderSource(vertexHandle, vsrc);
		glCompileShader(vertexHandle);

		System.out.println("Vertex shader output for " + vertexFileName + ":\n" + this.getCompilerOutputShader(vertexHandle));
		
		glShaderSource(fragmentHandle, fsrc);
		glCompileShader(fragmentHandle);

		System.out.println("Fragment shader output for " + fragmentFileName + ":\n" + this.getCompilerOutputShader(fragmentHandle));
		
		p = glCreateProgram();
		glAttachShader(p, vertexHandle);
		glAttachShader(p, fragmentHandle);
		glLinkProgram(p);
		
		System.out.println("Linker output:\n" + this.getLinkerOutput(p));
		
		// Report errors
		int[] status = new int[1];
		glGetShaderiv(vertexHandle, GL_COMPILE_STATUS, status);
		if(status[0] == GL_FALSE) {
			throw new Exception("Could not compile vertex shader " + vertexFileName + ".");
		}
		glGetShaderiv(fragmentHandle, GL_COMPILE_STATUS, status);
		if(status[0] == GL_FALSE) {
			throw new Exception("Could not compile fragment shader " + fragmentFileName + ".");
		}
		
		IntBuffer ib = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder()).asIntBuffer();
		ib.rewind();
		glGetProgramiv(p, GL_LINK_STATUS, ib);
		if(ib.get(0) == GL_FALSE) {
			throw new Exception("Could not link vertex and fragment shader.");
		}
	}
		
	public int programId()
	{
		return p;		
	}
	
	public void dispose(){
		glDeleteShader(this.vertexHandle);
		glDeleteShader(this.fragmentHandle);
		glDeleteProgram(this.p);
	}
}
