package com.avilix.teleportoffline;

import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerPlayerGameMode;
import net.minecraft.server.MinecraftServer;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.Util;
import com.mojang.authlib.GameProfile;

import java.io.*;
import java.nio.file.*;

import net.minecraft.network.chat.Component;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.UUID;
import com.google.gson.*;


public class TeleportOfflineHandler {

    // Один экземпляр Gson для парсинга usercache.json
    private static final Gson GSON = new Gson();

    public static int teleport(CommandSourceStack src, String playerName, double x, double y, double z) {
        MinecraftServer server = src.getServer();
        Path dataDir = server.getWorldPath(LevelResource.PLAYER_DATA_DIR);

        Path usercache = server.getServerDirectory().resolve("usercache.json");
        UUID uuid = null;
        try {
            uuid = lookupUUID(usercache, playerName);
        } catch (IOException e) {
            src.sendFailure(Component.literal("Не удалось прочитать usercache.json: " + e.getMessage()));
            return 0;
        }

        Path playerPath = null;
        if (uuid != null) {
            playerPath = dataDir.resolve(uuid.toString() + ".dat");
            if (!Files.exists(playerPath)) {
                src.sendSystemMessage(Component.literal("Файл данных для UUID " + uuid + " не найден."));
                playerPath = null;
            }
        } else {
            src.sendSystemMessage(Component.literal("Игрок «" + playerName + "» не найден в usercache.json."));
        }

        // 2) Если по UUID не нашли — можно попробовать ваш старый обход по содержимому NBT
        if (playerPath == null) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dataDir, "*.dat")) {
                for (Path datFile : stream) {
                    try (InputStream in = Files.newInputStream(datFile);
                         BufferedInputStream bin = new BufferedInputStream(in)) {
                        CompoundTag tag = NbtIo.readCompressed(bin, NbtAccounter.unlimitedHeap());
                        if (tag.contains("bukkit", Tag.TAG_COMPOUND)) {
                            CompoundTag b = tag.getCompound("bukkit");
                            if (b.contains("lastKnownName", Tag.TAG_STRING)
                                    && playerName.equalsIgnoreCase(b.getString("lastKnownName"))) {
                                playerPath = datFile;
                                src.sendSystemMessage(Component.literal("Найден NBT-файл по lastKnownName."));
                                break;
                            }
                        }
                    } catch (IOException ioe) {
                        // пропускаем битые файлы
                    }
                }
            } catch (IOException e) {
                src.sendFailure(Component.literal("Ошибка доступа к playerdata: " + e.getMessage()));
                return 0;
            }
        }

        if (playerPath == null) {
            src.sendFailure(Component.literal("Не найден файл данных для игрока «" + playerName + "»."));
            return 0;
        }

        // 3) Чтение, правка и запись координат
        try {
            CompoundTag tag;
            try (InputStream in = Files.newInputStream(playerPath);
                 BufferedInputStream bin = new BufferedInputStream(in)) {
                tag = NbtIo.readCompressed(bin, NbtAccounter.unlimitedHeap());
            }

            // Задаём новую позицию
            ListTag pos = new ListTag();
            pos.add(DoubleTag.valueOf(x));
            pos.add(DoubleTag.valueOf(y));
            pos.add(DoubleTag.valueOf(z));
            tag.put("Pos", pos);

            // Обнуляем скорость и падение
            tag.put("Motion", new ListTag());
            tag.putFloat("FallDistance", 0f);

            try (OutputStream out = Files.newOutputStream(playerPath);
                 BufferedOutputStream bout = new BufferedOutputStream(out)) {
                NbtIo.writeCompressed(tag, bout);
            }

            src.sendSuccess(
                    () -> Component.literal("Оффлайн-телепорт игрока «" + playerName + "» на [" +
                            String.format("%.2f", x) + ", " +
                            String.format("%.2f", y) + ", " +
                            String.format("%.2f", z) + "] выполнен успешно."),
                    true
            );
            return 1;

        } catch (Exception e) {
            src.sendFailure(Component.literal("Ошибка при работе с NBT: " + e.getMessage()));
            e.printStackTrace();
            return 0;
        }
    }

    /**
     * Пробегает по массиву в usercache.json и возвращает UUID игрока по имени.
     */
    private static UUID lookupUUID(Path usercachePath, String playerName) throws IOException {
        if (!Files.exists(usercachePath)) {
            throw new FileNotFoundException("usercache.json не найден по пути " + usercachePath);
        }
        String json = Files.readString(usercachePath);
        JsonArray arr = JsonParser.parseString(json).getAsJsonArray();
        for (JsonElement el : arr) {
            JsonObject obj = el.getAsJsonObject();
            if (playerName.equalsIgnoreCase(obj.get("name").getAsString())) {
                return UUID.fromString(obj.get("uuid").getAsString());
            }
        }
        return null;
    }
}