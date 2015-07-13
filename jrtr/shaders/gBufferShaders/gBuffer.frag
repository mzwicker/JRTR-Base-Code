#version 330
uniform mat4 projection; 
uniform mat4 modelview;

in vec4 normalOut;
in vec4 colorOut;
in vec4 texcoordOut;

layout(location = 0) out vec4 normalColor;
layout(location = 1) out vec4 colorColor;
layout(location = 2) out vec4 texcoordColor;

void main(){
	normalColor = (normalOut+1.0)*.5;
	colorColor = colorOut;
	texcoordColor = texcoordOut;
}