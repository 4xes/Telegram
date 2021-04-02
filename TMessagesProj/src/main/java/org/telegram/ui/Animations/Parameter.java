package org.telegram.ui.Animations;

public enum Parameter {
    X("X Position", false),
    Y("Y Position", false),
    Bubble("Bubble shape", false),
    Scale("scale", false),
    Color("Color change", false),
    TimeAppears("Time appears", false),
    SendMsg("Send Message",true),
    OpenChat("Open Chat",true),
    JumpToMsg("Jump to Message",true);

    final String title;
    final boolean hasDuration;

    Parameter(String title, boolean hasDuration) {
        this.title = title;
        this.hasDuration = hasDuration;
    }
}
