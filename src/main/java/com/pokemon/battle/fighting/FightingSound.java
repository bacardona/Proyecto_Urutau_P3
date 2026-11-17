package com.pokemon.battle.fighting;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.SourceDataLine;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * 8-bit sound effects made with code (square waves, noise and pitch sweeps).
 * There are no audio files, so there is no copyright problem.
 *
 * A small mixer thread sends the sounds to the speakers, so several sounds can
 * play at the same time. If the computer has no audio, it stays silent and the
 * game keeps working.
 */
public final class FightingSound {

    public enum Sfx {
        HIT_LIGHT, HIT_HEAVY, WHOOSH, JUMP, LAND, BLOCK,
        FIRE_SHOT, FIRE_HIT, WATER_SHOT, WATER_HIT, ELECTRIC_SHOT, ELECTRIC_HIT,
        POWDER, VINE, BITE, GROWL, BUFF, KO, ULTI_CHARGE, ULTI_RELEASE,
        VS, COUNT, GO, CUT_IN,
        // Jefe secreto: trueno de su llegada y su risa cuando gana
        THUNDER, LAUGH
    }

    private static final int RATE = 22050;
    private static final FightingSound INSTANCE = new FightingSound();

    private final Map<Sfx, float[]> samples = new EnumMap<>(Sfx.class);
    private final List<Voice> voices = new ArrayList<>();
    private volatile boolean muted;
    private volatile boolean started;
    private volatile boolean available = true;
    // Vigilante del mezclador: si el hilo de audio se muere o se queda trabado
    // (por ejemplo, Windows cambia de parlantes), se abre uno nuevo.
    private volatile long lastWriteNanos;
    private volatile int generation;
    private volatile boolean everOpened;
    private static final long STALL_NANOS = 1_500_000_000L;

    private static final class Voice {
        private final float[] data;
        private final float volume;
        private int position;

        private Voice(float[] data, float volume) {
            this.data = data;
            this.volume = volume;
        }
    }

    // Musica de fondo (se repite sola) y su volumen
    private static final float MUSIC_VOLUME = 0.32f;
    private volatile float[] music;
    private volatile boolean musicPlaying;
    private int musicPosition;
    private float[] battleTheme;
    private float[] fanfare;

    private FightingSound() {
    }

    /** Starts the 8-bit battle loop (generated once, the first time). */
    public void startBattleMusic() {
        if (!available) return;
        synchronized (this) {
            if (battleTheme == null) battleTheme = MusicTheme.battle(RATE);
        }
        if (music != battleTheme) musicPosition = 0;
        music = battleTheme;
        musicPlaying = true;
        ensureStarted();
    }

    public void stopMusic() {
        musicPlaying = false;
    }

    public boolean isMusicPlaying() {
        return musicPlaying;
    }

    /** Short victory fanfare. */
    public void playFanfare() {
        if (muted || !available) return;
        synchronized (this) {
            if (fanfare == null) fanfare = MusicTheme.fanfare(RATE);
        }
        ensureStarted();
        synchronized (voices) {
            voices.add(new Voice(fanfare, 0.9f));
        }
    }

