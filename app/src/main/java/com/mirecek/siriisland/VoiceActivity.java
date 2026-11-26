package com.mirecek.siriisland;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.AudioRecord;
import android.media.AudioRecordingConfiguration;
import android.media.MediaRecorder;
import android.media.audiofx.Visualizer;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;

import java.util.List;

/**
 * Drives the ribbon from sound while Xiao Ai is on screen: overall level plus four frequency bands
 * (bass, low-mid, high-mid, treble) that each ribbon follows.
 *
 * Sources:
 * <ul>
 *   <li>Microphone: the user's voice, metered with AudioRecord (VOICE_RECOGNITION). Android lets an
 *       accessibility service record alongside the assistant, the same way 超级岛 PRO does it.</li>
 *   <li>Xiao Ai's voice: the output mix through a Visualizer on session 0.</li>
 *   <li>Synthetic: speech-like motion only, for ROMs that block concurrent capture.</li>
 * </ul>
 * It also reports when Xiao Ai stops listening but has not started talking yet ("thinking").
 */
final class VoiceActivity implements IslandView.LevelSource {
    private static final String TAG = "SiriIsland";
    private static final int RATE = 16000;
    private static final int N = 512;
    /** Band edges in Hz. */
    private static final float[] EDGES = {80, 300, 1000, 3000, 8000};

    static final String SOURCE_MIC = "mic";
    static final String SOURCE_OUTPUT = "output";
    static final String SOURCE_SYNTHETIC = "synthetic";

    private final Context context;
    private final AudioManager audio;
    private final Handler main = new Handler(Looper.getMainLooper());

    private volatile boolean othersRecording;
    private volatile boolean speaking;
    private boolean heardListening;
    private long quietSince;
    private boolean started;

    private Visualizer visualizer;
    private final byte[] fft = new byte[N];
    private AudioRecord recorder;
    private Thread micThread;
    private volatile boolean micRunning;
    private final float[] micBands = new float[4];
    private volatile float micLevel;

    private final float[] bands = new float[4];
    private float level;
    private float sensitivity = 1f;

