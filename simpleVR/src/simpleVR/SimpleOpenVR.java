package simpleVR;

import jrtr.*;
import jrtr.glrenderer.*;

import javax.swing.*;

import java.lang.reflect.Array;
import javax.vecmath.*;
import java.util.Timer;

/**
 * Implements a simple VR application that renders to an HMD via
 * OpenVR. OpenVR functionality is provided by the {@link OpenVRRenderPanel}.
 * Also demonstrates tracking of a VR hand controller.
 */
public class SimpleOpenVR
{	
	static VRRenderPanel renderPanel;
	static RenderContext renderContext;
	static SimpleSceneManager sceneManager;
	
	//shapes
	static Shape ball;
	static Shape controllerCube;
	static Shape controllerCubeTriggered;
	static Shape surroundingCube;
	static Shape controllerRacket;
	
	//stores bounding box for racket. Useful for collision detection with ball.
	static Vector3f racketBoundsMax = new Vector3f(Float.MIN_VALUE, Float.MIN_VALUE, Float.MIN_VALUE);
	static Vector3f racketBoundsMin = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
	
	//scene-geometry parameters
	static float ballRadius = 0.15f;
	static float roomSize = 2.f;
	static float controllerSize = 0.015f;

	//additional parameters
	static Vector3f throwingTranslationAccum;


	/**
	 * An extension of {@link OpenVRRenderPanel} to 
	 * provide a call-back function for initialization. 
	 */ 
	public final static class SimpleVRRenderPanel extends VRRenderPanel
	{
		private Timer timer;	// Timer to trigger animation rendering
		
