package dev.randomvillagerskins.client;

import com.mojang.blaze3d.platform.NativeImage;
import dev.randomvillagerskins.RandomVillagerSkinsClient;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Stream;

/** PNG files are read on resource reload, never during per-villager rendering. */
public final class SkinLibrary {
    private final Path directory;
    private volatile List<PlayerSkin> customSkins = List.of();
    private Set<Identifier> registeredTextures = Set.of();
    private volatile boolean reloadRequested = true;

    public SkinLibrary(Path directory) {
        this.directory = directory;
    }

    public void requestReload() {
        reloadRequested = true;
    }

    public PlayerSkin skinFor(UUID villagerId) {
        List<PlayerSkin> skins = customSkins;
        return skins.isEmpty() ? DefaultPlayerSkin.get(villagerId)
            : skins.get(SkinRules.indexFor(villagerId, skins.size()));
    }

    public void reloadIfNeeded(TextureManager textures) {
        if (!reloadRequested) return;
        reloadRequested = false;
        List<Path> files;
        try {
            Files.createDirectories(directory);
            writeInstructions();
            try (Stream<Path> paths = Files.list(directory)) {
                files = paths.filter(Files::isRegularFile)
                    .filter(path -> SkinRules.isPng(path.getFileName().toString()))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();
            }
        } catch (IOException error) {
            RandomVillagerSkinsClient.LOGGER.warn("Could not read the local villager skin folder; retaining current skins.", error);
            return;
        }

        List<PlayerSkin> loaded = new ArrayList<>();
        Set<Identifier> nextTextures = new HashSet<>();
        for (Path file : files) {
            try {
                if (Files.size(file) > SkinRules.MAX_PNG_BYTES)
                    throw new IOException("PNG exceeds the 2 MiB skin file limit");
                byte[] png = Files.readAllBytes(file);
                SkinRules.validatePng(png);
                boolean slim = SkinRules.isSlim(file.getFileName().toString());
                String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(png));
                Identifier id = Identifier.fromNamespaceAndPath(RandomVillagerSkinsClient.MOD_ID,
                    "custom/" + hash + (slim ? "_slim" : "_wide"));
                if (nextTextures.contains(id)) continue;

                NativeImage image = NativeImage.read(new ByteArrayInputStream(png));
                DynamicTexture texture = null;
                boolean registered = false;
                try {
                    if (image.getWidth() != 64 || image.getHeight() != 64)
                        throw new IOException("Skin must be 64 x 64 pixels");
                    texture = new DynamicTexture(() -> "Villager skin: " + file.getFileName(), image);
                    textures.register(id, texture);
                    registered = true;
                } finally {
                    if (!registered) {
                        if (texture != null) texture.close();
                        else image.close();
                    }
                }

                ClientAsset.Texture body = new ClientAsset.ResourceTexture(id, id);
                loaded.add(PlayerSkin.insecure(body, null, null, slim ? PlayerModelType.SLIM : PlayerModelType.WIDE));
                nextTextures.add(id);
            } catch (Exception error) {
                RandomVillagerSkinsClient.LOGGER.warn("Skipping villager skin {}: {}", file.getFileName(), error.getMessage());
            }
        }

        customSkins = List.copyOf(loaded);
        for (Identifier old : registeredTextures) {
            if (!nextTextures.contains(old)) textures.release(old);
        }
        registeredTextures = Set.copyOf(nextTextures);
        RandomVillagerSkinsClient.LOGGER.info("Villager skins ready: {} valid local skins; {}.", loaded.size(),
            loaded.isEmpty() ? "using built-in player skins" : "using local PNG skin pool");
    }

    private void writeInstructions() throws IOException {
        Path readme = directory.resolve("皮肤说明.txt");
        if (!Files.exists(readme)) {
            Files.writeString(readme, """
                随机人形村民：将标准 64×64 的 Minecraft 玩家皮肤 PNG 放在本文件夹内。
                普通胳膊：任意名称.png；细胳膊：名称_slim.png。
                有有效 PNG 时，村民从这些自定义皮肤中随机选择；否则使用游戏自带玩家皮肤。
                同一村民按 UUID 固定分配皮肤。皮肤文件列表不变时，重进游戏后保持相同外观。
                添加或删除文件后按 F3+T 重载资源；皮肤列表改变后可能重新分配外观。
                仅改变客户端外观，不改变村民交易、职业、AI 或存档数据。
                """);
        }
    }
}