    private final AudioManager.AudioPlaybackCallback playback = new AudioManager.AudioPlaybackCallback() {
        @Override public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> configs) {
            speaking = anySpeech(configs);
        }
    };

    private final AudioManager.AudioRecordingCallback recordingCallback = new AudioManager.AudioRecordingCallback() {
        @Override public void onRecordingConfigChanged(List<AudioRecordingConfiguration> configs) {
            othersRecording = anyOtherRecording(configs);
        }
    };

    VoiceActivity(Context context) {
        this.context = context;
        this.audio = context.getSystemService(AudioManager.class);
    }

    void start() {
        if (started) return;
        started = true;
        heardListening = false;
        quietSince = 0;
        sensitivity = Prefs.sensitivity(context);
        audio.registerAudioPlaybackCallback(playback, main);
        audio.registerAudioRecordingCallback(recordingCallback, main);
        speaking = anySpeech(audio.getActivePlaybackConfigurations());
        othersRecording = anyOtherRecording(audio.getActiveRecordingConfigurations());

        String source = Prefs.audioSource(context);
        boolean allowed = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
        if (!allowed || SOURCE_SYNTHETIC.equals(source)) return;
        startVisualizer();
        // Give Xiao Ai a moment to open the microphone first, so it never finds it busy.
        if (SOURCE_MIC.equals(source)) main.postDelayed(this::startMic, 350);
    }

    void stop() {
        if (!started) return;
        started = false;
        main.removeCallbacksAndMessages(null);
        audio.unregisterAudioPlaybackCallback(playback);
        audio.unregisterAudioRecordingCallback(recordingCallback);
        stopMic();
        if (visualizer != null) {
            visualizer.release();
            visualizer = null;
        }
    }

    private void startVisualizer() {
        try {
            visualizer = new Visualizer(0);
            visualizer.setEnabled(false);
            visualizer.setCaptureSize(N);
            visualizer.setMeasurementMode(Visualizer.MEASUREMENT_MODE_PEAK_RMS);
            visualizer.setEnabled(true);
        } catch (RuntimeException e) {
            // Some ROMs refuse output-mix capture; the microphone and motion still drive the ribbon.
            Log.w(TAG, "Output-mix visualizer unavailable", e);
            if (visualizer != null) visualizer.release();
            visualizer = null;
        }
    }

    @SuppressLint("MissingPermission")
    private void startMic() {
        if (!started || recorder != null) return;
        int min = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE,
                    AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min * 2, N * 8));
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) throw new IllegalStateException("not initialised");
            recorder.startRecording();
        } catch (RuntimeException e) {
            Log.w(TAG, "Microphone meter unavailable", e);
            stopMic();
            return;
        }
        micRunning = true;
        AudioRecord rec = recorder;
        micThread = new Thread(() -> readMic(rec), "SiriIslandMic");
        micThread.setDaemon(true);
        micThread.start();
    }

    private void stopMic() {
        micRunning = false;
        if (micThread != null) {
            micThread.interrupt();
            micThread = null;
        }
        if (recorder != null) {
            try {
                recorder.stop();
            } catch (RuntimeException ignored) {
                // Already stopped by the system.
            }
            recorder.release();
            recorder = null;
        }
        micLevel = 0;
        for (int i = 0; i < 4; i++) micBands[i] = 0;
    }

    private void readMic(AudioRecord rec) {
        short[] pcm = new short[N];
        float[] re = new float[N];
        float[] im = new float[N];
        float[] out = new float[4];
        while (micRunning) {
            int n = rec.read(pcm, 0, N);
            if (n < N) {
                if (n < 0) break;
                continue;
            }
            double sum = 0;
            for (int i = 0; i < N; i++) {
                float s = pcm[i] / 32768f;
                sum += s * s;
                // Hann window.
                re[i] = s * (0.5f - 0.5f * (float) Math.cos(2 * Math.PI * i / (N - 1)));
                im[i] = 0;
            }
            fftInPlace(re, im);
            bandEnergies(re, im, RATE, out);
            float rmsDb = (float) (10 * Math.log10(sum / N + 1e-10));
            micLevel = norm(rmsDb, -58f, -18f);
            System.arraycopy(out, 0, micBands, 0, 4);
        }
    }

    /** Converts squared magnitudes into four band levels, 0..1, normalised for speech. */
    private static void bandEnergies(float[] re, float[] im, int rate, float[] out) {
        float binHz = (float) rate / N;
        for (int b = 0; b < 4; b++) {
            int lo = Math.max(1, (int) (EDGES[b] / binHz));
            int hi = Math.min(N / 2 - 1, (int) (EDGES[b + 1] / binHz));
            double e = 0;
            for (int k = lo; k <= hi; k++) e += re[k] * re[k] + im[k] * im[k];
            e /= Math.max(1, hi - lo + 1);
            float db = (float) (10 * Math.log10(e + 1e-12));
            out[b] = norm(db, -52f, -8f);
        }
    }

    private static float norm(float db, float floor, float ceil) {
        return Math.max(0f, Math.min(1f, (db - floor) / (ceil - floor)));
    }

    /** Iterative radix-2 FFT; N is a power of two. */
    private static void fftInPlace(float[] re, float[] im) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) {
                float t = re[i]; re[i] = re[j]; re[j] = t;
                t = im[i]; im[i] = im[j]; im[j] = t;
            }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double ang = -2 * Math.PI / len;
            float wr = (float) Math.cos(ang), wi = (float) Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                float cr = 1, ci = 0;
                for (int k = 0; k < len / 2; k++) {
                    int a = i + k, b = a + len / 2;
                    float xr = re[b] * cr - im[b] * ci;
                    float xi = re[b] * ci + im[b] * cr;
                    re[b] = re[a] - xr; im[b] = im[a] - xi;
                    re[a] += xr; im[a] += xi;
                    float t = cr * wr - ci * wi;
                    ci = cr * wi + ci * wr;
                    cr = t;
                }
            }
        }
    }

    private boolean anyOtherRecording(List<AudioRecordingConfiguration> configs) {
        int own = recorder != null ? recorder.getAudioSessionId() : -1;
        for (AudioRecordingConfiguration c : configs) {
            if (c.getClientAudioSessionId() != own) return true;
        }
        return false;
    }

    private static boolean anySpeech(List<AudioPlaybackConfiguration> configs) {
        for (AudioPlaybackConfiguration c : configs) {
            AudioAttributes a = c.getAudioAttributes();
            if (a.getUsage() == AudioAttributes.USAGE_ASSISTANT
                    || a.getUsage() == AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY
                    || a.getContentType() == AudioAttributes.CONTENT_TYPE_SPEECH) {
                return true;
            }
        }
        return false;
    }

    /** Output-mix level and bands from the Visualizer; false when unavailable. */
    private boolean outputBands(float[] out, float[] lvl) {
        if (visualizer == null || visualizer.getFft(fft) != Visualizer.SUCCESS) return false;
        float binHz = visualizer.getSamplingRate() / 1000f / N;
        float total = 0;
        for (int b = 0; b < 4; b++) {
            int lo = Math.max(1, (int) (EDGES[b] / binHz));
            int hi = Math.min(N / 2 - 1, (int) (EDGES[b + 1] / binHz));
            double e = 0;
            for (int k = lo; k <= hi; k++) {
                float r = fft[2 * k], i = fft[2 * k + 1];
                e += r * r + i * i;
            }
            e /= Math.max(1, hi - lo + 1);
            out[b] = norm((float) (10 * Math.log10(e + 1e-6)), 0f, 30f);
            total += out[b];
        }
        lvl[0] = total / 4f;
        return true;
    }

    private final float[] tmpBands = new float[4];
    private final float[] tmpLevel = new float[1];

    /** Called once per frame by the island; updates {@link #bands} and returns the level. */
    @Override
    public float level() {
        float t = SystemClock.uptimeMillis() / 1000f;
        float[] target = tmpBands;
        float lvl;
        boolean metered = false;
        if (recorder != null && micLevel > 0.02f) {
            System.arraycopy(micBands, 0, target, 0, 4);
            lvl = micLevel;
            metered = true;
        } else {
            for (int i = 0; i < 4; i++) target[i] = 0;
            lvl = 0;
        }
        if (outputBands(tmpBands2, tmpLevel) && tmpLevel[0] > 0.03f) {
            for (int i = 0; i < 4; i++) target[i] = Math.max(target[i], tmpBands2[i]);
            lvl = Math.max(lvl, tmpLevel[0]);
            metered = true;
        }
        if (!metered) {
            // No live signal: React Bits Strands' "listening" voice (phrases, syllables, stress), so
            // the light moves like someone speaking instead of sitting dead.
            float base = speaking ? 0.45f : othersRecording ? 0.22f : heardListening ? 0.02f : 0.12f;
            for (int i = 0; i < 4; i++) {
                target[i] = Math.min(1f, base * 2f * StrandsMotion.speech(t * (1f + 0.13f * i) + 1.7f * i));
            }
            lvl = Math.min(1f, base * 2f * StrandsMotion.speech(t));
        }
        for (int i = 0; i < 4; i++) {
            float v = Math.min(1f, target[i] * sensitivity);
            // Fast attack, slower release, like a VU meter.
            bands[i] += (v - bands[i]) * (v > bands[i] ? 0.55f : 0.12f);
        }
        float v = Math.min(1f, lvl * sensitivity);
        level += (v - level) * (v > level ? 0.5f : 0.1f);

        // Thinking: Xiao Ai listened, then stopped recording and is not talking yet.
        if (othersRecording) heardListening = true;
        boolean quiet = heardListening && !othersRecording && !speaking;
        long now = SystemClock.uptimeMillis();
        if (!quiet) quietSince = 0;
        else if (quietSince == 0) quietSince = now;
        return level;
    }

    private final float[] tmpBands2 = new float[4];

    @Override
    public void bands(float[] out) {
        System.arraycopy(bands, 0, out, 0, 4);
    }

    @Override
    public boolean thinking() {
        return quietSince != 0 && SystemClock.uptimeMillis() - quietSince > 250;
    }
}