		/**
		 * Initialization call-back. We initialize our renderer here.
		 * 
		 * @param r	the render context that is associated with this render panel
		 */
		public void init(RenderContext r)
		{
			renderContext = r;
			
			// Make a simple geometric object: a cube
			
			// The vertex positions of the cube
			float v[] = {-1,-1,1, 1,-1,1, 1,1,1, -1,1,1,		// front face
				         -1,-1,-1, -1,-1,1, -1,1,1, -1,1,-1,	// left face
					  	 1,-1,-1,-1,-1,-1, -1,1,-1, 1,1,-1,		// back face
						 1,-1,1, 1,-1,-1, 1,1,-1, 1,1,1,		// right face
						 1,1,1, 1,1,-1, -1,1,-1, -1,1,1,		// top face
						-1,-1,1, -1,-1,-1, 1,-1,-1, 1,-1,1};	// bottom face
			//for(int i=0; i<Array.getLength(v); i++) v[i] = v[i] * 0.1f;	// make it smaller
			
			// The vertex colors
			float c[] = {0.5f,0,0, 0.5f,0,0, 0.5f,0,0, 0.5f,0,0,
					     0,0.3f,0, 0,0.3f,0, 0,0.3f,0, 0,0.3f,0,
					     0.5f,0,0, 0.5f,0,0, 0.5f,0,0, 0.5f,0,0,
					     0,0.3f,0, 0,0.3f,0, 0,0.3f,0, 0,0.3f,0,
					     0,0.3f,0.3f, 0,0.3f,0.3f, 0,0.3f,0.3f, 0,0.3f,0.3f,
					     0,0.3f,0.3f, 0,0.3f,0.3f, 0,0.3f,0.3f, 0,0.3f,0.3f,};
			
			// The vertex normals 
			float n[] = {0,0,1, 0,0,1, 0,0,1, 0,0,1,			// front face
				         -1,0,0, -1,0,0, -1,0,0, -1,0,0,		// left face
					  	 0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1,		// back face
						 1,0,0, 1,0,0, 1,0,0, 1,0,0,			// right face
						 0,1,0, 0,1,0, 0,1,0, 0,1,0,			// top face
						 0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0};		// bottom face

			// Texture coordinates 
			float uv[] = {0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1,
					  0,0, 1,0, 1,1, 0,1};
			
			// The triangles (three vertex indices for each triangle)
			int indices[] = {0,2,3, 0,1,2,			// front face
							 4,6,7, 4,5,6,			// left face
							 8,10,11, 8,9,10,		// back face
							 12,14,15, 12,13,14,	// right face
							 16,18,19, 16,17,18,	// top face
							 20,22,23, 20,21,22};	// bottom face
			
			// A room around the cube, made out of an other cube
			float[] vRoom = new float[Array.getLength(v)];
			float[] nRoom = new float[Array.getLength(n)];
			float[] cRoom = new float[Array.getLength(c)];
			for(int i=0; i<Array.getLength(vRoom); i++) vRoom[i] = v[i] * (roomSize);
			for(int i=0; i<Array.getLength(nRoom); i++) nRoom[i] = n[i] * -1.f;
			for(int i=0; i<Array.getLength(cRoom); i++) cRoom[i] = c[i] * 0.5f;			
						
			VertexData vertexDataRoom = renderContext.makeVertexData(24);
			vertexDataRoom.addElement(cRoom, VertexData.Semantic.COLOR, 3);
			vertexDataRoom.addElement(vRoom, VertexData.Semantic.POSITION, 3);			
			vertexDataRoom.addElement(nRoom, VertexData.Semantic.NORMAL, 3);
			vertexDataRoom.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataRoom.addIndices(indices);
			
			// A smaller cube to show the tracked VR controller
			float[] vControllerCube = new float[Array.getLength(v)];
			float[] nControllerCube = new float[Array.getLength(n)];
			float[] cControllerCube = new float[Array.getLength(c)];
			for(int i=0; i<Array.getLength(vRoom); i++) vControllerCube[i] = v[i] * controllerSize;
			for(int i=0; i<Array.getLength(nRoom); i++) nControllerCube[i] = n[i];
			for(int i=0; i<Array.getLength(cRoom); i++) cControllerCube[i] = 0.4f;	
			VertexData vertexDataControllerCube = renderContext.makeVertexData(24);
			vertexDataControllerCube.addElement(cControllerCube, VertexData.Semantic.COLOR, 3);
			vertexDataControllerCube.addElement(vControllerCube, VertexData.Semantic.POSITION, 3);			
			vertexDataControllerCube.addElement(nControllerCube, VertexData.Semantic.NORMAL, 3);
			vertexDataControllerCube.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataControllerCube.addIndices(indices);			
			
			// A smaller cube to show the tracked VR controller (brighter, when triggered)
			float[] cControllerCubeTriggered = new float[Array.getLength(c)];
			for(int i=0; i<Array.getLength(cRoom); i++) cControllerCubeTriggered[i] = 0.8f;	
			VertexData vertexDataControllerCubeTriggered = renderContext.makeVertexData(24);
			vertexDataControllerCubeTriggered.addElement(cControllerCubeTriggered, VertexData.Semantic.COLOR, 3);
			vertexDataControllerCubeTriggered.addElement(vControllerCube, VertexData.Semantic.POSITION, 3);			
			vertexDataControllerCubeTriggered.addElement(nControllerCube, VertexData.Semantic.NORMAL, 3);
			vertexDataControllerCubeTriggered.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataControllerCubeTriggered.addIndices(indices);	
			
			// same controller cube with different colors, make it long and tin
			float[] vRacket = new float[Array.getLength(v)];
			for(int i=0; i<Array.getLength(vRoom)/3; i++){
				vRacket[3*i] =   controllerSize * v[3*i];
				vRacket[3*i+1] =   5.f * controllerSize * v[3*i+1];
				vRacket[3*i+2] =  20.f * controllerSize * v[3*i+2] - 0.2f;
				racketBoundsMax.x = Math.max(racketBoundsMax.x, vRacket[3*i]);
				racketBoundsMax.y = Math.max(racketBoundsMax.y, vRacket[3*i+1]);
				racketBoundsMax.z = Math.max(racketBoundsMax.z, vRacket[3*i+2]);
				racketBoundsMin.x = Math.min(racketBoundsMin.x, vRacket[3*i]);
				racketBoundsMin.y = Math.min(racketBoundsMin.y, vRacket[3*i+1]);
				racketBoundsMin.z = Math.min(racketBoundsMin.z, vRacket[3*i+2]);
			}
			VertexData vertexDataRacket = renderContext.makeVertexData(24);
			vertexDataRacket.addElement(cControllerCube, VertexData.Semantic.COLOR, 3);
			vertexDataRacket.addElement(vRacket, VertexData.Semantic.POSITION, 3);			
			vertexDataRacket.addElement(nControllerCube, VertexData.Semantic.NORMAL, 3);
			vertexDataRacket.addElement(uv, VertexData.Semantic.TEXCOORD, 2);
			vertexDataRacket.addIndices(indices);
			
			//The ball
			Sphere ballObj = new Sphere(30, ballRadius, new float[]{0.5f,0.4f,0.1f}, new float[]{0.2f,0.3f,0.5f});
			VertexData vertexDataBall = renderContext.makeVertexData(ballObj.n);
			vertexDataBall.addElement(ballObj.colors, VertexData.Semantic.COLOR, 3);
			vertexDataBall.addElement(ballObj.vertices, VertexData.Semantic.POSITION, 3);			
			vertexDataBall.addElement(ballObj.normals, VertexData.Semantic.NORMAL, 3);
			vertexDataBall.addElement(ballObj.texcoords, VertexData.Semantic.TEXCOORD, 2);
			vertexDataBall.addIndices(ballObj.indices);	
			
			// Make a scene manager and add the objects
			sceneManager = new SimpleSceneManager();
			
			surroundingCube 		= new Shape(vertexDataRoom);
			controllerCube 			= new Shape(vertexDataControllerCube);		
			controllerCubeTriggered = new Shape(vertexDataControllerCubeTriggered);
			controllerRacket 		= new Shape(vertexDataRacket);
			ball 					= new Shape(vertexDataBall);
			

			sceneManager.addShape(surroundingCube);
			sceneManager.addShape(controllerCube);
			sceneManager.addShape(controllerCubeTriggered);
			sceneManager.addShape(controllerRacket);
			sceneManager.addShape(ball);
	
			throwingTranslationAccum = new Vector3f();
			
			// Set up the camera
			sceneManager.getCamera().setCenterOfProjection(new Vector3f(0,-1.f,-0.3f));
			sceneManager.getCamera().setLookAtPoint(new Vector3f(0,-1.f,0));
			sceneManager.getCamera().setUpVector(new Vector3f(0,1,0));

			// Add the scene to the renderer
			renderContext.setSceneManager(sceneManager);
	
			resetBallPosition(); //set inital ball position

		}
		
