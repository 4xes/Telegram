package org.telegram.ui.Animations;

class InterpolatorData {
    final String key;
    float progressionStart;
    float progressionEnd;
    float timeStart;
    float timeEnd;

    public InterpolatorData(String key, float progressionStart, float progressionEnd, float timeStart, float timeEnd) {
        this.key = key;
        this.progressionStart = progressionStart;
        this.progressionEnd = progressionEnd;
        this.timeStart = timeStart;
        this.timeEnd = timeEnd;
    }
}