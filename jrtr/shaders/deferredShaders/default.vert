#version 330
// Default vertex shader

// Uniform variables, set in main program
uniform mat4 projection; 
uniform mat4 modelview;

// Input vertex attributes; passed from main program to shader 
// via vertex buffer objects
in vec4 position;
in vec4 color;
in vec2 texcoord;

// Output variables
out vec4 frag_color;
out vec2 texCoordOut;

void main()
{
	texCoordOut = texcoord;
	frag_color = color;
	gl_Position = projection * modelview * position;
}
