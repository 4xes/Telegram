package org.telegram.ui.Animations.Background;

import android.content.Context;

import androidx.annotation.RawRes;

public class Shader {
    public String vertexShader;
    public String fragmentShader;

    public Shader(String vertexShader, String fragmentShader) {
        this.vertexShader = vertexShader;
        this.fragmentShader = fragmentShader;
    }

    public static Shader read(Context context, @RawRes int vertexShader, @RawRes int fragmentShader) {
        return new Shader(
                ShaderReader.readShader(context, vertexShader),
                ShaderReader.readShader(context, fragmentShader));
    }
}

