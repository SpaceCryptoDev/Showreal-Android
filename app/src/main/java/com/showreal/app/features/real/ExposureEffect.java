package com.showreal.app.features.real;

import android.opengl.GLSurfaceView;

import com.sherazkhilji.videffects.interfaces.ShaderInterface;


/**
 * Adjusts the brightness of the video.
 *
 * @author sheraz.khilji
 */
public class ExposureEffect implements ShaderInterface {
    private float brightnessValue;


    public ExposureEffect(float brightnessvalue) {
        this.brightnessValue = brightnessvalue;
    }

    @Override
    public String getShader(GLSurfaceView mGlSurfaceView) {


        String shader = "#extension GL_OES_EGL_image_external : require\n"
                + "precision mediump float;\n"
                + "uniform samplerExternalOES sTexture;\n"
                + "float exposure ;\n" + "varying vec2 vTextureCoord;\n"
                + "void main() {\n" + "  exposure =" + brightnessValue
                + ";\n"
                + "  vec4 color = texture2D(sTexture, vTextureCoord);\n"
                + "  gl_FragColor = pow(2.0, exposure) * color;\n" + "}\n";

        return shader;
    }

}
