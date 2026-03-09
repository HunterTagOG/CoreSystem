package dev.huntertagog.coresystem.fabric.common.friends.gui;

import net.minecraft.network.PacketByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record FriendGuiSnapshot(
        List<FriendEntry> friends,
        List<RequestEntry> incoming,
        List<RequestEntry> outgoing,
        FriendSettings settings
) {
    public static FriendGuiSnapshot read(PacketByteBuf buf) {
        int f = buf.readVarInt();
        List<FriendEntry> friends = new ArrayList<>(f);
        for (int i = 0; i < f; i++) friends.add(FriendEntry.read(buf));

        int in = buf.readVarInt();
        List<RequestEntry> incoming = new ArrayList<>(in);
        for (int i = 0; i < in; i++) incoming.add(RequestEntry.read(buf));

        int out = buf.readVarInt();
        List<RequestEntry> outgoing = new ArrayList<>(out);
        for (int i = 0; i < out; i++) outgoing.add(RequestEntry.read(buf));

        FriendSettings settings = FriendSettings.read(buf);
        return new FriendGuiSnapshot(friends, incoming, outgoing, settings);
    }

    public void write(PacketByteBuf buf) {
        buf.writeVarInt(friends.size());
        for (FriendEntry e : friends) e.write(buf);

        buf.writeVarInt(incoming.size());
        for (RequestEntry e : incoming) e.write(buf);

        buf.writeVarInt(outgoing.size());
        for (RequestEntry e : outgoing) e.write(buf);

        settings.write(buf);
    }

    public record FriendEntry(
            UUID uuid,
            String name,
            boolean online,
            long lastSeenEpochMillis // 0 = unknown
    ) {
        public static FriendEntry read(PacketByteBuf buf) {
            return new FriendEntry(
                    buf.readUuid(),
                    buf.readString(32),
                    buf.readBoolean(),
                    buf.readLong()
            );
        }

        public void write(PacketByteBuf buf) {
            buf.writeUuid(uuid);
            buf.writeString(name, 32);
            buf.writeBoolean(online);
            buf.writeLong(lastSeenEpochMillis);
        }
    }

    public record RequestEntry(
            UUID uuid,
            String name
    ) {
        public static RequestEntry read(PacketByteBuf buf) {
            return new RequestEntry(buf.readUuid(), buf.readString(32));
        }

        public void write(PacketByteBuf buf) {
            buf.writeUuid(uuid);
            buf.writeString(name, 32);
        }
    }

    public record FriendSettings(
            boolean allowRequests,
            boolean allowFollow,
            boolean showLastSeen
    ) {
        public static FriendSettings read(PacketByteBuf buf) {
            return new FriendSettings(
                    buf.readBoolean(),
                    buf.readBoolean(),
                    buf.readBoolean()
            );
        }

        public void write(PacketByteBuf buf) {
            buf.writeBoolean(allowRequests);
            buf.writeBoolean(allowFollow);
            buf.writeBoolean(showLastSeen);
        }
    }
}
