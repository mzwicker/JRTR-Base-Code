package jrtr.gldeferredrenderer;

import java.awt.Component;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import jrtr.RenderContext;
import jrtr.RenderPanel;
//import jrtr.utils.GPUProfiler;

public abstract class GLDeferredRenderPanel implements RenderPanel{

	/**
	 * Implementats the {@link RenderPanel} interface using
	 * OpenGL. Its purpose is to provide an AWT component that displays
	 * the rendered image. The class {@link GLRenderContext} performs the actual
	 * rendering. The user needs to extend this class and provide an 
	 * implementation for the <code>init</code> call-back function.
	 */

		/**
		 * An event listener for the GLJPanel to which this context renders.
		 * The main purpose of this event listener is to redirect display 
		 * events to the renderer (the {@link GLRenderContext}).
		 */
		protected class GLDeferredRenderContextEventListener implements GLEventListener
		{
			private GLDeferredRenderPanel renderPanel;
			private GLDeferredRenderContext renderContext;
			
			public GLDeferredRenderContextEventListener(GLDeferredRenderPanel renderPanel)
			{
				this.renderPanel = renderPanel;
			}
			
			/**
			 * Initialization call-back. Makes a render context (a renderer) using 
			 * the provided <code>GLAutoDrawable</code> and calls the user provided
			 * <code>init</code> of the render panel.
			 */
			public void init(GLAutoDrawable drawable)
			{
				// Make an OpenGL rendering context for deferred shading
				GLDeferredRenderContext.gl = drawable.getGL().getGL3();
				renderContext = new GLDeferredRenderContext();
				renderContext.init(drawable.getWidth(), drawable.getHeight());
				
				// Invoke the user-provided call back function
				renderPanel.init(renderContext);
			}
			
			/**
			 * Redirect the display event to the renderer.
			 */
			public void display(GLAutoDrawable drawable)
			{
				renderContext.display(drawable);
			}
			
			public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height)
			{
				renderContext.resize(drawable);
			}
			
			public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged)
			{
			}
			
			public void dispose(GLAutoDrawable g)
			{
				renderContext.dispose();
			}
		}

		/**
		 * Because of problems with the computers in the ExWi pool, we are using 
		 * <code>GLCanvas</code>, which is based on AWT, instead of 
		 * <code>GLJPanel</code>, which is based on Swing.
		 */
		private	GLCanvas canvas;

		public GLDeferredRenderPanel()
		{
		    canvas = new GLCanvas(new GLCapabilities(GLProfile.get(GLProfile.GL3)));
		    
			GLEventListener eventListener = new GLDeferredRenderContextEventListener(this);
			canvas.addGLEventListener(eventListener);
		}

		/**
		 * Return the AWT component that contains the rendered image. The user application
		 * needs to call this. The returned component is usually added to an application 
		 * window.
		 */
		public final Component getCanvas() 
		{
			return canvas;
		}

		/**
		 * This call-back function needs to be implemented by the user.
		 */
		abstract public void init(RenderContext renderContext);
}
