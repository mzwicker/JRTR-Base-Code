package jrtr.swrenderer;

import jrtr.RenderContext;
import jrtr.RenderItem;
import jrtr.SceneManagerInterface;
import jrtr.SceneManagerIterator;
import jrtr.Shader;
import jrtr.Texture;
import jrtr.VertexData;
import jrtr.Material;

import java.awt.image.*;
import javax.vecmath.*;
import java.util.LinkedList;
import java.util.ListIterator;


/**
 * A skeleton for a software renderer. It works in combination with
 * {@link SWRenderPanel}, which displays the output image. In project 2 
 * you will implement your own rasterizer in this class.
 * <p>
 * To use the software renderer, you will simply replace {@link GLRenderPanel} 
 * with {@link SWRenderPanel} in the user application.
 */
public class SWRenderContext implements RenderContext {

	private SceneManagerInterface sceneManager;
	private BufferedImage colorBuffer;
	private int[] zeroColorBuffer;
	private int width, height;
	
	// Rendering pipeline state variables
	private Matrix4f viewportMatrix;
	private Matrix4f projectionMatrix;
	private float[] vertexColor;
	private float[] vertexNormal;
	private float[] vertexTexCoords;
	
	public SWRenderContext()
	{
		// Initialize rendering pipeline state variables to default values
		projectionMatrix = new Matrix4f();
		viewportMatrix = new Matrix4f();
		vertexColor = new float[3];
		vertexColor[0] = 1.f;
		vertexColor[1] = 1.f;
		vertexColor[2] = 1.f;
		vertexNormal = new float[3];
		vertexTexCoords = new float[2];
	}
	
	public void setSceneManager(SceneManagerInterface sceneManager)
	{
		this.sceneManager = sceneManager;
	}
	
	/**
	 * This is called by the SWRenderPanel to render the scene to the 
	 * software frame buffer.
	 */
	public void display()
	{
		if(sceneManager == null) return;
		
		beginFrame();
	
		SceneManagerIterator iterator = sceneManager.iterator();	
		while(iterator.hasNext())
		{
			draw(iterator.next());
		}		
		
		endFrame();
	}

	/**
	 * This is called by the {@link SWJPanel} to obtain the color buffer that
	 * will be displayed.
	 */
	public BufferedImage getColorBuffer()
	{
		return colorBuffer;
	}
	
