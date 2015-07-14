package jrtr.gldeferredrenderer;

import java.util.ArrayList;
import java.util.Iterator;

import javax.media.opengl.GL3;
import javax.vecmath.Matrix4f;

import jrtr.Camera;
import jrtr.Frustum;
import jrtr.glrenderer.*;
import jrtr.Light;

/**
 * Sets up a shader and textures for deferred shading. A default implementation
 * that uses a default shader that simply passes through a texture from the 
 * framebuffer.
 * 
 * @author Heinrich Reich
 *
 */
public class DirectionalDiffuseSecondPassDrawer implements SecondPassDrawer{
	
	protected GLShader shader;
	protected GLDeferredRenderContext renderContext;
	ArrayList<Light> directionalLights;
	private Light defaultLight;
		
	public DirectionalDiffuseSecondPassDrawer(GLDeferredRenderContext context){
		this.renderContext = context;
		this.shader = GLUtils.loadShader("../jrtr/shaders/deferredShaders/default.vert", "../jrtr/shaders/deferredShaders/directionaldiffuse.frag");
		directionalLights = new ArrayList<Light>();
		defaultLight = new Light();
	}
	
	@Override
	public void manageShader(GLDeferredRenderContext context) {
		context.useShader(this.shader);
		context.bindTexture(3, context.getGBuffer().getDepthBufferTexture(), "depth", shader);
		context.bindTexture(2, context.getGBuffer().getUVBufferTexture(), "uvs", shader);
		context.bindTexture(1, context.getGBuffer().getNormalBufferTexture(), "normals", shader);
		context.bindTexture(0, context.getGBuffer().getColorBufferTexture(), "color", shader);
	}

	@Override
	public void manageLights(GL3 gl, Iterator<Light> iterator) {
		if(iterator != null) {
			while(iterator.hasNext()){
				Light l = iterator.next();
				switch(l.type){
				case DIRECTIONAL: this.directionalLights.add(l); break;
				}
			}
			if(this.directionalLights.size()==0)
				this.directionalLights.add(defaultLight);
			GLUtils.passDirectionalLightsToShader(gl, this.shader, this.directionalLights);
			this.directionalLights.clear();
		}
	}

	//private final float[] arrayMatrix = new float[16];
	private final Matrix4f temp = new Matrix4f();
	@Override
	public void managePerspective(GL3 gl, Camera cam, Frustum frust) {
		GLUtils.setCameraMatrix(cam.getCameraMatrix());
		try{
			temp.invert(frust.getProjectionMatrix());
			GLUtils.setUniformMatrix4f(this.renderContext, this.shader, "invProj", temp);
		} catch(Exception e){
			
		}
		GLUtils.setUniformMatrix4f(this.renderContext, this.shader, "proj", frust.getProjectionMatrix());
		try{
			temp.invert(cam.getCameraMatrix());
			GLUtils.setUniformMatrix4f(this.renderContext, this.shader, "cameraViewToWorldMatrix", temp);
		} catch(Exception e){}
		GLUtils.setUniformMatrix4f(this.renderContext, this.shader, "worldToCameraViewMatrix", cam.getCameraMatrix());
	}
}
