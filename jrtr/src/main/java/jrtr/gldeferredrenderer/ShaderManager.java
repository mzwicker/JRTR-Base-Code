package jrtr.gldeferredrenderer;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.media.opengl.GL3;

import jrtr.glrenderer.GLShader;
import jrtr.gldeferredrenderer.GLDeferredRenderContext;


import static javax.media.opengl.GL2ES2.*;

/**
 * A helper class for managing shaders.
 * This class is able to re-compile shaders on the fly while the application is running.
 * @author Heinrich Reich
 *
 */
public class ShaderManager{
	
	private HashMap<SimpleEntry<String, String>, GLShader> shaders;
	private HashMap<SimpleEntry<String, String>, GLShader> toReload;
	
	public ShaderManager(){
		this.shaders = new HashMap<SimpleEntry<String, String>, GLShader>();
		this.toReload = new HashMap<SimpleEntry<String, String>, GLShader>();
	}
	
	public void dispose(GL3 gl){
		gl.glUseProgram(0);
		@SuppressWarnings("unchecked")
		SimpleEntry<String, String>[] s = new SimpleEntry[this.shaders.size()];
		this.shaders.keySet().toArray(s);
		for(SimpleEntry<String, String> e: s)
			this.shaders.get(e).dispose();;
	}
	
	public void reloadShaders(final GLDeferredRenderContext context){
		@SuppressWarnings("unchecked")
		final
		SimpleEntry<String, String>[] s = new SimpleEntry[this.toReload.size()];
		this.toReload.keySet().toArray(s);
		for(SimpleEntry<String, String> e: s)
			reloadShader(context, e);
		toReload.clear();
	}

	private void reloadShader(GLDeferredRenderContext context, SimpleEntry<String, String> e){
		GL3 gl = GLDeferredRenderContext.gl;
		GLShader originalShader = this.shaders.get(e);
		
		context.useShader(originalShader);
		IntBuffer params = IntBuffer.allocate(1);
		//Get the number of uniforms used in the shader program
		gl.glGetProgramiv(originalShader.programId(), GL3.GL_ACTIVE_UNIFORMS, params);
		ArrayList<ShaderInfo> infos = new ArrayList<ShaderInfo>();
		IntBuffer length = IntBuffer.allocate(1);
		IntBuffer size = IntBuffer.allocate(1);
		IntBuffer type = IntBuffer.allocate(1);
		ByteBuffer name = ByteBuffer.allocate(100);
		for(int i = 0; i< params.get(0); i++){
			ShaderInfo info = new ShaderInfo();
			//Load uniform information into the buffers
			gl.glGetActiveUniform(originalShader.programId(), i, name.capacity()-1, length, size, type, name);
			info.type = type.get(0);
			FloatBuffer b = ShaderUtils.getFloatBufferForType(info.type);
			gl.glGetUniformfv(originalShader.programId(), i, b); //Load floating point values in current shader
			info.floats = b;
			info.size = size.get(0);
			infos.add(info);
		}
		params = null;
		int[] texIds = new int[32];
		IntBuffer p = IntBuffer.allocate(32);
		for(int i = 0; i < texIds.length; i++){
			gl.glActiveTexture(GL_TEXTURE0 + i);
			gl.glGetIntegerv(GL3.GL_TEXTURE_BINDING_2D, p);
			texIds[i] = p.get(0);
		}
		gl.glUseProgram(0);
		Field vertex = null, fragment = null, program = null;
		int progId = originalShader.programId(), vertexId = 0, fragmentId = 0;
		try {
			vertex = originalShader.getClass().getDeclaredField("vertexHandle");
			vertex.setAccessible(true);
			fragment = originalShader.getClass().getDeclaredField("fragmentHandle");
			fragment.setAccessible(true);
			program = originalShader.getClass().getDeclaredField("p");
			program.setAccessible(true);
			vertexId = vertex.getInt(originalShader);
			fragmentId = fragment.getInt(originalShader);

			try {
				originalShader.load(e.getKey(), e.getValue());
				context.useShader(originalShader);
				int texIndex = 0;
				for(int i = 0; i < infos.size(); i++){
					if(infos.get(i).type == GL_SAMPLER_2D){
						gl.glActiveTexture(GL_TEXTURE0+texIndex);
						gl.glEnable(GL_TEXTURE_2D);
						gl.glBindTexture(GL_TEXTURE_2D, texIds[texIndex]);
						gl.glUniform1i(i, texIndex++);
					}
					else ShaderUtils.setUniformf(i, infos.get(i).type, infos.get(i).floats, infos.get(i).size); //Pass old values to shader
				}
				gl.glUseProgram(0);
				gl.glDeleteShader(vertexId);
				gl.glDeleteShader(fragmentId);
				gl.glDeleteProgram(progId);
			} catch (Exception e1) {
				try {
					vertex.set(originalShader, new Integer(vertexId));
					fragment.set(originalShader, new Integer(fragmentId));
					program.set(originalShader, new Integer(progId));
					vertex.setAccessible(false);
					fragment.setAccessible(false);
					program.setAccessible(false);
				} catch (IllegalArgumentException | IllegalAccessException e2) {
					e2.printStackTrace();
				}
				e1.printStackTrace();
			}
		} catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e2) {
			e2.printStackTrace();
		}
	}
	
	public void pushShaderToReload(String vertex, String fragment){
		SimpleEntry<String, String> e = this.getEntryFor(vertex, fragment);
		this.toReload.put(e, this.shaders.get(e));
	}
	
	public void addShader(String vertex, String fragment, GLShader shader){
		SimpleEntry<String, String> e = this.getEntryFor(vertex, fragment);
		this.shaders.put((e == null) ? new SimpleEntry<String, String>(vertex, fragment) :  e, shader);
	}
	
	private SimpleEntry<String, String> getEntryFor(String vertex, String fragment){
		@SuppressWarnings("unchecked")
		SimpleEntry<String, String>[] s = new SimpleEntry[this.shaders.size()];
		this.shaders.keySet().toArray(s);
		for(SimpleEntry<String, String> e: s)
			if(e.getKey().equals(vertex) && e.getValue().equals(fragment)) return e;
		return null;
	}
	
	public String getVertexShaderFor(String fragment){
		List<String> vertexes = this.getVertexShadersFor(fragment);
		return (vertexes.size() > 0) ?  vertexes.get(0): null;
	}
	
	public List<String> getVertexShadersFor(String fragment){
		@SuppressWarnings("unchecked")
		SimpleEntry<String, String>[] s = new SimpleEntry[this.shaders.size()];
		this.shaders.keySet().toArray(s);
		List<String> vertexes = new ArrayList<String>();
		for(SimpleEntry<String, String> e: s)
			if(e.getValue().equals(fragment)) 
				vertexes.add(e.getKey());
		return vertexes;
	}
	
	public String getFragmentShaderFor(String vertex){
		List<String> fragments = this.getFragmentShadersFor(vertex);
		return (fragments.size() > 0) ?  fragments.get(0): null;
	}
	
	public List<String> getFragmentShadersFor(String vertex){
		@SuppressWarnings("unchecked")
		SimpleEntry<String, String>[] s = new SimpleEntry[this.shaders.size()];
		this.shaders.keySet().toArray(s);
		List<String> fragments = new ArrayList<String>();
		for(SimpleEntry<String, String> e: s)
			if(e.getKey().equals(vertex))
				fragments.add(e.getValue());
		return fragments;
	}
	
	private static class ShaderInfo{
		FloatBuffer floats;
		int type, size;
	}

}
