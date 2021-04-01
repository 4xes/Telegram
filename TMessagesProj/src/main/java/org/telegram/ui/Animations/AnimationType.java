package org.telegram.ui.Animations;

public enum AnimationType {
    Background("Background", new Interpolator[]{
            Interpolator.SendMsg, Interpolator.OpenChat, Interpolator.JumpToMsg
    }),
    ShortText("Short Text", new Interpolator[] {
            Interpolator.X,
            Interpolator.Y,
            Interpolator.Shape,
            Interpolator.Scale,
            Interpolator.Color,
            Interpolator.Appears
    }),
    LongText("Long Text", new Interpolator[] {
            Interpolator.X,
            Interpolator.Y,
            Interpolator.Scale,
            Interpolator.Shape,
            Interpolator.Scale,
            Interpolator.Color,
            Interpolator.Appears
    }),
    Link("Link", new Interpolator[] {
            Interpolator.X,
            Interpolator.Y,
            Interpolator.Scale,
            Interpolator.Appears
    }),
    Emoji("Emoji", new Interpolator[] {
            Interpolator.X,
            Interpolator.Y,
            Interpolator.Scale,
            Interpolator.Appears
    }),
    Sticker("Sticker", new Interpolator[] {
            Interpolator.X,
            Interpolator.Y,
            Interpolator.Scale,
            Interpolator.Appears
    }),
    Voice("Voice", new Interpolator[] {
            Interpolator.X,
            Interpolator.Y,
            Interpolator.Scale,
            Interpolator.Appears
    }),
    Video("Video", new Interpolator[] {
            Interpolator.X,
            Interpolator.Y,
            Interpolator.Scale,
            Interpolator.Appears
    }),
    Photo("Photo", new Interpolator[] {
        Interpolator.X,
                Interpolator.Y,
                Interpolator.Scale,
                Interpolator.Appears
    }),
    Album("Album", new Interpolator[] {
        Interpolator.X,
                Interpolator.Y,
                Interpolator.Scale,
                Interpolator.Appears
    });

    final String title;
    final Interpolator[] params;

    AnimationType(String title, Interpolator[] params) {
        this.title = title;
        this.params = params;
    }

}
