#version 150

// Phong fragment shader
//
// Contributor:  Saurabh Gandhele

// INCOMING DATA

// add all variables containing data used by the
// fragment shader for lighting and shading

// uniform variables assigned in Lighting.java

uniform float ka;
uniform float kd;
uniform float ks;
uniform float specularExponent;

uniform vec4 ambLightScene;
uniform vec4 propLightblueColor;
uniform vec4 propLightPosition;
uniform vec4 ambMaterial1Color;
uniform vec4 diffMaterial1Color;
uniform vec4 specMaterial1Color;

// OUTGOING DATA
out vec4 fragmentColor;


// varying variables from phong.vert

varying vec3 flatvert1;
varying vec3 flatvert2;
varying vec3 flatvert3;

void main()
{
    // Add all necessary code to implement the
    // fragment shader portion of Phong shading
    fragmentColor = vec4( 1.0, 1.0, 1.0, 1.0 );
    
    vec3 normvec, light, mirror, view;
    
    vec4 ambColor, diffColor, specColor;
    
    
    // normalize the view
    
    view = normalize(flatvert2);
    
    // normalize the normal vector
    
    normvec = normalize(flatvert1);
    
    
    
    light = normalize(flatvert3 - flatvert2);
    
    // find the mirror reflection
    
    mirror = normalize(reflect(normvec,light));
    
    // set the ambient color
    
    ambColor = ambMaterial1Color * ka * ambLightScene;
     
    // set the diffuse color 
     
    diffColor = diffMaterial1Color * kd  * dot(light,normvec);
    
    // set the specular color
    
    specColor = specMaterial1Color * ks * pow(max(dot(view,mirror),0.0),specularExponent);
    
    // calculate the fragment color
    
    vec4 addvec = (diffColor + specColor);
    
    fragmentColor = ambColor + propLightblueColor * addvec;
}
