package jrtr;

import java.lang.reflect.Array;

public class Sphere {

	public float[] vertices;
	public int[] indices;
	public float[] colors;
	public float[] normals;
	public float[] texcoords;
	public int n;
	
	public Sphere(int s, float rad, float[] color1, float[] color2)
	{
		
		if(Array.getLength(color1)<3 || Array.getLength(color2)<3){
			System.out.println("illegal color for sphere");
			color1 = new float[]{0,0,0};
			color2 = new float[]{1,1,1};
		}
		
		//s=3;
		n = s*s;
		vertices = new float[3*s*s];
		indices = new int[6*s*s];
		colors = new float[3*s*s];
		normals = new float[3*s*s];
		texcoords = new float[2*s*s];
		
		for(int i=0;i<s;i++)
		{
			float theta = i*(float)(2.f*Math.PI/s);
			for(int j=0;j<s;j++)
			{
				float sigma = j*(float)(Math.PI/(s-1));
				
				float x = (float)(Math.sin(sigma)*Math.cos(theta));
				float y = (float)(Math.sin(sigma)*Math.sin(theta));
				float z = (float)Math.cos(sigma);
				
				vertices[3*(i*s+j)+0] = rad*x;
				vertices[3*(i*s+j)+1] = rad*y;
				vertices[3*(i*s+j)+2] = rad*z;
				
				indices[6*(i*s+j)+0] = i*s 			+ j;
				indices[6*(i*s+j)+1] = ((i+1)%s)*s	+ (j+1)%s;
				indices[6*(i*s+j)+2] = i*s 			+ (j+1)%s;
				indices[6*(i*s+j)+3] = i*s 			+ j;
				indices[6*(i*s+j)+4] = ((i+1)%s)*s 	+ j;
				indices[6*(i*s+j)+5] = ((i+1)%s)*s 	+ (j+1)%s;
				
				colors[3*(i*s+j)+0] = color1[0]*(i%2) + color2[0]*((i+1)%2);
				colors[3*(i*s+j)+1] = color1[1]*(i%2) + color2[1]*((i+1)%2);
				colors[3*(i*s+j)+2] = color1[2]*(i%2) + color2[2]*((i+1)%2);
				
				//fix pole colors
				if(j==0){
					colors[3*(i*s+j)+0] = color1[0];
					colors[3*(i*s+j)+1] = color1[1];
					colors[3*(i*s+j)+2] = color1[2];
				}
				if(j==s-1){
					colors[3*(i*s+j)+0] = color2[0];
					colors[3*(i*s+j)+1] = color2[1];
					colors[3*(i*s+j)+2] = color2[2];
				}
				
				normals[3*(i*s+j)+0] = x;
				normals[3*(i*s+j)+1] = y;
				normals[3*(i*s+j)+2] = z;
				
				texcoords[2*(i*s+j)+0] = i/(float)s;
				texcoords[2*(i*s+j)+1] = j/(float)s;
				
			}
		}
	}
}
