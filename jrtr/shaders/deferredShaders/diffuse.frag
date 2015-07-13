#version 330
varying vec2 texCoords;
#define MAX_POS_LIGHTS 10
#define MAX_DIR_LIGHTS 10
#define MAX_SPOT_LIGHTS 10
#define PI 3.1415926535897932384626433832795

uniform vec3 positionLightPosition[MAX_POS_LIGHTS];
uniform vec3 positionLightAttenuation[MAX_POS_LIGHTS];
uniform vec3 positionLightColor[MAX_POS_LIGHTS];

uniform vec3 dirLightColor[MAX_DIR_LIGHTS];
uniform vec3 dirLightDirection[MAX_DIR_LIGHTS];

uniform vec3 spotLightPosition[MAX_SPOT_LIGHTS];
uniform vec3 spotLightDirection[MAX_SPOT_LIGHTS];
uniform vec3 spotLightAttenuation[MAX_SPOT_LIGHTS];
uniform vec3 spotLightColor[MAX_SPOT_LIGHTS];
uniform float spotLightAngle[MAX_SPOT_LIGHTS];

uniform sampler2D normals;
uniform sampler2D depth;
uniform mat4 invProj;
uniform mat4 proj;

const vec3 ambient = vec3(0.1);
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
	vec3 normalColor = texture2D(normals, texCoords).rgb;
	vec3 pos = getPositionFromDepth();//texture2D(positions, texCoords).xyz;
	
	vec3 n = normalColor*2.0 - 1.0;
	vec3 sum = vec3(0.0);
	for(int i = 0; i< MAX_POS_LIGHTS; i++){
		vec3 deltaPos = positionLightPosition[i] - pos;
		vec3 lDirN = normalize(deltaPos);
		float d = length(deltaPos);
		float att = 1.0/(d*d*positionLightAttenuation[i].z + d*positionLightAttenuation[i].y + positionLightAttenuation[i].x);
		float lambert = clamp(dot(n, lDirN), 0.0, 1.0);
		vec3 intensity = positionLightColor[i] * lambert * att;
		sum += intensity;
	}
	
	for(int i = 0; i< MAX_DIR_LIGHTS; i++){
		vec3 lDirN = normalize(dirLightDirection[i]);
		float lambert = clamp(dot(n, lDirN), 0.0, 1.0);
		sum += dirLightColor[i]  * lambert;
	}
	sum += ambient;
	
	gl_FragColor = vec4(sum, 1.0);
}