package de.buddydog.voice;

import java.util.Locale;

import org.vosk.Model;
import org.vosk.Recognizer;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.opus.OpusDecoder;

/**
 * Pro Spieler: Opus-Decoder + Vosk-Erkenner. Wird nur vom Worker-Thread benutzt.
 */
final class PlayerVoiceState {

    enum Command { SIT, DOWN, STAND }

    private static final String GRAMMAR = "[\"sitz\", \"platz\", \"komm\", \"hier\", \"auf\", \"[unk]\"]";
    private static final long COOLDOWN_MS = 1500;

    long nextBarkAt = 0;

    private final OpusDecoder decoder;
    private final Recognizer recognizer; // null ohne Modell
    private long ignoreUntil = 0;

    PlayerVoiceState(VoicechatServerApi api, Model model) {
        this.decoder = api != null ? api.createDecoder() : null;
        Recognizer r = null;
        if (model != null) {
            try {
                r = new Recognizer(model, 16000f, GRAMMAR);
            } catch (Exception e) {
                r = null;
            }
        }
        this.recognizer = r;
    }

    /** Fuettert ein Opus-Paket (20 ms) und liefert ein Kommando, falls eins erkannt wurde. */
    Command feed(byte[] opus) {
        if (decoder == null || recognizer == null) return null;

        short[] pcm48 = decoder.decode(opus); // 48 kHz, mono, 16 bit
        byte[] pcm16 = downsampleTo16kLittleEndian(pcm48);

        boolean sentenceDone = recognizer.acceptWaveForm(pcm16, pcm16.length);
        String json = sentenceDone ? recognizer.getResult() : recognizer.getPartialResult();
        String text = extractText(json);
        if (text.isEmpty()) return null;

        long now = System.currentTimeMillis();
        Command cmd = null;
        if (now >= ignoreUntil) {
            // Letztes Wort zaehlt (Teilergebnisse wachsen waehrend des Sprechens)
            String[] words = text.split("\\s+");
            cmd = toCommand(words[words.length - 1]);
        }
        if (cmd != null) {
            ignoreUntil = now + COOLDOWN_MS;
            recognizer.reset();
        }
        return cmd;
    }

    void endOfSpeech() {
        if (decoder != null) decoder.resetState();
        if (recognizer != null) recognizer.reset();
    }

    void close() {
        if (decoder != null) decoder.close();
        if (recognizer != null) recognizer.close();
    }

    // ------------------------------------------------------------------ Helfer

    private static Command toCommand(String word) {
        return switch (word.toLowerCase(Locale.ROOT)) {
            case "sitz" -> Command.SIT;
            case "platz" -> Command.DOWN;
            case "komm", "hier", "auf" -> Command.STAND;
            default -> null;
        };
    }

    private static String extractText(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            if (o.has("text")) return o.get("text").getAsString().trim();
            if (o.has("partial")) return o.get("partial").getAsString().trim();
        } catch (Exception ignored) {
        }
        return "";
    }

    /** 48 kHz -> 16 kHz (Mittelwert aus je 3 Samples), als 16-bit Little-Endian Bytes. */
    private static byte[] downsampleTo16kLittleEndian(short[] in) {
        int n = in.length / 3;
        byte[] out = new byte[n * 2];
        for (int i = 0; i < n; i++) {
            int s = (in[i * 3] + in[i * 3 + 1] + in[i * 3 + 2]) / 3;
            out[i * 2] = (byte) (s & 0xFF);
            out[i * 2 + 1] = (byte) ((s >> 8) & 0xFF);
        }
        return out;
    }
}
