#version 330
uniform mat4 projection; 
uniform mat4 modelview;
uniform sampler2D diffuseMap;

in vec4 normalOut;
in vec4 colorOut;
in vec4 texcoordOut;

// Specify the buffer id for writing each output
layout(location = 0) out vec4 normalColor;
layout(location = 1) out vec4 colorColor;
layout(location = 2) out vec4 texcoordColor;

const vec4 zerovec4 = vec4(0,0,0,1);

void main(){
	normalColor = (normalOut+1.0)*.5;
	vec4 texColor = texture2D(diffuseMap, texcoordOut.xy);
	
	// If there is a diffuse texture (texColor is not black), use it
	// Otherwise, use color passed in from vertex shader
	if(any(notEqual(texColor, zerovec4)))
		colorColor = texColor;
	else
		colorColor = colorOut;
		
	texcoordColor = texcoordOut;
}