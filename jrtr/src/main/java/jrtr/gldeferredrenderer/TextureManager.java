package jrtr.gldeferredrenderer;

import java.util.ArrayList;

import javax.media.opengl.GL3;

import jrtr.glrenderer.GLTexture;

/**
 * Simple helper class which is able to hold and dispose textures.
 * @author Heinrich Reich
 */
public class TextureManager {
	
	private ArrayList<GLTexture> textures;
	
	public TextureManager(){
		this.textures = new ArrayList<GLTexture>();
	}
	
	/**
	 * Disposes all textures.
	 * @param gl
	 */
	public void dispose(GL3 gl){
		for(GLTexture texture: this.textures)
			gl.glDeleteTextures(1, new int[]{texture.getId()}, 0);
	}
	
	/**
	 * Adds the given texture to this manager.
	 * @param tex
	 */
	public void addTexture(GLTexture tex){
		this.textures.add(tex);
	}

}