		public void dispose()
		{
			// Stop timer from triggering rendering of animation frames
			//timer.cancel();
			//timer.purge();
		}

		/*
		 * Helper function to visualise the controller corresponding to the hand.
		 * Gives visual feedback when trigger is pressed.
		 * Returns the trafo of the controller. 
		 */
		private Matrix4f visualizeHand(int index)
		{		
			Matrix4f handT = new Matrix4f();
			handT.setIdentity();

			if(index != -1) 
			{
				Matrix4f hiddenT = new Matrix4f();
    			Shape visibleShape, hiddenShape;
    			
    			// To have some feedback when pushing the trigger button we flip the two 
    			// "trigger" and "untrigger" shapes. The currently hidden object is 
    			// translated out of the viewfrustum since openGL does not have a direct 
    			// "make invisible" command for individual shapes w/o changing the jrtr
    			// pipeline.
    			if(renderPanel.getTriggerTouched(renderPanel.controllerIndexHand))
    			{
    				visibleShape = controllerCubeTriggered;
    				hiddenShape = controllerCube;
    			}
    			else
    			{
    				hiddenShape = controllerCubeTriggered;
    				visibleShape = controllerCube;	
    			}
    			
        		// Update pose of hand controller; note that the pose of the hand controller
        		// is independent of the scene camera pose, so we include the inverse scene
        		// camera matrix here to undo the camera trafo that is automatically applied
        		// by the renderer to all scene objects
    			handT = new Matrix4f(sceneManager.getCamera().getCameraMatrix());
    			handT.invert();
    			handT.mul(renderPanel.poseMatrices[index]);
	    		visibleShape.setTransformation(handT);
	    		
	    		//hidden shape is translated to "oblivion"
	    		hiddenT = new Matrix4f();
	    		hiddenT.setIdentity();
	    		hiddenT.setTranslation(new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE));
	    		hiddenShape.setTransformation(hiddenT);
    		}			
			return handT;
		}
		
