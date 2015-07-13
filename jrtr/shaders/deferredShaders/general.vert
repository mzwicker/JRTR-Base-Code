#version 330
uniform mat4 projection; 
uniform mat4 modelview;

in vec4 position;
in vec2 texcoord;

// Output variables
out vec2 texCoords;

void main()
{
	texCoords = texcoord;
	gl_Position = projection * modelview * position;
}