package org.telegram.ui.Animations;

import androidx.annotation.Nullable;

public enum AnimationType {
    Background("Background", new Parameter[]{
            Parameter.SendMsg, Parameter.OpenChat, Parameter.JumpToMsg
    }),

    ShortText("Short Text", new Parameter[] {
            Parameter.X,
            Parameter.Y,
            Parameter.Bubble,
            Parameter.Scale,
            Parameter.Color,
            Parameter.TimeAppears
    }),
    LongText("Long Text", new Parameter[] {
            Parameter.X,
            Parameter.Y,
            Parameter.Bubble,
            Parameter.Scale,
            Parameter.Color,
            Parameter.TimeAppears
    }),
    Link("Link", new Parameter[] {
            Parameter.X,
            Parameter.Y,
            Parameter.Scale,
            Parameter.TimeAppears
    }),
    Emoji("Emoji", new Parameter[] {
            Parameter.X,
            Parameter.Y,
            Parameter.Scale,
            Parameter.TimeAppears
    }),
    Sticker("Sticker", new Parameter[] {
            Parameter.X,
            Parameter.Y,
            Parameter.Scale,
            Parameter.TimeAppears
    }),
    Voice("Voice", new Parameter[] {
            Parameter.X,
            Parameter.Y,
            Parameter.Scale,
            Parameter.TimeAppears
    }),
    Video("Video", new Parameter[] {
            Parameter.X,
            Parameter.Y,
            Parameter.Scale,
            Parameter.TimeAppears
    }),
    Photo("Photo", new Parameter[] {
        Parameter.X,
                Parameter.Y,
                Parameter.Scale,
                Parameter.TimeAppears
    }),
    Album("Album", new Parameter[] {
        Parameter.X,
                Parameter.Y,
                Parameter.Scale,
                Parameter.TimeAppears
    });

    final String title;
    final Parameter[] params;

    public long defaultDuration(@Nullable Parameter parameter) {
        if (parameter == null) {
            return 500L;
        }
        if (parameter == Parameter.SendMsg) {
            return 1000L;
        }
        if (parameter == Parameter.OpenChat) {
            return 2000L;
        }
        if (parameter == Parameter.JumpToMsg) {
            return 1000L;
        }
        return 500L;
    }

    AnimationType(String title, Parameter[] params) {
        this.title = title;
        this.params = params;
    }

}
