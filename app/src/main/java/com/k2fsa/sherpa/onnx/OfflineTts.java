package com.k2fsa.sherpa.onnx;

public class OfflineTts {
    private static final long NULL_PTR = 0L;
    static {
        System.loadLibrary("sherpa-onnx-jni");
    }

    /** Native sherpa handle; {@link #NULL_PTR} means the engine is not allocated. */
    private long ptr = NULL_PTR;

    public OfflineTts(OfflineTtsConfig config) {
        ptr = newFromFile(config);
        if (ptr == NULL_PTR) {
            throw new IllegalArgumentException("Invalid OfflineTtsConfig: failed to create native OfflineTts");
        }
    }

    public int getSampleRate() {
        return getSampleRate(ptr);
    }

    public int getNumSpeakers() {
        return getNumSpeakers(ptr);
    }

    public GeneratedAudio generate(String text) {
        return generate(text, 0, 1.0f);
    }

    public GeneratedAudio generate(String text, int sid) {
        return generate(text, sid, 1.0f);
    }

    public GeneratedAudio generate(String text, int sid, float speed) {
        return generateImpl(ptr, text, sid, speed);
    }


    public void release() {
        if (ptr == NULL_PTR) return;
        delete(ptr);
        ptr = NULL_PTR;
    }

    private native void delete(long ptr);
    private native int getSampleRate(long ptr);
    private native int getNumSpeakers(long ptr);
    private native GeneratedAudio generateImpl(long ptr, String text, int sid, float speed);
    private native long newFromFile(OfflineTtsConfig config);
}
