#version 330
uniform mat4 projection; 
uniform mat4 modelview;
uniform sampler2D myTexture;

in vec4 normalOut;
in vec4 colorOut;
in vec2 texcoordOut;

layout(location = 0) out vec4 normalColor;
layout(location = 1) out vec4 colorColor;
layout(location = 2) out vec4 texcoordColor;

void main(){
	//positionColor = positionOut;
	normalColor = (normalOut+1.0)*.5;
	colorColor = colorOut * texture2D(myTexture, texcoordOut);
	texcoordColor = vec4(texcoordOut,0.0,0.0);
}