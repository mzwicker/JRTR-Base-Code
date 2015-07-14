#version 330
// Default vertex shader for deferred shading.

// Uniform variables, set in main program
uniform mat4 projection; 
uniform mat4 modelview;

// Input vertex attributes; passed from main program to shader 
// via vertex buffer objects
in vec4 position;
in vec2 texcoord;

// Output variables
out vec2 texCoords;

void main()
{
	texCoords = texcoord;
	gl_Position = projection * modelview * position;
}
