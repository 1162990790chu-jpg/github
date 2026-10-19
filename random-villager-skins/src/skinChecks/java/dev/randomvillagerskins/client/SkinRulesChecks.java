package dev.randomvillagerskins.client;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.Random;
import java.util.UUID;

/** Requirement checks runnable by Gradle without Minecraft, a GPU, or a test library. */
public final class SkinRulesChecks {
    public static void main(String[] args) throws IOException {
        Random random = new Random(0x51A1B);
        for (int count : new int[]{1, 2, 18, 23}) {
            BitSet seen = new BitSet(count);
            for (int i = 0; i < 4096; i++) {
                UUID original = new UUID(random.nextLong(), random.nextLong());
                int index = SkinRules.indexFor(original, count);
                require(index >= 0 && index < count, "Skin index outside the available pool");
                require(index == SkinRules.indexFor(UUID.fromString(original.toString()), count),
                    "The same saved villager UUID changed its assigned skin");
                seen.set(index);
            }
            require(seen.cardinality() == count, "Random assignment never selected some available skins");
        }
        try {
            SkinRules.indexFor(UUID.randomUUID(), 0);
            throw new AssertionError("An empty skin pool must be rejected");
        } catch (IllegalArgumentException expected) { }

        require(SkinRules.isPng("村民.PNG"), "Uppercase PNG extensions must work");
        require(!SkinRules.isPng("steve.png.disabled"), "Disabled files must be ignored");
        require(SkinRules.isSlim("alex_slim.PNG"), "Slim model suffix was not recognized");
        require(!SkinRules.isSlim("steve.png"), "A standard skin must use the wide model");

        SkinRules.validatePng(png(64, 64));
        reject(png(64, 32));
        reject(png(128, 128));
        reject(new byte[0]);
        reject("not a PNG image".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        reject(new byte[SkinRules.MAX_PNG_BYTES + 1]);
        byte[] damaged = png(64, 64);
        damaged[0] = 0;
        reject(damaged);
        System.out.println("Skin checks passed: saved UUID stability, pool diversity, PNG validation, and slim/wide naming.");
    }

    private static byte[] png(int width, int height) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB), "png", bytes);
        return bytes.toByteArray();
    }

    private static void reject(byte[] bytes) {
        try {
            SkinRules.validatePng(bytes);
            throw new AssertionError("Invalid or unsupported skin PNG was accepted");
        } catch (IOException expected) { }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
