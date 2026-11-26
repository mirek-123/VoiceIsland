package com.mirecek.siriisland;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.util.Log;

import java.util.HashMap;
import java.util.Map;

/**
 * Plays a short summon sound when the island opens. The four built-in styles are synthesised here
 * (additive sines with bell-like envelopes); "Custom" plays an audio file the user picked. Played as assistance
 * sonification, so it follows the system's sound settings.
 */
final class LaunchSound {
    private static final String TAG = "SiriIsland";
    private static final int RATE = 44100;

    /** Plays the audio file the user picked (Prefs.CUSTOM_SOUND_URI). */
    static final String CUSTOM = "custom";
    static final String[] STYLES = {"glass", "chime", "bloom", "pulse", CUSTOM};
    static final String[] NAMES = {"Glass", "Chime", "Bloom", "Pulse", "Custom"};

    private static final Map<String, short[]> cache = new HashMap<>();
    private static AudioTrack playing;
    private static android.media.MediaPlayer playingFile;

    private LaunchSound() {}

    /** Plays the configured style if the launch sound is on. */
    static void playIfEnabled(Context c) {
        if (Prefs.launchSound(c)) play(c, Prefs.soundStyle(c), Prefs.soundVolume(c));
    }

    static synchronized void play(Context c, String style, int volume) {
        if (CUSTOM.equals(style)) {
            playFile(c, Prefs.customSound(c), volume);
            return;
        }
        short[] pcm = cache.computeIfAbsent(style, LaunchSound::render);
        if (playing != null) {
            playing.release();
            playing = null;
        }
        try {
            AudioTrack track = new AudioTrack.Builder()
                    .setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .build())
                    .setAudioFormat(new AudioFormat.Builder()
                            .setSampleRate(RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build())
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .setBufferSizeInBytes(pcm.length * 2)
                    .build();
            track.write(pcm, 0, pcm.length);
            float v = Math.max(1, Math.min(10, volume)) / 10f;
            track.setVolume(v * v);
            track.setNotificationMarkerPosition(pcm.length);
            track.setPlaybackPositionUpdateListener(new AudioTrack.OnPlaybackPositionUpdateListener() {
                @Override public void onMarkerReached(AudioTrack t) { release(t); }
                @Override public void onPeriodicNotification(AudioTrack t) {}
            });
            track.play();
            playing = track;
        } catch (RuntimeException e) {
            Log.w(TAG, "Launch sound failed", e);
        }
    }

    private static void playFile(Context c, String uri, int volume) {
        if (uri == null) return;
        if (playingFile != null) {
            playingFile.release();
            playingFile = null;
        }
        android.media.MediaPlayer mp = android.media.MediaPlayer.create(c.getApplicationContext(),
                android.net.Uri.parse(uri), null,
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(), 0);
        if (mp == null) {
            Log.w(TAG, "Custom launch sound unreadable: " + uri);
            return;
        }
        float v = Math.max(1, Math.min(10, volume)) / 10f;
        mp.setVolume(v * v, v * v);
        mp.setOnCompletionListener(LaunchSound::releaseFile);
        mp.start();
        playingFile = mp;
    }

    private static synchronized void releaseFile(android.media.MediaPlayer mp) {
        mp.release();
        if (playingFile == mp) playingFile = null;
    }

    private static synchronized void release(AudioTrack t) {
        t.release();
        if (playing == t) playing = null;
    }

    /** A partial: frequency (Hz), start (s), amplitude, decay time constant (s), glide (Hz/s). */
    private static final class Tone {
        final double f, start, amp, decay, glide, attack;

        Tone(double f, double start, double amp, double decay, double glide, double attack) {
            this.f = f; this.start = start; this.amp = amp; this.decay = decay; this.glide = glide; this.attack = attack;
        }
    }

    private static short[] render(String style) {
        Tone[] tones;
        double length;
        switch (style) {
            case "chime": // three rising notes, soft mallet
                tones = new Tone[]{
                        bell(783.99, 0.00, 0.5), bell(1046.50, 0.09, 0.45), bell(1567.98, 0.18, 0.4)};
                length = 0.95;
                break;
            case "bloom": // airy swell of a major-ninth chord that rises and fades
                tones = new Tone[]{
                        new Tone(523.25, 0, 0.22, 0.45, 18, 0.16), new Tone(659.25, 0.02, 0.2, 0.45, 20, 0.16),
                        new Tone(783.99, 0.04, 0.18, 0.42, 24, 0.16), new Tone(1174.66, 0.06, 0.14, 0.38, 30, 0.18)};
                length = 1.1;
                break;
            case "pulse": // two quick glassy pops, second higher
                tones = new Tone[]{
                        new Tone(1318.51, 0.00, 0.5, 0.06, 0, 0.004), new Tone(2637.02, 0.00, 0.15, 0.04, 0, 0.004),
                        new Tone(1760.00, 0.11, 0.5, 0.08, 0, 0.004), new Tone(3520.00, 0.11, 0.15, 0.05, 0, 0.004)};
                length = 0.5;
                break;
            default: // glass: two bright partials that glide up a little, like a struck glass
                tones = new Tone[]{
                        new Tone(1046.50, 0.00, 0.40, 0.30, 60, 0.008), new Tone(1567.98, 0.035, 0.35, 0.28, 80, 0.008),
                        new Tone(3135.96, 0.035, 0.10, 0.12, 120, 0.004), new Tone(2093.00, 0.00, 0.08, 0.15, 90, 0.004)};
                length = 0.8;
        }
        int n = (int) (length * RATE);
        double[] mix = new double[n];
        for (Tone t : tones) {
            int s0 = (int) (t.start * RATE);
            double phase = 0;
            for (int i = s0; i < n; i++) {
                double time = (i - s0) / (double) RATE;
                double env = Math.min(1, time / t.attack) * Math.exp(-time / t.decay);
                if (env < 1e-4 && time > t.attack) break;
                phase += 2 * Math.PI * (t.f + t.glide * time) / RATE;
                mix[i] += t.amp * env * Math.sin(phase);
            }
        }
        // Fade the last 20 ms and normalise to -3 dBFS.
        double peak = 1e-9;
        for (double v : mix) peak = Math.max(peak, Math.abs(v));
        short[] out = new short[n];
        int fade = RATE / 50;
        for (int i = 0; i < n; i++) {
            double g = i > n - fade ? (n - i) / (double) fade : 1;
            out[i] = (short) (mix[i] / peak * 0.707 * g * 32767);
        }
        return out;
    }

    /** A mallet note: fundamental plus an inharmonic overtone. */
    private static Tone bell(double f, double start, double amp) {
        return new Tone(f, start, amp, 0.32, 0, 0.005);
    }
}
