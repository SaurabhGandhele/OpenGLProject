package Zproj;


//
// finalMain.java
//
//  Copyright 2016 Rochester Institute of Technology.  All rights reserved.
//
// Main class for lighting/shading/texturing assignment.
//
// Contributor:  Saurabh Gandhele

// Referred from Lab6 textingmain module and added new objects
//

import java.awt.*;
import java.io.IOException;
import java.nio.*;
import java.awt.event.*;


import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.Animator;


public class finalMain implements GLEventListener, KeyListener
{

    ///
    // dimensions of the drawing window
    ///
    private static int w_width  = 900;
    private static int w_height = 780;

    ///
    // buffer info
    ///

    ///
    // We need two vertex buffers and four element buffers:
    // one set for the quad (texture mapped), and one set
    // for the teapot (Phong shaded)
    private BufferSet quadBuffers;
    private BufferSet coneBuffers;
    private BufferSet sphereBuffers;
    private BufferSet cylinderBuffers;
    ///
    // Animation control
    ///
    Animator anime;
    boolean animating;

    ///
    // Initial animation rotation angles
    ///
    float angles[];

    ///
    // Program IDs...for shader programs
    ///
    public int flatshade;
    public int phongshade;
    public int textshade;
    public int gouraudshade;

    ///
    // Shape info
    ///

    ///
    // Lighting information
    ///
    Lighting flatlight;
    Lighting phonglight;
    Lighting gouraudlight;
    ///
    // Viewing information
    ///
    Viewing myView;

    ///
    // Texturing information
    ///
    Textures myTexture;

    ///
    // canvas and shape info
    ///
    GLCanvas myGLCanvas;
    Canvas canvas;
    Shapes myShape;
    ShaderSetup myShaders;
    private static Frame frame;
    private boolean updateNeeded;

    ///
    // Constructor
    ///
    public finalMain( GLCanvas G )
    {
        myGLCanvas = G;

	// initialize our various parameters

        angles = new float[8];

        animating = false;


		angles[0] = 30.0f;
		angles[1] = 90.0f;
		angles[2] = 060.0f;
		angles[3] = 45.0f;

        // Initialize lighting, view, etc.
	canvas = new Canvas( w_width, w_height );
	myShaders = new ShaderSetup();
        phonglight = new Lighting();
        gouraudlight = new Lighting();
        flatlight = new Lighting();
        myView = new Viewing();
	myTexture = new Textures();
	myShape = new Shapes( canvas );
	quadBuffers = new BufferSet();
	coneBuffers = new BufferSet();
	sphereBuffers = new BufferSet();
	cylinderBuffers = new BufferSet();

        // Set up event listeners
        G.addGLEventListener (this);
        G.addKeyListener (this);
    }


    ///
    // create a buffer
    ///
    private int makeBuffer( GL3 gl3, int target, Buffer data, long size )
    {
        int buffer[] = new int[1];

        gl3.glGenBuffers( 1, buffer, 0 );
        gl3.glBindBuffer( target, buffer[0] );
        gl3.glBufferData( target, size, data, GL.GL_STATIC_DRAW );

        return( buffer[0] );
    }

    ///
    // Create a set of vertex and element buffers
    ///
    private void createBuffers( GL3 gl3, BufferSet B, Canvas C ) {

        // get the vertices
        B.numElements = C.nVertices();
        Buffer points = C.getVertices();
        // #bytes = number of elements * 4 floats/element * bytes/float
        B.vSize = B.numElements * 4l * 4l;
        long vbufferSize = B.vSize;

        // get the normals
        Buffer normals = C.getNormals();
        B.nSize = B.numElements * 3l * 4l;
        vbufferSize += B.nSize;

        // get the UV data (if any)
        Buffer uv = C.getUV();
	B.tSize = 0;
	if( uv != null ) {
            B.tSize = B.numElements * 2l * 4l;
	}
	vbufferSize += B.tSize;

        // get the element data
        Buffer elements = C.getElements();
        B.eSize = B.numElements * 4l;

        // set up the vertex buffer
        if( B.bufferInit ) {
            // must delete the existing buffers first
            int buf[] = new int[2];
            buf[0] = B.vbuffer;
            buf[1] = B.ebuffer;
            gl3.glDeleteBuffers( 2, buf, 0 );
            B.bufferInit = false;
        }

        // first, create the connectivity data
        B.ebuffer = makeBuffer( gl3, GL.GL_ELEMENT_ARRAY_BUFFER,
                                elements, B.eSize );

        // next, the vertex buffer, containing vertices and "extra" data
        B.vbuffer = makeBuffer( gl3, GL.GL_ARRAY_BUFFER, null, vbufferSize );
	gl3.glBufferSubData( GL.GL_ARRAY_BUFFER, 0, B.vSize, points );
	gl3.glBufferSubData( GL.GL_ARRAY_BUFFER, B.vSize, B.nSize, normals );
	if( uv != null ) {
	    gl3.glBufferSubData( GL.GL_ARRAY_BUFFER, B.vSize+B.nSize,
	        B.tSize, uv );
	}

        // finally, mark it as set up
        B.bufferInit = true;

    }


