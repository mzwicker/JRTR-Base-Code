#version 150
// Fragment shader for "pseudo normal shading": Show z-coordinates
// of camera space normal as gray scale color

// Input variable, passed from vertex to fragment shader
// and interpolated automatically to each fragment
in vec4 frag_normal;

// Output variable, will be written to framebuffer automatically
out vec4 out_color;

void main()
{		
	out_color = vec4(frag_normal.z, frag_normal.z, frag_normal.z, 0);
}

