package dev.randomvillagerskins.client;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Locale;
import java.util.UUID;

/** Skin selection and file validation without depending on the renderer or GPU. */
public final class SkinRules {
    public static final int MAX_PNG_BYTES = 2 * 1024 * 1024;
    private static final byte[] PNG_SIGNATURE = {(byte) 137, 80, 78, 71, 13, 10, 26, 10};

    private SkinRules() {}

    public static int indexFor(UUID id, int skinCount) {
        if (skinCount < 1) throw new IllegalArgumentException("Skin pool is empty");
        return Math.floorMod(id.hashCode(), skinCount);
    }

    public static boolean isPng(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith(".png");
    }

    public static boolean isSlim(String name) {
        return name.toLowerCase(Locale.ROOT).endsWith("_slim.png");
    }

    public static void validatePng(byte[] png) throws IOException {
        if (png.length < 33 || png.length > MAX_PNG_BYTES)
            throw new IOException("Invalid skin PNG length");
        for (int i = 0; i < PNG_SIGNATURE.length; i++) {
            if (png[i] != PNG_SIGNATURE[i]) throw new IOException("File is not a PNG image");
        }
        ByteBuffer header = ByteBuffer.wrap(png);
        if (header.getInt(8) != 13 || header.getInt(12) != 0x49484452
            || header.getInt(16) != 64 || header.getInt(20) != 64)
            throw new IOException("Skin must be a standard 64 x 64 PNG");
    }
}
