package de.buddydog.voice;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vosk.LibVosk;
import org.vosk.LogLevel;
import org.vosk.Model;

import de.buddydog.BuddyDogMod;
import de.buddydog.PuppyEntity;
import de.maxhenkel.voicechat.api.VoicechatApi;
import de.maxhenkel.voicechat.api.VoicechatPlugin;
import de.maxhenkel.voicechat.api.VoicechatServerApi;
import de.maxhenkel.voicechat.api.events.EventRegistration;
import de.maxhenkel.voicechat.api.events.MicrophonePacketEvent;
import de.maxhenkel.voicechat.api.events.PlayerDisconnectedEvent;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Haengt sich in "Simple Voice Chat" ein:
 *  - Wenn du sprichst, bellt dein Welpe ab und zu.
 *  - Sagst du "Sitz", "Platz" oder "Komm", fuehrt er das aus (Offline-Erkennung mit Vosk).
 *
 * Fuer die Spracherkennung muss ein deutsches Vosk-Modell in
 * config/buddydog/model/ liegen (siehe README). Ohne Modell bellt er trotzdem.
 */
public class PuppyVoicePlugin implements VoicechatPlugin {

    private static final Logger LOG = LoggerFactory.getLogger("buddydog-voice");

    private final Map<UUID, PlayerVoiceState> states = new ConcurrentHashMap<>();
    // Ein einziger Worker: haelt die Reihenfolge der Pakete ein und bremst den Voicechat-Thread nicht aus.
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "buddydog-voice");
        t.setDaemon(true);
        return t;
    });

    private VoicechatServerApi serverApi;
    private Model model; // null = keine Spracherkennung

    @Override
    public String getPluginId() {
        return BuddyDogMod.MOD_ID;
    }

    @Override
    public void initialize(VoicechatApi api) {
        if (api instanceof VoicechatServerApi s) {
            this.serverApi = s;
        }
        loadModel();
    }

    @Override
    public void registerEvents(EventRegistration registration) {
        registration.registerEvent(MicrophonePacketEvent.class, this::onMicrophone);
        registration.registerEvent(PlayerDisconnectedEvent.class, this::onDisconnect);
    }

    private void loadModel() {
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve("buddydog").resolve("model");
            if (!Files.isDirectory(dir) || !Files.exists(dir.resolve("am"))) {
                LOG.warn("Kein Vosk-Modell in {} gefunden - Sprachkommandos sind aus, Bellen funktioniert trotzdem.", dir);
                return;
            }
            LibVosk.setLogLevel(LogLevel.WARNINGS);
            this.model = new Model(dir.toString());
            LOG.info("Vosk-Modell geladen, Sprachkommandos (Sitz/Platz/Komm) sind aktiv.");
        } catch (Throwable t) {
            LOG.error("Vosk konnte nicht geladen werden - Sprachkommandos sind aus.", t);
            this.model = null;
        }
    }

    private void onMicrophone(MicrophonePacketEvent event) {
        if (event.getSenderConnection() == null) return;
        if (!(event.getSenderConnection().getPlayer().getPlayer() instanceof ServerPlayer player)) return;

        byte[] opus = event.getPacket().getOpusEncodedData();
        UUID id = player.getUUID();
        MinecraftServer server = player.level().getServer();

        worker.execute(() -> {
            try {
                PlayerVoiceState st = states.computeIfAbsent(id, k -> new PlayerVoiceState(serverApi, model));
                handlePacket(st, player, server, opus);
            } catch (Throwable t) {
                LOG.debug("Fehler bei Sprachverarbeitung", t);
            }
        });
    }

    private void handlePacket(PlayerVoiceState st, ServerPlayer player, MinecraftServer server, byte[] opus) {
        long now = System.currentTimeMillis();

        // Leeres Paket = Spieler hat aufgehoert zu sprechen
        if (opus == null || opus.length == 0) {
            st.endOfSpeech();
            return;
        }

        // 1) Bellen, wenn gesprochen wird (mit Pause, damit er nicht nervt)
        if (st.nextBarkAt == 0) {
            st.nextBarkAt = now + 3000;
        }
        if (now >= st.nextBarkAt) {
            st.nextBarkAt = now + 6000 + ThreadLocalRandom.current().nextInt(14000);
            server.execute(() -> BuddyDogMod.commandPuppiesOf(player, PuppyEntity::playAmbientSound));
        }

        // 2) Sprachkommando erkennen
        PlayerVoiceState.Command cmd = st.feed(opus);
        if (cmd == null) return;

        server.execute(() -> {
            int n = switch (cmd) {
                case SIT -> BuddyDogMod.commandPuppiesOf(player, PuppyEntity::commandSit);
                case DOWN -> BuddyDogMod.commandPuppiesOf(player, PuppyEntity::commandDown);
                case STAND -> BuddyDogMod.commandPuppiesOf(player, PuppyEntity::commandStand);
            };
            if (n > 0) {
                LOG.debug("{}: Kommando {} an {} Welpe(n)", player.getName().getString(), cmd, n);
            }
        });
    }

    private void onDisconnect(PlayerDisconnectedEvent event) {
        PlayerVoiceState st = states.remove(event.getPlayerUuid());
        if (st != null) {
            worker.execute(st::close);
        }
    }
}