    ///
    // creates a new shape
    ///
    public void createShape( GL3 gl3, int obj, BufferSet B )
    {
        // clear the old shape
        canvas.clear();

        // make the shape
        myShape.makeShape( obj );

	// create the necessary buffers
        createBuffers( gl3, B, canvas );

    }


    ///
    // Bind the correct vertex and element buffers
    //
    // Assumes the correct shader program has already been enabled
    ///
    private void selectBuffers( GL3 gl3, int program, int obj, BufferSet B )
    {

        gl3.glBindBuffer( GL.GL_ARRAY_BUFFER, B.vbuffer );
        gl3.glBindBuffer( GL.GL_ELEMENT_ARRAY_BUFFER, B.ebuffer );

        // set up the vertex attribute variables
        int vPosition = gl3.glGetAttribLocation( program, "vPosition" );
        gl3.glEnableVertexAttribArray( vPosition );
        gl3.glVertexAttribPointer( vPosition, 4, GL.GL_FLOAT, false,
                                       0, 0l );

        int vNormal = gl3.glGetAttribLocation( program, "vNormal" );
        gl3.glEnableVertexAttribArray( vNormal );
        gl3.glVertexAttribPointer( vNormal, 3, GL.GL_FLOAT, false,
                                   0, B.vSize );

	if( obj == Shapes.OBJ_QUAD ) {
            int vTexCoord = gl3.glGetAttribLocation( program, "vTexCoord" );
            gl3.glEnableVertexAttribArray( vTexCoord );
            gl3.glVertexAttribPointer( vTexCoord, 2, GL.GL_FLOAT, false,
                                       0, B.vSize+B.nSize );
	}

    }


    ///
    // verify shader creation
    ///
    private void checkShaderError( ShaderSetup myShaders, int program,
        String which )
    {
        if( program == 0 ) {
            System.err.println( "Error setting " + which +
                " shader - " +
                myShaders.errorString(myShaders.shaderErrorCode)
            );
            System.exit( 1 );
        }
    }

    ///
    // OpenGL initialization
    ///
    public void init( GLAutoDrawable drawable )
    {
        // get the gl object
        GL3 gl3 = drawable.getGL().getGL3();

        // create the Animator now that we have the drawable
        anime = new Animator( drawable );

	// Load texture image(s)
	try {
		myTexture.loadTexture( gl3 );
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}

        textshade = myShaders.readAndCompile(gl3,"texture.vert","texture.frag");
        checkShaderError( myShaders, textshade, "texture" );

        phongshade = myShaders.readAndCompile( gl3, "phong.vert", "phong.frag" );
        checkShaderError( myShaders, phongshade, "phong" );
        
        gouraudshade = myShaders.readAndCompile( gl3, "gouraudshader.vert", "gouraudshader.frag" );
        checkShaderError( myShaders, gouraudshade, "gouraud" );
        
        flatshade = myShaders.readAndCompile( gl3, "flatshader.vert", "flatshader.frag" );
        checkShaderError( myShaders, flatshade, "flat" );
        // Create all our objects
        createShape( gl3, Shapes.OBJ_QUAD, quadBuffers );
        createShape( gl3, Shapes.OBJ_CONE, coneBuffers );
        createShape( gl3, Shapes.OBJ_SPHERE, sphereBuffers );
        createShape( gl3, Shapes.OBJ_CYLINDER, cylinderBuffers );
        // Other GL initialization
        gl3.glEnable( GL.GL_DEPTH_TEST );
        gl3.glFrontFace( GL.GL_CCW );
	gl3.glPolygonMode( GL.GL_FRONT_AND_BACK, GL3.GL_FILL );
        gl3.glClearColor( 0.0f, 0.0f, 0.0f, 0.0f );
        gl3.glDepthFunc( GL.GL_LEQUAL );
        gl3.glClearDepth( 1.0f );

    }

