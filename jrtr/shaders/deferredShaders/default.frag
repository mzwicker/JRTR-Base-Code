#version 330
uniform sampler2D myTexture;
// Default fragment shader

// Input variable, passed from vertex to fragment shader
// and interpolated automatically to each fragment
in vec4 frag_color;
in vec2 texCoords;

void main()
{
	vec4 texColor = texture2D(myTexture, texCoords);
	gl_FragColor = frag_color*texColor;
}