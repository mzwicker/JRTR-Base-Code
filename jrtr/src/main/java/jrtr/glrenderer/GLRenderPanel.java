package jrtr.glrenderer;

import java.nio.*;

import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL45.*;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

import jrtr.RenderPanel;
import jrtr.RenderContext;

/**
 * Implementats the {@link RenderPanel} interface using
 * OpenGL. The interface to OpenGL and the native window system
 * are provided by the LWJGL and GLFW libraries. The class 
 * {@link GLRenderContext} performs the actual rendering. Note
 * that all OpenGL calls apply to the GLFW window that is created
 * by the current thread. 
 * 
 * The user needs to extend this class and provide an 
 * implementation for the <code>init</code> call-back function.
 */
public abstract class GLRenderPanel implements RenderPanel {

	// The window handle
	protected long window;
	
	// Fixed time step to perform some periodic tasks 
	protected double timeStep;
	
	private GLRenderContext renderContext;
	
	public GLRenderPanel()
	{		
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if ( !glfwInit() )
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(512, 512, "OpenGL Render Window", NULL, NULL);
		if ( window == NULL )
			throw new RuntimeException("Failed to create the GLFW window");		
		
		// Get the thread stack and push a new frame
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(
				window,
				(vidmode.width() - pWidth.get(0)) / 2,
				(vidmode.height() - pHeight.get(0)) / 2
			);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);
	}

	public void showWindow()
	{
		// Make the window visible
		glfwShowWindow(window);		
		
		// Resize OpenGL viewport when window is resized
		glfwSetFramebufferSizeCallback(window, (window, width, height) -> glViewport(0, 0, width, height));
		
		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();		

		// Call user defined initialization 
		renderContext = new GLRenderContext();
		init(renderContext);

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		double t0 = glfwGetTime();
		timeStep = 0.01d;
		while ( !glfwWindowShouldClose(window) ) {

			// Execute next time step
			double t1 = glfwGetTime();
			if(t1-t0 > timeStep)
			{
				executeStep();
				t0=t1;
			}			
			renderContext.display();
			
			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}
		
	/**
	 * This call-back function needs to be implemented by the user.
	 */
	abstract public void init(RenderContext renderContext);
}
