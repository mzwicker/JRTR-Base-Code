#version 330
// Default fragment shader for deferred shading
// Passes through a texture "myTexture"

// The texture passed through by the shader
uniform sampler2D myTexture;

// Input variable, passed from vertex to fragment shader
// and interpolated automatically to each fragment
in vec2 texCoords;

void main()
{
	vec4 texColor = texture2D(myTexture, texCoords);
	gl_FragColor = texColor;
}