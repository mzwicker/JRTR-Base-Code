#version 330
uniform mat4 projection; 
uniform mat4 modelview;

in vec4 position;
in vec3 normal;
in vec3 color;
in vec2 texcoord;

// Output variables
out vec4 normalOut;
out vec4 colorOut;
out vec2 texcoordOut;

void main(){
	normalOut = normalize(modelview * vec4(normal, 0.0));
	colorOut = vec4(color, 1.0);
	texcoordOut =  texcoord;
	gl_Position = projection * modelview * position;
}