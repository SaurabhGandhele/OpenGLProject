#version 150

// Texture mapping vertex shader
//
// Contributor:  Saurabh Gandhele

// INCOMING DATA

// add all variables containing data used by the
// fragment shader for lighting and texture mapping

uniform sampler2D smileyface;
uniform sampler2D frownyface;
in vec2 point;

// OUTGOING DATA

out vec4 fragmentColor;

void main()
{
    // Replace with proper texture mapping code
    fragmentColor = vec4( 1.0, 1.0, 1.0, 1.0 );
	
	// gl_FrontFacing provided in lab description to choose the front side for smiley
	// else choose the back side for frowny
	
	if(gl_FrontFacing)
		fragmentColor=texture(smileyface, point);
	else
		fragmentColor=texture(frownyface, point);
}
