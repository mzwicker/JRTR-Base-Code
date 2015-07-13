package jrtr.gldeferredrenderer;

import javax.media.opengl.GL3;

/**
 * A GLBuffer which contains 5 textures:
 * 	- positions
 *  - normals
 *  - colors
 *  - texture coordinates
 *  - depth 
 * @author Heinrich Reich
 *
 */
public class GBuffer extends GLBuffer{
	
//	public static final int POSITION_BUFFER = 0, NORMAL_BUFFER = 1, COLOR_BUFFER = 2, TEXCOORDS_BUFFER = 3;

	public GBuffer(GL3 gl, int width, int height) throws RuntimeException{
		super(gl, width, height, 3, true);
	}
	
	public GBuffer(GL3 gl, int width, int height, int format) throws RuntimeException{
		super(gl, width, height, 3, true, format);
	}
	
	// The following methods do the same as super.getTexture(int) for each known texture.
	
	public int getNormalBufferTexture(){
		return this.textures.get(0);
	}
	
	public int getColorBufferTexture(){
		return this.textures.get(1);
	}
	
	public int getUVBufferTexture(){
		return this.textures.get(2);
	}
	
	public int getDepthBufferTexture(){
		return this.depthBuffer.get(0);
	}

	@Override
	protected void handleCreationError(boolean failed) throws RuntimeException {
		if(failed)
			throw new RuntimeException("Error occured while creating the geometry buffer!");
	}
	
}
