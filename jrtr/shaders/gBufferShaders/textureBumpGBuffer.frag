#version 330
uniform mat4 projection; 
uniform mat4 modelview;
uniform sampler2D myTexture;
uniform sampler2D bumpMap;

in vec4 positionOut;
in vec4 colorOut;
smooth in vec4 normalOut;
in vec2 texcoordOut;

layout(location = 0) out vec4 normalColor;
layout(location = 1) out vec4 colorColor;
layout(location = 2) out vec4 texcoordColor;

mat3 getInverseTangentSpace(in vec3 normal,in vec3 p,in vec2 uv)
{
    // get edge vectors of the pixel triangle
    vec3 dp1 = dFdx(p);
    vec3 dp2 = dFdy(p);
    vec2 duv1 = dFdx(uv);
    vec2 duv2 = dFdy(uv);
 
    // solve the linear system
    vec3 dp2perp = cross(dp2, normal);
    vec3 dp1perp = cross(dp1, normal);
    vec3 T = dp2perp * duv1.x + dp1perp * duv2.x;
    vec3 B = dp2perp * duv1.y + dp1perp * duv2.y;
 
    // construct a scale-invariant frame 
    float invmax = inversesqrt( max( dot(T,T), dot(B,B) ) );
    return mat3( T * invmax, B * invmax, normal);
}

void main(){
	//positionColor = positionOut;
   	vec3 bumpNormal = (texture2D(bumpMap, texcoordOut).rgb * 2.0) - 1.0;
	mat3 tangentSpace = getInverseTangentSpace(normalOut.xyz, -positionOut.xyz, texcoordOut);
	normalColor = vec4((normalize(tangentSpace * bumpNormal) + 1.0)*.5, 1.0);
	colorColor = colorOut*texture2D(myTexture, texcoordOut);
	texcoordColor = vec4(texcoordOut,0.0,0.0);
}