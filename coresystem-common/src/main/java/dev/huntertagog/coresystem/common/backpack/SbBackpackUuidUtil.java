package dev.huntertagog.coresystem.common.backpack;

import net.p3pp3rf1y.sophisticatedbackpacks.backpack.wrapper.IBackpackWrapper;

import java.lang.reflect.Field;
import java.util.Optional;
import java.util.UUID;

public final class SbBackpackUuidUtil {

    private static Field ID_FIELD;

    private SbBackpackUuidUtil() {
    }

    public static Optional<UUID> getFromWrapper(IBackpackWrapper wrapper) {
        try {
            if (ID_FIELD == null) {
                // Beispiel – passe Feldnamen an deine dekompilierte API an:
                ID_FIELD = wrapper.getClass().getDeclaredField("id");
                ID_FIELD.setAccessible(true);
            }
            Object value = ID_FIELD.get(wrapper);
            if (value instanceof UUID uuid) {
                return Optional.of(uuid);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}
