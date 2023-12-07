package com.blackfox.surface.renderer.particle;

import android.opengl.GLES20;
import android.opengl.GLES31;

import org.telegram.messenger.R;
import com.blackfox.surface.renderer.EglUtils;
import com.blackfox.surface.renderer.TextureAtomicIndex;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.Utilities;

/**
 * @noinspection NonAsciiCharacters
 */
public class DustRenderer {

    private int width;
    private int height;
    private int program;
    private int textureHandle;
    private int textureId = EglUtils.NO_TEXTURE;
    private int textureIndex = GLES31.GL_TEXTURE0;
    private boolean reset;

    private final ParticleHandle particle = new ParticleHandle();
    private final DustRequest dustRequest;
    private int[] particlesData;
    private int currentBuffer = 0;

    private ImageRenderer rect = new ImageRenderer();

    public void setSize(int width, int height) {
        rect.setSize(width, height);
        if (this.width != width && this.height != height) {
            this.width = width;
            this.height = height;
        }
    }

    public DustRenderer(DustRequest dustRequest) {
        this.dustRequest = dustRequest;
        rect.init();
        init();
    }

    public void init() {
        int vertexShader = EglUtils.createVertexShader(AndroidUtilities.readRes(R.raw.dust_vertex) + "\n// " + Math.random());
        int fragmentShader = EglUtils.createFragmentShader(AndroidUtilities.readRes(R.raw.dust_fragment) + "\n// " + Math.random());
        program = EglUtils.createProgram(vertexShader, fragmentShader, ParticleHandle.FEEDBACK_VARYING);
        if (program == EglUtils.ERROR_PROGRAM) {
            return;
        }
        particle.getHandlers(program);
        textureHandle = GLES31.glGetUniformLocation(program, "oTexture");

        textureId = EglUtils.loadTexture(dustRequest.getBitmap());
        if (textureId != EglUtils.NO_TEXTURE) {
            textureIndex = TextureAtomicIndex.getIndex();
        }

        GLES31.glViewport(0, 0, width, height);
        GLES31.glEnable(GLES31.GL_BLEND);
        GLES31.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES31.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);

        GLES31.glUseProgram(program);
        
        particle.setRadius(dustRequest.getRadius());
        particle.setDustCount(dustRequest.getCountHorizontal(), dustRequest.getCountVertical());
        particle.setDustOffset((float) dustRequest.getOffsetX() / width, (float) dustRequest.getOffsetY() / height);
        particle.setDustSize((float) dustRequest.getWidth() / width, (float) dustRequest.getHeight() / height);

        reset = true;
        particle.setReset(true);
        particle.setRadius(AndroidUtilities.dpf2(1.2f));
        particle.setSeeds(Utilities.fastRandom.nextInt(256) / 256f);
        createPoints();
    }

    boolean drawFirst = false;

    public void drawParticles(float time, float deltaTime) {
        if (textureHandle != 0 && textureId != EglUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + textureIndex);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
            rect.setPosition(dustRequest);
            rect.draw(dustRequest, textureIndex, time, deltaTime);
        }
        GLES31.glUseProgram(program);
        if (textureHandle != 0 && textureId != EglUtils.NO_TEXTURE) {
            GLES20.glUniform1i(textureHandle, textureIndex);
        }
        GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, particlesData[currentBuffer]);
        GLES31.glVertexAttribPointer(0, 2, GLES31.GL_FLOAT, false, 20, 0); // Offset (vec2)
        GLES31.glEnableVertexAttribArray(0);
        GLES31.glVertexAttribPointer(1, 2, GLES31.GL_FLOAT, false, 20, 8); // Velocity (vec2)
        GLES31.glEnableVertexAttribArray(1);
        GLES31.glVertexAttribPointer(2, 1, GLES31.GL_FLOAT, false, 20, 16); // Time (float)
        GLES31.glEnableVertexAttribArray(3);
        GLES31.glBindBufferBase(GLES31.GL_TRANSFORM_FEEDBACK_BUFFER, 0, particlesData[1 - currentBuffer]);
        GLES31.glVertexAttribPointer(0, 2, GLES31.GL_FLOAT, false, 20, 0); // Offset (vec2)
        GLES31.glEnableVertexAttribArray(0);
        GLES31.glVertexAttribPointer(1, 2, GLES31.GL_FLOAT, false, 20, 8); // Velocity (vec2)
        GLES31.glEnableVertexAttribArray(1);
        GLES31.glVertexAttribPointer(2, 1, GLES31.GL_FLOAT, false, 20, 16); // Time (float)
        GLES31.glEnableVertexAttribArray(2);
        particle.setTime(time);
        particle.setDeltaTime(deltaTime);
        GLES31.glBeginTransformFeedback(GLES31.GL_POINTS);
        GLES31.glDrawArrays(GLES31.GL_POINTS, 0, dustRequest.getCount());
        GLES31.glEndTransformFeedback();

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0);

        if (reset) {
            reset = false;
            particle.setReset(false);
        }
        currentBuffer = 1 - currentBuffer;
        drawFirst = true;
    }

    public void cleanup() {
        if (program != 0) {
            try { GLES31.glDeleteProgram(program); } catch (Exception e) { EglUtils.log(e); };
            program = 0;
        }
        rect.cleanup();
        if (textureId != 0) {
            GLES20.glDeleteTextures(/* n= */ 1, new int[] {textureId}, /* offset= */ 0);
        }
    }

    private void createPoints() {
        if (particlesData != null) {
            GLES31.glDeleteBuffers(2, particlesData, 0);
        }

        particlesData = new int[2];
        GLES31.glGenBuffers(2, particlesData, 0);

        for (int i = 0; i < 2; ++i) {
            GLES31.glBindBuffer(GLES31.GL_ARRAY_BUFFER, particlesData[i]);
            GLES31.glBufferData(GLES31.GL_ARRAY_BUFFER, dustRequest.getCount() * 6 * 4, null, GLES31.GL_DYNAMIC_DRAW);
        }

        EglUtils.checkGlErrors("genParticlesData");
    }
}
