#version 330

in vec2 texCoords;
#define MAX_DIR_LIGHTS 4

uniform vec3 dirLightColor[MAX_DIR_LIGHTS];
uniform vec3 dirLightDirection[MAX_DIR_LIGHTS];

uniform sampler2D color;
uniform sampler2D normals;
uniform sampler2D depth;
uniform mat4 invProj;
uniform mat4 proj;

const vec2 oneVec = vec2(1.0);

vec3 getPositionFromDepth(){
	ivec2 size = textureSize(depth, 0);
	vec4 positionOnScreen = vec4(0.0);
	positionOnScreen.xy = (texCoords * 2.0 - oneVec);
	positionOnScreen.z = texture2D(depth, texCoords).r*2.0 - 1.0;
	float t1 = proj[2][2];
	float t2 = proj[3][2];
	float camZ = -t2/(positionOnScreen.z + t1); 
	positionOnScreen.w = -camZ;
	positionOnScreen.xyz *= positionOnScreen.w;
	vec4 p = invProj*positionOnScreen;
	return p.xyz;
}

void main()
{	
	vec3 c = texture2D(color, texCoords).rgb;
	
	vec3 normalColor = texture2D(normals, texCoords).rgb;
	vec3 n = normalColor*2.0 - 1.0;
	
	vec3 pos = getPositionFromDepth();//texture2D(positions, texCoords).xyz;
		
	vec3 sum = vec3(0.0);
	for(int i = 0; i< MAX_DIR_LIGHTS; i++){
		vec3 lDirN = normalize(dirLightDirection[i]);
		float lambert = clamp(dot(n, lDirN), 0.0, 1.0);
		sum += dirLightColor[i]  * lambert * c;
	}
	
	gl_FragColor = vec4(sum, 1.0);
}