    public static FightingSound get() {
        return INSTANCE;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public boolean isMuted() {
        return muted;
    }

    public void play(Sfx sfx) {
        play(sfx, 1f);
    }

    public void play(Sfx sfx, float volume) {
        if (muted || !available) return;
        ensureStarted();
        float[] data = sample(sfx);
        synchronized (voices) {
            if (voices.size() < 12) voices.add(new Voice(data, volume));
        }
    }

    private synchronized float[] sample(Sfx sfx) {
        return samples.computeIfAbsent(sfx, FightingSound::build);
    }

    // ------------------------------------------------------------------ mezclador

    /** Called every frame: restarts the audio thread if it died or got stuck. */
    public void keepAlive() {
        if (available && started && System.nanoTime() - lastWriteNanos > STALL_NANOS) {
            synchronized (this) {
                started = false;
            }
            ensureStarted();
        }
    }

    private synchronized void ensureStarted() {
        if (started && System.nanoTime() - lastWriteNanos <= STALL_NANOS) return;
        started = true;
        lastWriteNanos = System.nanoTime();
        int myGeneration = ++generation;
        Thread thread = new Thread(() -> mixLoop(myGeneration), "fighting-sound-" + myGeneration);
        thread.setDaemon(true);
        thread.start();
    }

    private void mixLoop(int myGeneration) {
        SourceDataLine line = null;
        try {
            AudioFormat format = new AudioFormat(RATE, 16, 1, true, false);
            line = AudioSystem.getSourceDataLine(format);
            line.open(format, 2048 * 4);
            line.start();
            everOpened = true;
            int chunk = 256;
            byte[] out = new byte[chunk * 2];
            float[] mix = new float[chunk];
            while (myGeneration == generation) {
                java.util.Arrays.fill(mix, 0f);
                synchronized (voices) {
                    for (int v = voices.size() - 1; v >= 0; v--) {
                        Voice voice = voices.get(v);
                        int n = Math.min(chunk, voice.data.length - voice.position);
                        for (int i = 0; i < n; i++) {
                            mix[i] += voice.data[voice.position + i] * voice.volume;
                        }
                        voice.position += n;
                        if (voice.position >= voice.data.length) voices.remove(v);
                    }
                }
                float[] track = music;
                if (musicPlaying && !muted && track != null && track.length > 0) {
                    for (int i = 0; i < chunk; i++) {
                        mix[i] += track[musicPosition] * MUSIC_VOLUME;
                        musicPosition = (musicPosition + 1) % track.length;
                    }
                }
                for (int i = 0; i < chunk; i++) {
                    // Limitador suave para que varios sonidos juntos no se rompan
                    float raw = Float.isFinite(mix[i]) ? mix[i] : 0f;
                    float value = (float) Math.tanh(raw * 0.9);
                    int pcm = (int) (value * 30000);
                    out[i * 2] = (byte) pcm;
                    out[i * 2 + 1] = (byte) (pcm >> 8);
                }
                line.write(out, 0, out.length);
                lastWriteNanos = System.nanoTime();
            }
        } catch (Throwable e) {
            if (!everOpened) {
                // Sin tarjeta de sonido (o sin permisos): el juego sigue en silencio
                available = false;
            } else {
                System.err.println("[FightingSound] audio thread stopped, restarting: " + e);
                synchronized (this) {
                    if (myGeneration == generation) started = false;
                }
            }
        } finally {
            if (line != null) {
                try {
                    line.close();
                } catch (Throwable ignored) {
                    // la linea ya estaba rota
                }
            }
        }
    }

    // ------------------------------------------------------------------ sintesis

    private static float[] build(Sfx sfx) {
        Random random = new Random(sfx.ordinal() * 7919L);
        return switch (sfx) {
            case HIT_LIGHT -> mixAll(thump(0.14, 190, 55, 0.9), noise(random, 0.08, 0.55, 0.35, 0));
            case HIT_HEAVY -> crush(mixAll(thump(0.30, 150, 38, 1.0),
                    noise(random, 0.20, 0.8, 0.25, 0)), 6);
            case WHOOSH -> sweepNoise(random, 0.20, 0.05, 0.6, 0.45);
            case JUMP -> square(0.14, 260, 720, 0.28, 0.5);
            case LAND -> thump(0.09, 120, 50, 0.6);
            case BLOCK -> mixAll(tone(0.22, 1250, 0.35), tone(0.22, 1810, 0.25),
                    noise(random, 0.03, 0.4, 0.8, 0));
            case FIRE_SHOT -> mixAll(sweepNoise(random, 0.38, 0.15, 0.5, 0.6),
                    crackle(random, 0.38, 0.35));
            case FIRE_HIT -> crush(mixAll(explosion(random, 0.55, 0.95),
                    thump(0.30, 110, 35, 0.8), crackle(random, 0.55, 0.3)), 5);
            case WATER_SHOT -> mixAll(bubbly(random, 0.32, 0.5), sweepNoise(random, 0.32, 0.6, 0.9, 0.25));
            case WATER_HIT -> mixAll(noise(random, 0.35, 0.7, 0.55, 0.12), droplets(random, 0.4, 0.4));
            case ELECTRIC_SHOT -> mixAll(buzz(random, 0.30, 70, 0.45), noise(random, 0.30, 0.25, 1.0, 0.1));
            case ELECTRIC_HIT -> crush(mixAll(zap(random, 0.42, 0.55), noise(random, 0.2, 0.45, 1.0, 0)), 4);
            case POWDER -> shimmer(0.45, 0.30);
            case VINE -> mixAll(sweepNoise(random, 0.12, 0.3, 1.0, 0.5), noise(random, 0.03, 0.7, 1.0, 0.09));
            case BITE -> mixAll(noise(random, 0.05, 0.8, 0.6, 0), noise(random, 0.05, 0.8, 0.6, 0.09),
                    thump(0.12, 160, 70, 0.5));
            case GROWL -> mixAll(saw(0.65, 105, 85, 0.45, 9), noise(random, 0.65, 0.25, 0.2, 0));
            case BUFF -> arpeggio(new double[]{523, 659, 784, 1047, 1319}, 0.07, 0.3);
            case KO -> crush(mixAll(square(0.85, 620, 70, 0.45, 0.5), explosion(random, 0.7, 0.6)), 5);
            case ULTI_CHARGE -> mixAll(saw(0.9, 90, 360, 0.30, 14), noise(random, 0.9, 0.15, 0.6, 0));
            case ULTI_RELEASE -> crush(mixAll(explosion(random, 1.1, 1.0), thump(0.6, 90, 25, 1.0),
                    zap(random, 0.6, 0.3)), 4);
            case VS -> crush(mixAll(explosion(random, 0.7, 0.8), thump(0.45, 140, 40, 1.0)), 5);
            case COUNT -> square(0.18, 660, 660, 0.35, 0.5);
            case GO -> arpeggio(new double[]{523, 784, 1047, 1568}, 0.06, 0.4);
            case CUT_IN -> mixAll(sweepNoise(random, 0.35, 0.1, 0.9, 0.5), square(0.25, 880, 1320, 0.25, 0.5));
            case THUNDER -> crush(mixAll(explosion(random, 1.0, 1.0), zap(random, 0.45, 0.5),
                    thump(0.5, 70, 30, 0.9)), 4);
            case LAUGH -> laugh();
        };
    }

    private static float[] buffer(double seconds) {
        return new float[(int) (seconds * RATE)];
    }

    private static double envelope(int i, int length, double attackSeconds) {
        int attack = Math.max(1, (int) (attackSeconds * RATE));
        double a = Math.min(1, i / (double) attack);
        double t = i / (double) length;
        return a * Math.pow(1 - t, 2);
    }

    /** Golpe grave: seno que baja de tono rapido. */
    private static float[] thump(double seconds, double fromHz, double toHz, double volume) {
        float[] out = buffer(seconds);
        double phase = 0;
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) out.length;
            double hz = fromHz * Math.pow(toHz / fromHz, t);
            phase += 2 * Math.PI * hz / RATE;
            out[i] = (float) (Math.sin(phase) * volume * Math.pow(1 - t, 3));
        }
        return out;
    }

    /** Ruido con filtro paso bajo (brillo 0..1) y un retraso opcional. */
    private static float[] noise(Random random, double seconds, double volume,
                                 double brightness, double delaySeconds) {
        float[] out = buffer(seconds + delaySeconds);
        int start = (int) (delaySeconds * RATE);
        int length = out.length - start;
        double last = 0;
        for (int i = 0; i < length; i++) {
            double white = random.nextDouble() * 2 - 1;
            last += (white - last) * brightness;
            out[start + i] = (float) (last * volume * envelope(i, length, 0.002));
        }
        return out;
    }

    /** Ruido cuyo brillo cambia con el tiempo: suena como un "fiuu". */
    private static float[] sweepNoise(Random random, double seconds, double fromBright,
                                      double toBright, double volume) {
        float[] out = buffer(seconds);
        double last = 0;
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) out.length;
            double brightness = fromBright + (toBright - fromBright) * t;
            last += (random.nextDouble() * 2 - 1 - last) * brightness;
            double env = Math.sin(Math.PI * Math.min(1, t * 1.4)) * (1 - t);
            out[i] = (float) (last * volume * env * 1.6);
        }
        return out;
    }

    private static float[] crackle(Random random, double seconds, double volume) {
        float[] out = buffer(seconds);
        for (int i = 0; i < out.length; i++) {
            if (random.nextDouble() < 0.006) {
                int len = 30 + random.nextInt(60);
                double amp = volume * (0.5 + random.nextDouble() * 0.5) * (1 - i / (double) out.length);
                for (int k = 0; k < len && i + k < out.length; k++) {
                    out[i + k] += (float) ((random.nextDouble() * 2 - 1) * amp * (1 - k / (double) len));
                }
            }
        }
        return out;
    }

    private static float[] explosion(Random random, double seconds, double volume) {
        float[] out = buffer(seconds);
        double last = 0;
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) out.length;
            double brightness = 0.45 * Math.pow(1 - t, 2) + 0.03;
            last += (random.nextDouble() * 2 - 1 - last) * brightness;
            out[i] = (float) (last * volume * 2.2 * envelope(i, out.length, 0.004));
        }
        return out;
    }

    private static float[] square(double seconds, double fromHz, double toHz,
                                  double volume, double duty) {
        float[] out = buffer(seconds);
        double phase = 0;
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) out.length;
            double hz = fromHz * Math.pow(toHz / fromHz, t);
            phase = (phase + hz / RATE) % 1.0;
            out[i] = (float) ((phase < duty ? 1 : -1) * volume * envelope(i, out.length, 0.003));
        }
        return out;
    }

    private static float[] saw(double seconds, double fromHz, double toHz, double volume,
                               double vibratoHz) {
        float[] out = buffer(seconds);
        double phase = 0;
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) out.length;
            double hz = (fromHz + (toHz - fromHz) * t)
                    * (1 + 0.06 * Math.sin(2 * Math.PI * vibratoHz * i / RATE));
            phase = (phase + hz / RATE) % 1.0;
            double env = Math.min(1, t * 8) * (1 - t);
            out[i] = (float) ((phase * 2 - 1) * volume * env);
        }
        return out;
    }

    private static float[] tone(double seconds, double hz, double volume) {
        float[] out = buffer(seconds);
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) out.length;
            out[i] = (float) (Math.sin(2 * Math.PI * hz * i / RATE) * volume * Math.pow(1 - t, 3));
        }
        return out;
    }

    private static float[] buzz(Random random, double seconds, double baseHz, double volume) {
        float[] out = buffer(seconds);
        double phase = 0;
        double hz = baseHz;
        for (int i = 0; i < out.length; i++) {
            if (i % 300 == 0) hz = baseHz * (1 + random.nextDouble() * 6);
            phase = (phase + hz / RATE) % 1.0;
            out[i] = (float) ((phase < 0.3 ? 1 : -1) * volume * envelope(i, out.length, 0.004));
        }
        return out;
    }

    private static float[] zap(Random random, double seconds, double volume) {
        float[] out = buffer(seconds);
        double phase = 0;
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) out.length;
            double hz = 1400 * Math.pow(0.12, t) * (0.7 + random.nextDouble() * 0.6);
            phase = (phase + hz / RATE) % 1.0;
            out[i] = (float) ((phase < 0.5 ? 1 : -1) * volume * envelope(i, out.length, 0.002));
        }
        return out;
    }

    private static float[] bubbly(Random random, double seconds, double volume) {
        float[] out = buffer(seconds);
        double phase = 0;
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) out.length;
            double wobble = Math.sin(2 * Math.PI * 23 * i / RATE);
            double hz = 380 + 260 * wobble + 200 * t;
            phase += 2 * Math.PI * hz / RATE;
            out[i] = (float) (Math.sin(phase) * volume * (1 - t) * Math.min(1, t * 20));
        }
        return out;
    }

    private static float[] droplets(Random random, double seconds, double volume) {
        float[] out = buffer(seconds);
        for (int d = 0; d < 7; d++) {
            int start = (int) (random.nextDouble() * 0.8 * out.length);
            double hz = 900 + random.nextDouble() * 1400;
            int len = (int) (0.05 * RATE);
            for (int k = 0; k < len && start + k < out.length; k++) {
                double t = k / (double) len;
                out[start + k] += (float) (Math.sin(2 * Math.PI * hz * (1 + t) * k / RATE)
                        * volume * (1 - t));
            }
        }
        return out;
    }

    private static float[] shimmer(double seconds, double volume) {
        float[] out = buffer(seconds);
        double[] notes = {1568, 2093, 1760, 2349};
        for (int i = 0; i < out.length; i++) {
            double t = i / (double) out.length;
            double hz = notes[(int) (t * 12) % notes.length];
            out[i] = (float) (Math.sin(2 * Math.PI * hz * i / RATE)
                    * volume * (1 - t) * (0.6 + 0.4 * Math.sin(2 * Math.PI * 9 * i / RATE)));
        }
        return out;
    }

    private static float[] arpeggio(double[] notes, double noteSeconds, double volume) {
        float[] out = buffer(notes.length * noteSeconds + 0.15);
        int noteLength = (int) (noteSeconds * RATE);
        for (int n = 0; n < notes.length; n++) {
            double phase = 0;
            int start = n * noteLength;
            int len = noteLength + (int) (0.15 * RATE);
            for (int k = 0; k < len && start + k < out.length; k++) {
                phase = (phase + notes[n] / RATE) % 1.0;
                double t = k / (double) len;
                out[start + k] += (float) ((phase < 0.5 ? 1 : -1) * volume * Math.pow(1 - t, 2));
            }
        }
        return out;
    }

    /** "¡Ja, ja, ja, ja!" grave: cuatro silabas que bajan de tono, con vibrato. */
    private static float[] laugh() {
        double syllable = 0.17;
        double gap = 0.07;
        int count = 5;
        float[] out = buffer(count * (syllable + gap) + 0.1);
        for (int n = 0; n < count; n++) {
            double from = 230 - n * 16;
            float[] ha = mixAll(saw(syllable, from, from * 0.82, 0.42, 22),
                    square(syllable, from * 2, from * 1.6, 0.08, 0.3));
            int start = (int) (n * (syllable + gap) * RATE);
            for (int i = 0; i < ha.length && start + i < out.length; i++) out[start + i] += ha[i];
        }
        return crush(out, 3);
    }

    private static float[] mixAll(float[]... parts) {
        int length = 0;
        for (float[] part : parts) length = Math.max(length, part.length);
        float[] out = new float[length];
        for (float[] part : parts) {
            for (int i = 0; i < part.length; i++) out[i] += part[i];
        }
        return out;
    }

    /** Baja la resolucion del sonido para que suene mas "8-bit". */
    private static float[] crush(float[] in, int hold) {
        float[] out = new float[in.length];
        for (int i = 0; i < in.length; i++) {
            float held = in[i - i % hold];
            out[i] = Math.round(held * 24) / 24f;
        }
        return out;
    }
}
