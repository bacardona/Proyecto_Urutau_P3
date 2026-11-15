package com.pokemon.battle.fighting;

import java.util.Random;

/**
 * Original 8-bit music made with code: a battle loop and a short victory fanfare.
 * Square wave melody, triangle bass and noise drums, like old consoles.
 * It does not copy any Pokemon song.
 */
final class MusicTheme {

    private MusicTheme() {
    }

    /** Battle loop in A minor, 150 bpm, 16 bars (about 26 seconds). It loops without a gap. */
    static float[] battle(int rate) {
        double beat = 60.0 / 150;
        double eighth = beat / 2;
        int bars = 16;
        float[] out = new float[(int) (bars * 8 * eighth * rate)];
        int[] chordRoots = {57, 53, 55, 52};              // Am, F, G, E
        int[][] melody = {                                // 0 = silence
                {69, 0, 72, 74, 76, 0, 74, 72, 71, 0, 72, 71, 69, 0, 64, 0},
                {65, 0, 69, 72, 74, 0, 72, 69, 67, 0, 71, 74, 76, 77, 76, 74},
                {69, 0, 72, 74, 76, 0, 79, 76, 77, 0, 76, 74, 72, 0, 71, 72},
                {71, 0, 68, 71, 76, 0, 74, 71, 69, 0, 0, 0, 64, 67, 68, 71}};
        Random random = new Random(150);
        for (int bar = 0; bar < bars; bar++) {
            double start = bar * 8 * eighth;
            int root = chordRoots[bar % 4];
            int[] phrase = melody[(bar / 2) % 4];
            int half = (bar % 2) * 8;
            for (int k = 0; k < 8; k++) {
                double at = start + k * eighth;
                int bassNote = k % 2 == 0 ? root - 12 : root;
                add(out, triangle(rate, hz(bassNote), eighth * 0.9, 0.22), at, rate);
                if (k == 0 || k == 4) add(out, drum(rate, random, 0.12, 0.25, 25, 0.15), at, rate);
                if (k == 2 || k == 6) add(out, drum(rate, random, 0.10, 0.18, 35, 1.0), at, rate);
                add(out, drum(rate, random, 0.03, 0.05, 120, 1.0), at + eighth / 2, rate);
                int note = phrase[half + k];
                if (note > 0) {
                    add(out, square(rate, hz(note), eighth * 1.6, 0.25, 0.11), at, rate);
                    add(out, square(rate, hz(note - 12), eighth * 1.2, 0.5, 0.04), at, rate);
                }
            }
        }
        return out;
    }

    /** Short victory fanfare (about 2.5 seconds). */
    static float[] fanfare(int rate) {
        double step = 0.16;
        int[][] notes = {{67, 1}, {72, 1}, {76, 1}, {79, 3}, {77, 1}, {76, 1}, {74, 1}, {76, 2}, {79, 1}, {84, 6}};
        float[] out = new float[(int) (rate * 3.2)];
        double at = 0;
        for (int[] n : notes) {
            double length = step * n[1];
            add(out, square(rate, hz(n[0]), length * 1.1, 0.25, 0.14), at, rate);
            add(out, square(rate, hz(n[0] - 5), length, 0.5, 0.06), at, rate);
            add(out, triangle(rate, hz(n[0] - 24), length, 0.2), at, rate);
            at += length;
        }
        return out;
    }

    private static double hz(int midi) {
        return 440 * Math.pow(2, (midi - 69) / 12.0);
    }

    private static float[] square(int rate, double hz, double seconds, double duty, double volume) {
        float[] out = new float[(int) (seconds * rate)];
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) rate;
            double wave = (t * hz) % 1 < duty ? 1 : -1;
            double envelope = Math.min(1, i / 200.0) * Math.exp(-t * 3);
            out[i] = (float) (wave * volume * envelope);
        }
        return out;
    }

    private static float[] triangle(int rate, double hz, double seconds, double volume) {
        float[] out = new float[(int) (seconds * rate)];
        for (int i = 0; i < out.length; i++) {
            double phase = (i / (double) rate * hz) % 1;
            double release = Math.min(1, (out.length - i) / 300.0);
            out[i] = (float) ((4 * Math.abs(phase - 0.5) - 1) * volume * release);
        }
        return out;
    }

    private static float[] drum(int rate, Random random, double seconds, double volume,
                                double decay, double brightness) {
        float[] out = new float[(int) (seconds * rate)];
        double last = 0;
        for (int i = 0; i < out.length; i++) {
            last += (random.nextDouble() * 2 - 1 - last) * brightness;
            out[i] = (float) (last * volume * Math.exp(-i / (double) rate * decay) * (brightness < 1 ? 2 : 1));
        }
        return out;
    }

    /** Mixes a sound into the track; whatever passes the end wraps to the start (seamless loop). */
    private static void add(float[] track, float[] sound, double atSeconds, int rate) {
        int start = (int) (atSeconds * rate);
        for (int i = 0; i < sound.length; i++) {
            int index = (start + i) % track.length;
            track[index] += sound[i];
        }
    }
}