    ///
    // Called by the drawable to initiate OpenGL rendering by the client.
    ///
    public void display( GLAutoDrawable drawable )
    {
        // get GL
        GL3 gl3 = (drawable.getGL()).getGL3();

        // clear and draw params..
        gl3.glClear( GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT );

        // now, draw the sphere
        		gl3.glUseProgram(gouraudshade);

        		// set up viewing and projection parameters
        		myView.setUpFrustum(gouraudshade, gl3);

        		// set up the Phong shading information
        		gouraudlight.setUpPhong(gouraudshade, gl3);

        		// set up the camera
        		myView.setUpCamera( gouraudshade, gl3,
        		        0.5f, 2.8f, 7.3f,
        		        0, 1, 0,
        		        0, 1, 0
        		    );
        		
        		// set up the transformations
        		myView.setUpTransforms(gouraudshade, gl3,
        				0.4f, 0.4f, 0.4f,
        				0, 
        				-angles[Shapes.OBJ_SPHERE], 
        				-angles[Shapes.OBJ_SPHERE],  
        				-0.01f, 2.3f, 0.0f);

        		// draw it
        		selectBuffers( gl3, gouraudshade, Shapes.OBJ_SPHERE, sphereBuffers );
               gl3.glDrawElements( GL.GL_TRIANGLES, (int) sphereBuffers.vSize,
                 GL.GL_UNSIGNED_INT, 0l            );
               
            // now, draw the outer ring
               
               gl3.glUseProgram(phongshade);

        		// set up viewing and projection parameters
        		myView.setUpFrustum(phongshade, gl3);

        		// set up the Phong shading information
        		phonglight.setUpPhong(phongshade, gl3);

        		// set up the camera
        		myView.setUpCamera( phongshade, gl3,
        		        0.5f, 2.8f, 7.3f,
        		        0, 1, 0,
        		        0, 1, 0
        		    );
       		myView.setUpTransforms(phongshade, gl3, 
       				3.3f, 3.3f, 3.3f, 
       				0, 
       				0,
       				-angles[Shapes.OBJ_CYLINDER],
       				0.0f, 0.0f, 0.0f);
        		// draw it
        		selectBuffers( gl3, phongshade, Shapes.OBJ_CYLINDER, cylinderBuffers );
               gl3.glDrawElements( GL.GL_TRIANGLES, (int) cylinderBuffers.vSize,
                 GL.GL_UNSIGNED_INT, 0l            );
               

        
        // first, the quad
        gl3.glUseProgram( textshade );

        // set up viewing and projection parameters
        myView.setUpFrustum( textshade, gl3 );
	
	// set up the texture information
	myTexture.setUpTexture( textshade, gl3 );

        // set up the camera
        myView.setUpCamera( textshade, gl3,
            0.5f, 2.8f, 7.3f,
            0, 1, 0,
            0, 1, 0
        );

        // set up transformations for the quad
        myView.setUpTransforms( textshade, gl3,
            0.8f, 0.8f, 0.8f,
            0.2f,
            angles[Shapes.OBJ_QUAD],
            0.2f,
            -1.5f, 2.5f, 0.0f
        );

        // draw it
        selectBuffers( gl3, textshade, Shapes.OBJ_QUAD, quadBuffers );
        gl3.glDrawElements( GL.GL_TRIANGLES, (int) quadBuffers.vSize,
            GL.GL_UNSIGNED_INT, 0l
        );

        // draw the cone
	
        gl3.glUseProgram( flatshade );

        // set up viewing and projection parameters
        myView.setUpFrustum( flatshade, gl3 );

	// set up the Phong shading information
	flatlight.setUpPhong( flatshade, gl3 );

        
    // set up the camera
    myView.setUpCamera( flatshade, gl3,
        0.5f, 2.8f, 7.3f,
        0, 1, 0,
        0, 1, 0
    );
    
 // set up the transformations
        myView.setUpTransforms( flatshade, gl3,
            0.6f, 0.6f, 0.6f,
           0,
           -angles[Shapes.OBJ_CONE],
            0,
            1.6f, 2.5f, 0
        );

        // draw it
        selectBuffers( gl3, flatshade, Shapes.OBJ_CONE, coneBuffers );
        gl3.glDrawElements( GL.GL_TRIANGLES, (int) coneBuffers.vSize,
            GL.GL_UNSIGNED_INT, 0l
        );

        	// draw the inner ring
              gl3.glUseProgram(gouraudshade);

     		// set up viewing and projection parameters
     		myView.setUpFrustum(gouraudshade, gl3);

     		// set up the Phong shading information
     		gouraudlight.setUpPhong(gouraudshade, gl3);

     		// set up the camera
     		myView.setUpCamera( gouraudshade, gl3,
     		        0.5f, 2.8f, 7.3f,
     		        0, 1, 0,
     		        0, 1, 0
     		    );
    		myView.setUpTransforms(gouraudshade, gl3, 
    				2.2f, 2.2f, 2.2f, 
    				0, 
    				angles[Shapes.OBJ_CYLINDER],
    				angles[Shapes.OBJ_CYLINDER],
    				0.0f, 0.0f, 0.0f);
     		// draw it
     		selectBuffers( gl3, gouraudshade, Shapes.OBJ_CYLINDER, cylinderBuffers );
            gl3.glDrawElements( GL.GL_TRIANGLES, (int) cylinderBuffers.vSize,
              GL.GL_UNSIGNED_INT, 0l            );
            
            
        // perform any required animation for the next time
        if( animating ) {
            animate();
        }
    }

