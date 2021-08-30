package jrtr;

import jrtr.glrenderer.GLRenderPanel;
import jrtr.swrenderer.SWRenderPanel;

/**
 * An interface to display images that are rendered
 * by a render context (a "renderer"). Currently, this
 * interface is implemented by {@link GLRenderPanel} and 
 * {@link SWRenderPanel}. 
 */
public interface RenderPanel {

	/**
	 * This is a call-back that needs to be implemented by the user
	 * to initialize the renderContext.
	 */
	void init(RenderContext renderContext);
	
	/*
	 * Show the rendering window.
	 */
	void showWindow();
	
	/*
	 * Call back function that needs to be called at fixed time
	 * intervals by the render panel.
	 */
	void executeStep();	
}
