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
	ArrayList<Light> pointLights, directionalLights, spotLights;
		
	public DirectionalDiffuseSecondPassDrawer(GLShader shader, GLDeferredRenderContext context){
		this.renderContext = context;
		this.setShader(shader);
		pointLights = new ArrayList<Light>();
		directionalLights = new ArrayList<Light>();
		spotLights = new ArrayList<Light>();
	}
	
	/**
	 * Sets the shader for this second pass drawer.
	 * @param shader
	 */
	public void setShader(GLShader shader) {
		this.shader = shader;
		ShaderUtils.setUniform3f(renderContext, shader, "ambient", .5f, .5f, .5f);
		ShaderUtils.setUniform1f(renderContext, shader, "specularCoefficent", 16);
		ShaderUtils.setUniform1i(renderContext, shader, "useColor", 1);
		ShaderUtils.setUniform1i(renderContext, shader, "drawPointLights", 1);
		ShaderUtils.setUniform1i(renderContext, shader, "drawSpotLights", 1);
	}

	@Override
	public void bindTextures(GLDeferredRenderContext context) {
		context.bindTexture(3, context.getGBuffer().getDepthBufferTexture(), "depth", shader);
		context.bindTexture(2, context.getGBuffer().getUVBufferTexture(), "uvs", shader);
		context.bindTexture(1, context.getGBuffer().getNormalBufferTexture(), "normals", shader);
	}

	@Override
	public void drawFinalTexture(GLDeferredRenderContext context) {
		context.drawTexture(1, context.getGBuffer().getUVBufferTexture(), "colors", shader, 0, 0, 1, 1);
	}

	@Override
	public void manageLights(GL3 gl, Iterator<Light> iterator) {
		if(iterator != null) {
			while(iterator.hasNext()){
				Light l = iterator.next();
				switch(l.type){
				case DIRECTIONAL: this.directionalLights.add(l); break;
				case SPOT: this.spotLights.add(l); break;
				case POINT: this.pointLights.add(l); break;
				}
			}
			ShaderUtils.setUniform3f(renderContext, shader, "lightsLength", this.pointLights.size(), this.directionalLights.size(), this.spotLights.size());
			ShaderUtils.passPointLightsToShader(gl, this.shader,this.pointLights);
			ShaderUtils.passDirectionalLightsToShader(gl, this.shader, this.directionalLights);
			ShaderUtils.passSpotLightsToShader(gl, this.shader, this.spotLights);
			this.spotLights.clear();
			this.directionalLights.clear();
			this.pointLights.clear();
		}
	}

	//private final float[] arrayMatrix = new float[16];
	private final Matrix4f temp = new Matrix4f();
	@Override
	public void managePerspective(GL3 gl, Camera cam, Frustum frust) {
		ShaderUtils.setCameraMatrix(cam.getCameraMatrix());
		try{
			temp.invert(frust.getProjectionMatrix());
			ShaderUtils.setUniformMatrix4f(this.renderContext, this.shader, "invProj", temp);
		} catch(Exception e){
			
		}
		ShaderUtils.setUniformMatrix4f(this.renderContext, this.shader, "proj", frust.getProjectionMatrix());
		try{
			temp.invert(cam.getCameraMatrix());
			ShaderUtils.setUniformMatrix4f(this.renderContext, this.shader, "cameraViewToWorldMatrix", temp);
		} catch(Exception e){}
		ShaderUtils.setUniformMatrix4f(this.renderContext, this.shader, "worldToCameraViewMatrix", cam.getCameraMatrix());
	}
}
