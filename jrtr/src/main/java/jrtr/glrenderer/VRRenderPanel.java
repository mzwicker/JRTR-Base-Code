package jrtr.glrenderer;

import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import javax.vecmath.Matrix4f;

import jrtr.*;

import java.awt.Component;
import java.nio.IntBuffer;

import com.jogamp.newt.opengl.GLWindow;
import com.jogamp.newt.awt.NewtCanvasAWT;

import jopenvr.JOpenVRLibrary;
import jopenvr.VR_IVRCompositor_FnTable;





import com.jogamp.opengl.util.FPSAnimator;





/**
 * An implementation of the {@link RenderPanel} interface using
 * OpenGL. Its purpose is to provide an AWT component that displays
 * the rendered image. The class {@link GLRenderContext} performs the actual
 * rendering. The user needs to extend this class and provide an 
 * implementation for the <code>init</code> call-back function.
 */
import jopenvr.*;

public abstract class VRRenderPanel implements RenderPanel {

	// Function table for access to OpenVR functions
	private static VR_IVRSystem_FnTable vrsystemFunctions;
	private static VR_IVRCompositor_FnTable vrcompositorFunctions;
	
	// Access to OpenVR native data structures containing 3D tracked device poses
	private static TrackedDevicePose_t.ByReference hmdTrackedDevicePoseReference;
	protected static TrackedDevicePose_t[] hmdTrackedDevicePoses;

	public static final VRControllerState_t[] cStates = new VRControllerState_t[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
	
	// Provides application access to data from OpenVR
	public boolean posesReady;
	public Matrix4f[] poseMatrices;
	public boolean[] poseValid;
	public Matrix4f headToLeftEye;
	public Matrix4f headToRightEye;
	public Matrix4f leftProjectionMatrix;
	public Matrix4f rightProjectionMatrix;
	public int controllerIndexHand, controllerIndexRacket;
	
	
	/**
	 * An event listener for the GLJPanel to which this context renders.
	 * The main purpose of this event listener is to redirect display 
	 * events to the renderer (the {@link GLRenderContext}).
	 */
	private class VRRenderContextEventListener implements GLEventListener
	{
		private VRRenderPanel renderPanel;
		private VRRenderContext renderContext;
		
		public VRRenderContextEventListener(VRRenderPanel renderPanel)
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
			for(int i=0;i<JOpenVRLibrary.k_unMaxTrackedDeviceCount;i++) {
				 cStates[i] = new VRControllerState_t();
		         cStates[i].setAutoSynch(false);
		         cStates[i].setAutoRead(false);
		         cStates[i].setAutoWrite(false);
				}
			
			renderContext = new VRRenderContext(drawable);
			// Invoke the user-provided call back function
			
			//renderContext = new VRRenderContext();
			renderContext.setOpenVR(vrcompositorFunctions, renderPanel);
		//	renderContext.init(drawable.getWidth(), drawable.getHeight());
			
			renderPanel.init(renderContext);		
			
			// Trigger 90 FPS display refresh
			final FPSAnimator animator = new FPSAnimator(renderPanel.glWindow, 90, true);
			animator.start();
			
		}
		
		/**
		 * Redirect the display event to the renderer.
		 */
		public void display(GLAutoDrawable drawable)
		{
			renderPanel.prepareDisplay();
			renderContext.display(drawable);
		}
		
		public void reshape(GLAutoDrawable drawable, int x, int y, int width,
		    int height)
		{
	//		renderContext.resize(drawable);
		}
		
		public void displayChanged(GLAutoDrawable drawable, boolean modeChanged,
		    boolean deviceChanged)
		{
		}
		
		public void dispose(GLAutoDrawable g)
		{
			renderContext.dispose(); //required if we don't use any FBOs?
			renderPanel.dispose();
			// This call makes sure we wait and give other threads enough time 
			// (like a timer that triggers rendering of animation frames) 
			// that may still call OpenVR functionality to finish first
			renderPanel.waitGetPoses();  
			JOpenVRLibrary.VR_ShutdownInternal();
		}
	}

	/**
	 * Because of problems with the computers in the ExWi pool, we are using 
	 * <code>GLCanvas</code>, which is based on AWT, instead of 
	 * <code>GLJPanel</code>, which is based on Swing.
	 */
	private	NewtCanvasAWT canvas;
	private GLWindow glWindow;

