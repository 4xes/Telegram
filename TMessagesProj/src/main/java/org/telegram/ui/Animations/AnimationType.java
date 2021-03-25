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

    enum Interpolator {
        X("X Position", false),
        Y("Y Position", false),
        Shape("Bubble shape", false),
        Scale(" scale", false),
        Color("Color change", false),
        Appears("Time appears", false),
        SendMsg("Send Message",true),
        OpenChat("Open Chat",true),
        JumpToMsg("Jump to Message",true);

        final String title;
        final boolean hasDuration;

        Interpolator(String title, boolean hasDuration) {
            this.title = title;
            this.hasDuration = hasDuration;
        }
    }
}