    ///
    // Notifies the listener to perform the release of all OpenGL
    // resources per GLContext, such as memory buffers and GLSL
    // programs.
    ///
    public void dispose(GLAutoDrawable drawable)
    {
    }


    ///
    // Create a vertex or element array buffer
    //
    // @param target - which type of buffer to create
    // @param data   - source of data for buffer (or null)
    // @param size   - desired length of buffer
    ///
    int makeBuffer( int target, Buffer data, int size, GL3 gl3 )
    {
        int buffer[] = new int[1];

        gl3.glGenBuffers( 1, buffer, 0 );
        gl3.glBindBuffer( target, buffer[0] );
        gl3.glBufferData( target, size, null, GL.GL_STATIC_DRAW );

	return( buffer[0] );
    }

    ///
    // Called by teh drawable during the first repaint after the component
    // has been resized.
    ///
    public void reshape( GLAutoDrawable drawable, int x, int y, int width,
                         int height )
    {
    }


    ///
    // Because I am a Key Listener...we'll only respond to key presses
    ///
    public void keyTyped(KeyEvent e){}
    public void keyReleased(KeyEvent e){}

    ///
    // Invoked when a key has been pressed.
    ///
    public void keyPressed(KeyEvent e)
    {
        // Get the key that was pressed
        char key = e.getKeyChar();

        // Respond appropriately
        switch( key ) {

            case 'a':    // animate
                animating = true;
                anime.start();
                break;

            case 'r':    // reset rotations
            	angles[0] = 30.0f;
        		angles[1] = 90.0f;
        		angles[2] = 060.0f;
        		angles[3] = 45.0f;
                break;

            case 's':    // stop animating
                animating = false;
                anime.stop();
                break;

            case 'q': case 'Q':
            	frame.dispose();
                System.exit( 0 );
                break;
        }

        // do a redraw
        myGLCanvas.display();
    }

    ///
    // Simple animate function
    ///
    public void animate() { // set the speed of rotation of objects
        angles[Shapes.OBJ_QUAD]   += 0.6;
        angles[Shapes.OBJ_CONE] += 0.2;
        angles[Shapes.OBJ_SPHERE] += 0.8;
        angles[Shapes.OBJ_CYLINDER] += 0.5;
    }


    ///
    // main program
    ///
    public static void main(String [] args)
    {
        // GL setup
        GLProfile glp = GLProfile.get( GLProfile.GL3 );
        GLCapabilities caps = new GLCapabilities( glp );
        GLCanvas canvas = new GLCanvas( caps );
 
        // create your tessMain
        finalMain myMain = new finalMain( canvas );

        frame = new Frame( "Final Project - Shading, Lighting and Texturing of Objects");
        frame.setSize( w_width, w_height );
        frame.add( canvas );
        frame.setVisible( true );

        // by default, an AWT Frame doesn't do anything when you click
        // the close button; this bit of code will terminate the program when
        // the window is asked to close
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
		frame.dispose();
                System.exit(0);
            }
        });
    }
}
