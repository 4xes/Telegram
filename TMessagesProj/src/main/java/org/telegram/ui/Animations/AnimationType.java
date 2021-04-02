package org.telegram.ui.Animations;

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

    AnimationType(String title, Parameter[] params) {
        this.title = title;
        this.params = params;
    }

}
