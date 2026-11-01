# Buddy Dog – dein kleiner Hund (Fabric, Minecraft 26.3)

Ein Welpe, der

- ohne Auftrag herumläuft und dir folgt,
- sich **hinter dich versteckt**, sobald Monster in der Nähe sind,
- **bellt**, wenn du im Voicechat redest (nicht dauernd, mit Pausen),
- auf **„Sitz“** sitzt und auf **„Platz“** flach liegt (per Sprache **und** per Chat-Befehl).

## Voraussetzungen

- Minecraft Java **26.3**, Fabric Loader **0.20.1+**, Fabric API **0.163.0+26.3**
- Java **25**
- [Simple Voice Chat](https://modrinth.com/plugin/simple-voice-chat) (für Bellen + Sprachkommandos, sonst nur Chat-Befehle)

## Bauen

```
gradle wrapper      # falls noch kein ./gradlew vorhanden
./gradlew build
```

Die fertige Mod liegt danach in `build/libs/buddydog-1.0.0.jar`. In den `mods`-Ordner kopieren
(zusammen mit Fabric API und Simple Voice Chat – auf dem Server UND beim Spieler).

Falls Gradle meckert: aktuelle Werte für `loom_version`, `loader_version` und `fabric_api_version`
findest du auf https://fabricmc.net/develop und trägst sie in `gradle.properties` ein.

## Spracherkennung einrichten (einmalig)

1. Deutsches kleines Vosk-Modell herunterladen: **vosk-model-small-de-0.15** von https://alphacephei.com/vosk/models
2. Zip entpacken, sodass der Inhalt (Ordner `am`, `conf`, `graph`, …) direkt hier liegt:
   `config/buddydog/model/`
3. Server/Spiel neu starten. Im Log steht dann „Vosk-Modell geladen“.

Ohne Modell funktionieren Bellen und die Chat-Befehle trotzdem.

## Benutzen

| Befehl / Wort | Wirkung |
|---|---|
| `/welpe` | Holt einen gezähmten Welpen zu dir |
| „Sitz“ / `/sitz` | Hund setzt sich |
| „Platz“ / `/platz` | Hund legt sich hin |
| „Komm“, „Hier“, „Auf“ / `/komm` | Hund steht auf und folgt wieder |

Die Kommandos gelten für alle deine Welpen im Umkreis von 32 Blöcken.
Rechtsklick mit leerer Hand lässt ihn ebenfalls aufstehen/sitzen (wie bei einem Wolf).

## Dateien

- `PuppyEntity` – der Hund (Wolf-Basis, kämpft nie, Zustände Sitz/Platz)
- `HideBehindOwnerGoal` – Verstecken hinter dir, von Monstern aus gesehen
- `voice/PuppyVoicePlugin`, `voice/PlayerVoiceState` – Voicechat-Anbindung, Bellen, Vosk-Erkennung
- `client/PuppyRenderer` – Wolf-Modell, bei „Platz“ flach gedrückt
