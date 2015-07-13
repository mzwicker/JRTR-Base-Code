package jrtr;

import jrtr.glrenderer.GLRenderContext;
import jrtr.swrenderer.SWRenderContext;

/**
 * Declares the functionality of a render context, or a "renderer". It is
 * currently implemented by {@link GLRenderContext} and {@link SWRenderContext}. 
 */
public interface RenderContext {

	/**
	 * Set a scene manager that will be rendered.
	 */
	void setSceneManager(SceneManagerInterface sceneManager);
	
	/**
	 * Make a shader.
	 * 
	 * @return the shader
	 */
	Shader makeShader();
	
	/**
	 * Use the default shader.
	 */
	void useDefaultShader();
	
	/**
	 * Use a shader.
	 */
	void useShader(Shader s);
	
	/**
	 * Make a texture.
	 * 
	 * @return the texture
	 */
	Texture makeTexture();

	/**
	 * Make a vertex data sturcture.
	 * 
	 * @return the vertex data
	 */
	VertexData makeVertexData(int n);
}