	/**
	 * Set a new viewport size. The render context will also need to store
	 * a viewport matrix, which you need to reset here. 
	 */
	public void setViewportSize(int width, int height)
	{
		this.width = width;
		this.height = height;
		
		// Set viewport matrix, note that the y coordinate 
		// is multiplied by -1, because the java BufferedImage
		// has its origin at the top right
		viewportMatrix = new Matrix4f();
		viewportMatrix.setIdentity();
		viewportMatrix.setElement(0,0,(float)width/2.f);
		viewportMatrix.setElement(0,3,(float)width/2.f);
		viewportMatrix.setElement(1,1,-(float)height/2.f);
		viewportMatrix.setElement(1,3,(float)height/2.f);
		viewportMatrix.setElement(2,2,.5f);
		viewportMatrix.setElement(2,3,.5f);
		
		// Allocate framebuffer
		colorBuffer = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);
		zeroColorBuffer = new int[width*height];
	}
		
	/**
	 * Clear the framebuffer here.
	 */
	private void beginFrame()
	{
		projectionMatrix = sceneManager.getFrustum().getProjectionMatrix();
		
		// Clear framebuffer
		colorBuffer.setRGB(0, 0, width, height, zeroColorBuffer, 0, width);
	}
	
	private void endFrame()
	{		
	}
	
	/**
	 * The main rendering method. This collects all information necessary to render each triangle
	 * and calls @drawTriangle to perform the rasterization.
	 */
	private void draw(RenderItem renderItem)
	{
		VertexData vertexData = renderItem.getShape().getVertexData();
		LinkedList<VertexData.VertexElement> vertexElements = vertexData.getElements();
		int indices[] = vertexData.getIndices();

		// Don't draw if there are no indices
		if(indices == null) return;
		
		// Vertex attributes for a triangle
		float[][] colors = new float[3][3];
		float[][] positions = new float[3][4];
		float[][] normals = new float[3][3];
		float[][] texCoords = new float[3][2];

		// Construct full transformation matrix
		Matrix4f t= new Matrix4f(viewportMatrix);
		t.mul(projectionMatrix);
		t.mul(sceneManager.getCamera().getCameraMatrix());
		t.mul(renderItem.getT());
	     
        // Draw geometry
		int k = 0;	// index of triangle vertex
		for(int j=0; j<indices.length; j++)
		{
			int i = indices[j];
			
			// Iterate over vertex elements, i.e., position, color, normal, texture, etc.
			ListIterator<VertexData.VertexElement> itr = vertexElements.listIterator(0);
			while(itr.hasNext())
			{
				VertexData.VertexElement e = itr.next();
				if(e.getSemantic() == VertexData.Semantic.POSITION)
				{
					Vector4f p = new Vector4f(e.getData()[i*3],e.getData()[i*3+1],e.getData()[i*3+2],1);
					t.transform(p);
					positions[k][0] = p.x;
					positions[k][1] = p.y;
					positions[k][2] = p.z;
					positions[k][3] = p.w;
	
					// Assign the other "state variables" to the current vertex
					colors[k][0] = vertexColor[0];
					colors[k][1] = vertexColor[1];
					colors[k][2] = vertexColor[2];
					
					normals[k][0] = vertexNormal[0];
					normals[k][1] = vertexNormal[1];
					normals[k][2] = vertexNormal[2];
			
					texCoords[k][0] = vertexTexCoords[0];
					texCoords[k][1] = vertexTexCoords[1];
					
					k++;
				}
				// Read the "state variables" for color, normals, textures, if they are available
				if(e.getSemantic() == VertexData.Semantic.COLOR)
				{
					vertexColor[0] = e.getData()[i*3];
					vertexColor[1] = e.getData()[i*3+1];
					vertexColor[2] = e.getData()[i*3+2];
				}
				if(e.getSemantic() == VertexData.Semantic.NORMAL)
				{
					vertexNormal[0] = e.getData()[i*3];
					vertexNormal[1] = e.getData()[i*3+1];
					vertexNormal[2] = e.getData()[i*3+2];
				}
				if(e.getSemantic() == VertexData.Semantic.TEXCOORD)
				{
					vertexTexCoords[0] = e.getData()[i*2];
					vertexTexCoords[1] = e.getData()[i*2+1];
				}
			}
			
			if(k == 3)
			{
				drawTriangle(positions, colors, normals, texCoords, renderItem.getShape().getMaterial());
				k = 0;
			}
		}
	}
	
	/**
	 * Draw a triangle. Implement triangle rasterization here. You will need to include a z-buffer to 
	 * resolve visibility.  
	 */
	void drawTriangle(float positions[][], float colors[][], float normals[][], float texCoords[][], Material mat)
	{							
		// Project vertices and draw. This is only for demonstration purposes and needs to be replace
		// by your triangle rasterization code.
		for(int i=0; i<3; i++)	
		{
			if(positions[i][3]!=0)
			{
				// Project vertex to pixel coordinates by dividing by the w coordinate.
				int vx = (int)(positions[i][0]/positions[i][3]);
				int vy = (int)(positions[i][1]/positions[i][3]);
							
				if(vx>=0 && vx<width && vy>=0 && vy<height)
				{
					// Draw the pixel using the vertex color.
					// colorBuffer.setRGB(vx, vy, ((int)(255.f*colors[i][0]) << 16) | ((int)(255.f*colors[i][1]) << 8) | ((int)(255.f*colors[i][2])));
					
					// Draw vertices in white for better visibility
					colorBuffer.setRGB(vx, vy, ((int)(255) << 16) | ((int)(255) << 8) | ((int)(255)));
				}
			}
		}
	}
	
	/**
	 * Does nothing. We will not implement shaders for the software renderer.
	 */
	public Shader makeShader()	
	{
		return new SWShader();
	}
	
	/**
	 * Does nothing. We will not implement shaders for the software renderer.
	 */
	public void useShader(Shader s)
	{
	}
	
	/**
	 * Does nothing. We will not implement shaders for the software renderer.
	 */
	public void useDefaultShader()
	{
	}

	/**
	 * Does nothing. We will not implement textures for the software renderer.
	 */
	public Texture makeTexture()
	{
		return new SWTexture();
	}
	
	public VertexData makeVertexData(int n)
	{
		return new SWVertexData(n);		
	}
}
