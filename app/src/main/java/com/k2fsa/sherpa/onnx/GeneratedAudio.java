package com.k2fsa.sherpa.onnx;

/** Minimal sherpa-onnx Java wrapper for generated PCM samples returned from JNI. */
public class GeneratedAudio {
    private final float[] samples;
    private final int sampleRate;

    public GeneratedAudio(float[] samples, int sampleRate) {
        this.samples = samples;
        this.sampleRate = sampleRate;
    }

    public int getSampleRate() {
        return sampleRate;
    }

    public float[] getSamples() {
        return samples;
    }

    /** Persists the generated audio to a WAV file through the sherpa JNI bridge. */
    public boolean save(String filename) {
        return saveImpl(filename, samples, sampleRate);
    }

    private native boolean saveImpl(String filename, float[] samples, int sampleRate);
}
