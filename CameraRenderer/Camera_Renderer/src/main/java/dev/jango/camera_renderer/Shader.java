package dev.jango.camera_renderer;

import android.opengl.GLES20;

final class Shader
{
    private Shader(){ }


    public static int load(String srcVertex, String srcFragment)
    {
        int vertex   = loadShader(GLES20.GL_VERTEX_SHADER,   srcVertex);
        int fragment = loadShader(GLES20.GL_FRAGMENT_SHADER, srcFragment);

        int shaderHandle = GLES20.glCreateProgram();

        GLES20.glAttachShader(shaderHandle, vertex);
        GLES20.glAttachShader(shaderHandle, fragment);

        GLES20.glLinkProgram(shaderHandle);

        GLES20.glDetachShader(shaderHandle, vertex);
        GLES20.glDetachShader(shaderHandle, fragment);

        GLES20.glDeleteShader(vertex);
        GLES20.glDeleteShader(fragment);

        return shaderHandle;
    }


    private static int loadShader(int type, String source)
    {
        int id = GLES20.glCreateShader(type);

        GLES20.glShaderSource(id, source);
        GLES20.glCompileShader(id);

        return id;
    }


}