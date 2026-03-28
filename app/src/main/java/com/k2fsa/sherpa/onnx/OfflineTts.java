package com.k2fsa.sherpa.onnx;

public class OfflineTts {
    static {
        System.loadLibrary("sherpa-onnx-jni");
    }

    private long ptr = 0;

    public OfflineTts(OfflineTtsConfig config) {
        ptr = newFromFile(config);
        if (ptr == 0) {
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

    @Override
    protected void finalize() throws Throwable {
        release();
    }

    public void release() {
        if (ptr == 0) return;
        delete(ptr);
        ptr = 0;
    }

    private native void delete(long ptr);
    private native int getSampleRate(long ptr);
    private native int getNumSpeakers(long ptr);
    private native GeneratedAudio generateImpl(long ptr, String text, int sid, float speed);
    private native long newFromFile(OfflineTtsConfig config);
}