		/*
		 * Helper function to visualise the controller corresponding to the racket.
		 * Returns the trafo of the controller. 
		 */
		private Matrix4f visualizeRacket(int index)
		{		
			Matrix4f racketT = new Matrix4f();
			racketT.setIdentity();
			if(index != -1) 
			{	
    			//current shape follows the controller
    			racketT = new Matrix4f(sceneManager.getCamera().getCameraMatrix());
    			racketT.invert();
    			racketT.mul(renderPanel.poseMatrices[index]);
    			controllerRacket.setTransformation(racketT);
    		}			
			return racketT;
		}

		/*
		 * Helper function: Reset ball Position if we press the side buttons of the "hand"
		 */
		private void resetBallPosition()
		{
			//reset Ball Position
			Matrix4f ballInitTrafo = ball.getTransformation();
			ballInitTrafo.setIdentity();
			
			//reset all other class members related to remembering previous positions of objects	
			throwingTranslationAccum = new Vector3f(0,-0.7f,0); //shift ball a bit downwards since the camera is at 0,-1,-0.3
		}
		
		/*
		 * Override from base class. Triggered by 90 FPS animation.
		 */
		public void prepareDisplay()
		{
    		// Reset ball position
    		if(renderPanel.getSideTouched(renderPanel.controllerIndexHand))
    		{
    			resetBallPosition();		
    		}
    		
			// get current ball transformation matrix.
    		Matrix4f ballTrafo = ball.getTransformation();		
    		
    		// Get VR tracked poses. Anything using any tracking data from the VR devices must happen *after* waitGetPoses() is called!
    		renderPanel.waitGetPoses();
    		
    		// Visualise controlling devices
    		Matrix4f handTrafo   = visualizeHand(renderPanel.controllerIndexHand);	
    		Matrix4f racketTrafo = visualizeRacket(renderPanel.controllerIndexRacket);	
 
    		
    		// TODO: implement interaction with ball

    		
    		//update ball transformation matrix (right now this only shifts the ball a bit down)
    		ballTrafo.setTranslation(throwingTranslationAccum);
    	}    	
}

	
	/**
	 * The main function opens a 3D rendering window, constructs a simple 3D
	 * scene, and starts a timer task to generate an animation.
	 */
	public static void main(String[] args)
	{		
		// Make a render panel. The init function of the renderPanel
		// (see above) will be called back for initialization.
		renderPanel = new SimpleVRRenderPanel();
		
		// Make the main window of this application and add the renderer to it
		JFrame jframe = new JFrame("simple");
		jframe.setSize(1680, 1680);
		jframe.setLocationRelativeTo(null); // center of screen
		jframe.getContentPane().add(renderPanel.getCanvas());// put the canvas into a JFrame window

		// Add a mouse listener
	  //  renderPanel.getCanvas().addMouseListener(new SimpleMouseListener());
		renderPanel.getCanvas().setFocusable(true);
		
	    jframe.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	    jframe.setVisible(true); // show window
	}
}
