#version 330
uniform mat4 projection; 
uniform mat4 modelview;

in vec4 position;
in vec3 normal;
in vec3 color;
in vec2 texcoord;

// Output variables
out vec4 positionOut;
out vec4 colorOut;
out vec2 texcoordOut;
smooth out vec4 normalOut;

void main(){
	positionOut = modelview * position;
	normalOut = normalize(modelview * vec4(normal, 0.0));
	colorOut = vec4(color, 1.0);
	texcoordOut =  texcoord;
	gl_Position = projection * positionOut;
}