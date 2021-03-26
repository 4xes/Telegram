package org.telegram.ui.Animations;

public enum Interpolator {
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
