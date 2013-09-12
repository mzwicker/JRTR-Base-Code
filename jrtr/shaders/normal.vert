#version 150
// Fragment shader for "pseudo normal shading": Show z-coordinates
// of camera space normal as gray scale color

// Uniform variables, set in main program
uniform mat4 projection; 
uniform mat4 modelview;

// Input vertex attributes; passed from main program to shader 
// via vertex buffer objects
in vec4 position;
in vec4 color;
in vec3 normal;

// Output variables
out vec4 frag_normal;

void main()
{
	// Make sure the 4th component of the normal vector is 0,
	// transform normal to camera space, and pass to fragment shader
	frag_normal = modelview * vec4(normal, 0);
	
	// Note: gl_Position is a default output variable containing
	// the transformed vertex position
	gl_Position = projection * modelview * position;
}