	public VRRenderPanel()
	{
		// Initizalize OpenVR
		
        byte b = JOpenVRLibrary.VR_IsHmdPresent();
        System.out.println("OpenVR library is present: " + b);
        
        IntBuffer hmdErrorStore = IntBuffer.allocate(1);
        JOpenVRLibrary.VR_InitInternal(hmdErrorStore, JOpenVRLibrary.EVRApplicationType.EVRApplicationType_VRApplication_Scene);
        
        // Get access to IVRSystem functions
        if( hmdErrorStore.get(0) == 0 ) {
            // Try and get the vrsystem pointer..
            vrsystemFunctions = new VR_IVRSystem_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRSystem_Version, hmdErrorStore));
        }
        if( vrsystemFunctions == null || hmdErrorStore.get(0) != 0 ) {
            System.out.println("OpenVR Initialize Result: " + JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));
            return;
        } 
        
        System.out.println("OpenVR System initialized & VR connected.");
        
        // Read function table
        vrsystemFunctions.setAutoSynch(false);
        vrsystemFunctions.read();
        
        IntBuffer width = IntBuffer.allocate(1);
        IntBuffer height = IntBuffer.allocate(1);
        vrsystemFunctions.GetRecommendedRenderTargetSize.apply(width, height);
        System.out.println("Target render size " + width.get(0) + " x " + height.get(0));
        
        
        // Get access to IVRCompositor functions
        vrcompositorFunctions = new VR_IVRCompositor_FnTable(JOpenVRLibrary.VR_GetGenericInterface(JOpenVRLibrary.IVRCompositor_Version, hmdErrorStore));
        if(vrcompositorFunctions == null || hmdErrorStore.get(0) != 0 ){
            System.out.println("OpenVR Initialize Result: " + JOpenVRLibrary.VR_GetVRInitErrorAsEnglishDescription(hmdErrorStore.get(0)).getString(0));
            return;
        } 

        System.out.println("OpenVR Compositor initialized.\n");

        vrcompositorFunctions.setAutoSynch(false);
        vrcompositorFunctions.read();
        
        vrcompositorFunctions.SetTrackingSpace.apply(JOpenVRLibrary.ETrackingUniverseOrigin.ETrackingUniverseOrigin_TrackingUniverseSeated);

        // Prepare tracking matrices
        hmdTrackedDevicePoseReference = new TrackedDevicePose_t.ByReference();
        hmdTrackedDevicePoses = (TrackedDevicePose_t[])hmdTrackedDevicePoseReference.toArray(JOpenVRLibrary.k_unMaxTrackedDeviceCount);
        poseMatrices = new Matrix4f[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
        poseValid = new boolean[JOpenVRLibrary.k_unMaxTrackedDeviceCount];
        for(int i=0;i<poseMatrices.length;i++) { 
        	poseMatrices[i] = new Matrix4f();
        	poseValid[i] = false;
        }
        
        // Disable all this stuff which kills performance
        hmdTrackedDevicePoseReference.setAutoRead(false);
        hmdTrackedDevicePoseReference.setAutoWrite(false);
        hmdTrackedDevicePoseReference.setAutoSynch(false);
        for(int i=0;i<JOpenVRLibrary.k_unMaxTrackedDeviceCount;i++) {
            hmdTrackedDevicePoses[i].setAutoRead(false);
            hmdTrackedDevicePoses[i].setAutoWrite(false);
            hmdTrackedDevicePoses[i].setAutoSynch(false);
        }
        
        posesReady = false;
        
        // Find index for one of the VR hand controller(s)
        // Find index for one of the VR hand controller(s)
        controllerIndexHand = -1;
        controllerIndexRacket = -1;
        for(int i=0;i<JOpenVRLibrary.k_unMaxTrackedDeviceCount;i++)
        {
        	int deviceClass = vrsystemFunctions.GetTrackedDeviceClass.apply(i);
        	if(deviceClass == JOpenVRLibrary.ETrackedDeviceClass.ETrackedDeviceClass_TrackedDeviceClass_Controller) 
        	{
        		System.out.println(i);
        		      
        		if (controllerIndexHand!=-1)
        			controllerIndexRacket = i;
        		else
        			controllerIndexHand = i; 
        	}
        }
        
		// Make sure we get a canvas with OpenGL 3 capabilities (omitting 
		// this leads to problems on Apple Macs)
	    glWindow = GLWindow.create(new GLCapabilities(GLProfile.get(GLProfile.GL3)));
	    canvas = new NewtCanvasAWT(glWindow);
	    
		GLEventListener eventListener = new VRRenderContextEventListener(this);
		glWindow.addGLEventListener(eventListener);
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
	
	public boolean getSideTouched(int idx){
		return cStates[idx].ulButtonTouched == 4 || cStates[idx].ulButtonTouched == 2;
	}
	
	public boolean getTriggerTouched(int idx){
		//we have access to the raw data. If the trigger is pushed down, ulButtonTouched=8589934592		
		return cStates[idx].ulButtonTouched == 8589934592l;
	}
	
	public void triggerHapticPulse(int idx, float t){
		vrsystemFunctions.TriggerHapticPulse.apply(idx, 0, (short)Math.round(3f * t / 1e-3f));
	}
	
	/**
	 * This call-back function needs to be implemented by the user.
	 */
	abstract public void init(RenderContext renderContext);
	
	/**
	 * May be overwritten by the user to clean up.
	 */
	public void dispose()
	{		

	}
	
	/**
	 * Wait and get 3D tracking poses (HMD and controllers) from OpenVR. Needs to be called before rendering
	 * and passing the rendered image to the OpenVR compositor.
	 */
	public void waitGetPoses()
	{
		vrcompositorFunctions.WaitGetPoses.apply(hmdTrackedDevicePoseReference, JOpenVRLibrary.k_unMaxTrackedDeviceCount, null, 0);
		posesReady = true;
		
		vrsystemFunctions.GetControllerState.apply(controllerIndexHand, cStates[controllerIndexHand]);
	    cStates[controllerIndexHand].readField("ulButtonTouched");
	    
		// Get head-to-eye transformations
		HmdMatrix34_t leftEyeToHead = vrsystemFunctions.GetEyeToHeadTransform.apply(0);
		headToLeftEye = new Matrix4f(leftEyeToHead.m[0], leftEyeToHead.m[1], leftEyeToHead.m[2], leftEyeToHead.m[3], 
        		leftEyeToHead.m[4], leftEyeToHead.m[5], leftEyeToHead.m[6], leftEyeToHead.m[7], 
        		leftEyeToHead.m[8], leftEyeToHead.m[9], leftEyeToHead.m[10], leftEyeToHead.m[11], 
                0f, 0f, 0f, 1f);
        headToLeftEye.invert();
        HmdMatrix34_t rightEyeToHead = vrsystemFunctions.GetEyeToHeadTransform.apply(1);
        headToRightEye = new Matrix4f(rightEyeToHead.m[0], rightEyeToHead.m[1], rightEyeToHead.m[2], rightEyeToHead.m[3], 
        		rightEyeToHead.m[4], rightEyeToHead.m[5], rightEyeToHead.m[6], rightEyeToHead.m[7], 
        		rightEyeToHead.m[8], rightEyeToHead.m[9], rightEyeToHead.m[10], rightEyeToHead.m[11], 
                0f, 0f, 0f, 1f);
        headToRightEye.invert();

        // Get projection matrices
        HmdMatrix44_t lPr = vrsystemFunctions.GetProjectionMatrix.apply(0, .1f, 40.f, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
        leftProjectionMatrix = new Matrix4f(lPr.m[0], lPr.m[1], lPr.m[2], lPr.m[3], 
        		lPr.m[4], lPr.m[5], lPr.m[6], lPr.m[7], 
        		lPr.m[8], lPr.m[9], lPr.m[10], lPr.m[11], 
        		lPr.m[12], lPr.m[13], lPr.m[14], lPr.m[15]);
        HmdMatrix44_t rPr = vrsystemFunctions.GetProjectionMatrix.apply(1, .1f, 40.f, JOpenVRLibrary.EGraphicsAPIConvention.EGraphicsAPIConvention_API_OpenGL);
        rightProjectionMatrix = new Matrix4f(rPr.m[0], rPr.m[1], rPr.m[2], rPr.m[3], 
        		rPr.m[4], rPr.m[5], rPr.m[6], rPr.m[7], 
        		rPr.m[8], rPr.m[9], rPr.m[10], rPr.m[11], 
        		rPr.m[12], rPr.m[13], rPr.m[14], rPr.m[15]);
        
		// Read tracked poses data from native
        for (int nDevice = 0; nDevice < JOpenVRLibrary.k_unMaxTrackedDeviceCount; ++nDevice ){
            hmdTrackedDevicePoses[nDevice].readField("bPoseIsValid");
            if( hmdTrackedDevicePoses[nDevice].bPoseIsValid != 0 ){
                hmdTrackedDevicePoses[nDevice].readField("mDeviceToAbsoluteTracking");
                                
                HmdMatrix34_t hmdMatrix = hmdTrackedDevicePoses[nDevice].mDeviceToAbsoluteTracking;
                poseMatrices[nDevice] = new Matrix4f(hmdMatrix.m[0], hmdMatrix.m[1], hmdMatrix.m[2], hmdMatrix.m[3], 
                		hmdMatrix.m[4], hmdMatrix.m[5], hmdMatrix.m[6], hmdMatrix.m[7], 
                        hmdMatrix.m[8], hmdMatrix.m[9], hmdMatrix.m[10], hmdMatrix.m[11], 
                        0f, 0f, 0f, 1f);
                poseValid[nDevice] = true;
            } else {
            	poseValid[nDevice] = false;
            }
        }
	}
	
	// To be over-ridden by implementations of this class
	public void prepareDisplay()
	{		
	}
}

