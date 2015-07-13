package jrtr.gldeferredrenderer;

import javax.vecmath.Matrix4f;
import javax.vecmath.Vector3f;

import jrtr.glrenderer.GLVertexData;
import jrtr.RenderItem;
import jrtr.Shape;
import jrtr.VertexData;
import jrtr.VertexData.Semantic;

/**
 * Simple helper class for storing and rendering a quad on the screen.
 * @author Heinrich Reich
 *
 */
public class Quad {
	
	private float v[] = new float[12];
	private float c[] = new float[12];
	private float uv[] = {0,0, 1,0, 1,1, 0,1};
	private int indices[] = {0,1,2, 0,3,2};
	
	private float x, y, scaleX = 1f, scaleY = 1f;
	
	private VertexData quadVertices;
	private Shape quadShape;
	private RenderItem renderItem;
	private Matrix4f transformation;
	private final Vector3f translation = new Vector3f();
	
	public Quad(){
		this.transformation = new Matrix4f();
		this.transformation.setIdentity();
		this.setColor(1f, 1f, 1f);
		this.setPosition(0, 0);
	}
	
	private void initShape(){
		v[0] = 0; v[1] = 0; v[2] = 0f;
		v[3] = 1; v[4] = 0; v[5] = 0f;
		v[6] = 1; v[7] = 1; v[8] = 0f;
		v[9] = 0; v[10] = 1; v[11] = 0f;
		this.quadVertices = new GLVertexData(4);
		this.quadVertices.addElement(v, Semantic.POSITION, 3);
		this.quadVertices.addElement(c, Semantic.COLOR, 3);
		this.quadVertices.addElement(uv, Semantic.TEXCOORD, 2);
		this.quadVertices.addIndices(indices);
		this.quadShape = new Shape(this.quadVertices);
		this.renderItem   = new RenderItem(this.quadShape, this.transformation);
	}
	
	public void setColor(float r, float g, float b){
		c[0] = r; c[1] = g; c[2] = b;
		c[3] = r; c[4] = g; c[5] = b;
		c[6] = r; c[7] = g; c[8] = b;
		c[9] = r; c[10] = g; c[11] = b;
		this.initShape();
	}
	
	public void setTransformation(float x, float y, float scaleX, float scaleY){
		this.x = x; this.y = y;
		this.scaleX = scaleX; this.scaleY = scaleY;
		this.translation.set(x,y,0f);
		this.transformation.setTranslation(this.translation);
		this.transformation.m00 = scaleX;
		this.transformation.m11 = scaleY;
	}
	
	public void setPosition(float x, float y){
		this.setTransformation(x, y, this.scaleX, this.scaleY);
	}
	
	public void setScale(float scaleX, float scaleY){
		this.setTransformation(this.x, this.y, scaleX, scaleY);
	}
	
	public RenderItem getRenderItem(){
		return renderItem;
	}

}
