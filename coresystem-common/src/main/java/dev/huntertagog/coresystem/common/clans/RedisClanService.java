package dev.huntertagog.coresystem.common.clans;

import dev.huntertagog.coresystem.common.error.CoreError;
import dev.huntertagog.coresystem.common.error.CoreErrorCode;
import dev.huntertagog.coresystem.common.error.CoreErrorSeverity;
import dev.huntertagog.coresystem.common.log.Logger;
import dev.huntertagog.coresystem.common.log.LoggerFactory;
import dev.huntertagog.coresystem.common.redis.RedisClient;
import dev.huntertagog.coresystem.platform.clans.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.*;
import java.util.stream.Collectors;

public final class RedisClanService implements ClanService {

    private static final Logger LOG = LoggerFactory.get("Clans");

    private static String dataKey(UUID id) {
        return "cs:clan:" + id + ":data";
    }

    private static String membersKey(UUID id) {
        return "cs:clan:" + id + ":members";
    }

    private static String invitesKey(UUID id) {
        return "cs:clan:" + id + ":invites";
    }

    private static String playerClanKey(UUID p) {
        return "cs:clan:by_player:" + p;
    }

    private static String tagIndexKey(String tag) {
        return "cs:clan:tag:" + tag.toLowerCase(Locale.ROOT);
    }

    @Override
    public Optional<Clan> findById(UUID clanId) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            Map<String, String> data = jedis.hgetAll(dataKey(clanId));
            if (data == null || data.isEmpty()) {
                return Optional.empty();
            }

            Map<String, String> membersRaw = jedis.hgetAll(membersKey(clanId));
            Map<UUID, ClanMember> members = new HashMap<>();
            long now = System.currentTimeMillis();

            for (Map.Entry<String, String> e : membersRaw.entrySet()) {
                try {
                    UUID pid = UUID.fromString(e.getKey());
                    String val = e.getValue(); // role|joinedAt
                    String[] parts = val.split("\\|");
                    ClanRole role = ClanRole.valueOf(parts[0]);
                    long joinedAt = parts.length > 1 ? Long.parseLong(parts[1]) : now;
                    members.put(pid, new ClanMember(pid, role, joinedAt));
                } catch (Exception ex) {
                    CoreError.of(
                                    CoreErrorCode.CLAN_MEMBER_PARSE_FAILED,
                                    CoreErrorSeverity.WARN,
                                    "Failed to parse clan member row"
                            )
                            .withCause(ex)
                            .withContextEntry("clanId", clanId.toString())
                            .withContextEntry("rowKey", e.getKey())
                            .withContextEntry("rowValue", e.getValue())
                            .log();
                }
            }

            UUID id = UUID.fromString(data.getOrDefault("id", clanId.toString()));
            String tag = data.getOrDefault("tag", "???");
            String name = data.getOrDefault("name", tag);
            UUID ownerId = UUID.fromString(data.getOrDefault("ownerId", members.keySet().stream().findFirst().orElse(clanId).toString()));
            long createdAt = parseLongSafe(data.get("createdAt"), System.currentTimeMillis());
            String motd = data.getOrDefault("motd", "");

            Clan clan = new Clan(
                    id,
                    tag,
                    name,
                    ownerId,
                    members,
                    createdAt,
                    motd
            );
            return Optional.of(clan);
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CLAN_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to load clan by id"
                    )
                    .withCause(e)
                    .withContextEntry("clanId", clanId.toString())
                    .log();
            return Optional.empty();
        }
    }

    @Override
    public Optional<Clan> findByMember(UUID playerId) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            String cid = jedis.get(playerClanKey(playerId));
            if (cid == null || cid.isEmpty()) {
                return Optional.empty();
            }
            return findById(UUID.fromString(cid));
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CLAN_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to load clan by member"
                    )
                    .withCause(e)
                    .withContextEntry("playerId", playerId.toString())
                    .log();
            return Optional.empty();
        }
    }

    @Override
    public Optional<Clan> findByTag(String tag) {
        String lowered = tag.toLowerCase(Locale.ROOT);
        try (Jedis jedis = RedisClient.get().getResource()) {
            String cid = jedis.get(tagIndexKey(lowered));
            if (cid == null || cid.isEmpty()) {
                return Optional.empty();
            }
            return findById(UUID.fromString(cid));
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CLAN_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to load clan by tag"
                    )
                    .withCause(e)
                    .withContextEntry("tag", lowered)
                    .log();
            return Optional.empty();
        }
    }

    @Override
    public Clan createClan(UUID ownerId,
                           String tag,
                           String name) {

        String loweredTag = tag.toLowerCase(Locale.ROOT).trim();
        long now = System.currentTimeMillis();
        UUID clanId = UUID.randomUUID();

        try (Jedis jedis = RedisClient.get().getResource()) {
            // bereits in Clan?
            if (jedis.exists(playerClanKey(ownerId))) {
                CoreError.of(
                                CoreErrorCode.CLAN_ALREADY_IN_CLAN,
                                CoreErrorSeverity.WARN,
                                "Owner is already in a clan"
                        )
                        .withContextEntry("ownerId", ownerId.toString())
                        .log();
                return null;
            }

            // Tag schon vergeben?
            if (jedis.exists(tagIndexKey(loweredTag))) {
                CoreError.of(
                                CoreErrorCode.CLAN_TAG_ALREADY_USED,
                                CoreErrorSeverity.WARN,
                                "Clan tag already used"
                        )
                        .withContextEntry("tag", loweredTag)
                        .log();
                return null;
            }

            String clanIdStr = clanId.toString();
            String ownerIdStr = ownerId.toString();

            Map<String, String> data = new HashMap<>();
            data.put("id", clanIdStr);
            data.put("tag", loweredTag);
            data.put("name", name);
            data.put("ownerId", ownerIdStr);
            data.put("createdAt", Long.toString(now));
            data.put("motd", "");

            String memberVal = ClanRole.OWNER.name() + "|" + now;

            Transaction tx = jedis.multi();
            tx.hset(dataKey(clanId), data);
            tx.hset(membersKey(clanId), ownerIdStr, memberVal);
            tx.set(playerClanKey(ownerId), clanIdStr);
            tx.set(tagIndexKey(loweredTag), clanIdStr);
            tx.exec();

            Map<UUID, ClanMember> members = Map.of(
                    ownerId, new ClanMember(ownerId, ClanRole.OWNER, now)
            );

            return new Clan(
                    clanId,
                    loweredTag,
                    name,
                    ownerId,
                    members,
                    now,
                    ""
            );
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CLAN_REDIS_FAILURE,
                            CoreErrorSeverity.ERROR,
                            "Failed to create clan"
                    )
                    .withCause(e)
                    .withContextEntry("ownerId", ownerId.toString())
                    .withContextEntry("tag", loweredTag)
                    .withContextEntry("name", name)
                    .log();
            return null;
        }
    }

    @Override
    public boolean disbandClan(UUID clanId, UUID requester) {
        Optional<Clan> opt = findById(clanId);
        if (opt.isEmpty()) {
            return false;
        }
        Clan clan = opt.get();
        if (!clan.isOwner(requester)) {
            return false;
        }

        try (Jedis jedis = RedisClient.get().getResource()) {
            // alle Member-Mappings entfernen
            for (UUID memberId : clan.members().keySet()) {
                jedis.del(playerClanKey(memberId));
            }

            // Tag-Index entfernen
            jedis.del(tagIndexKey(clan.tag()));

            // Clan-spezifische Keys löschen
            jedis.del(dataKey(clanId));
            jedis.del(membersKey(clanId));
            jedis.del(invitesKey(clanId));

            return true;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CLAN_REDIS_FAILURE,
                            CoreErrorSeverity.ERROR,
                            "Failed to disband clan"
                    )
                    .withCause(e)
                    .withContextEntry("clanId", clanId.toString())
                    .log();
            return false;
        }
    }

    @Override
    public boolean inviteMember(UUID clanId,
                                UUID inviter,
                                UUID target) {

        Optional<Clan> opt = findById(clanId);
        if (opt.isEmpty()) return false;
        Clan clan = opt.get();

        ClanMember inviterMember = clan.members().get(inviter);
        if (inviterMember == null) return false; // kein Mitglied

        // Basic-Permission: OWNER + OFFICER dürfen einladen
        if (inviterMember.role() == ClanRole.MEMBER) {
            return false;
        }

        // Target bereits in einem Clan?
        if (findByMember(target).isPresent()) {
            return false;
        }

        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.sadd(invitesKey(clanId), target.toString());
            return true;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CLAN_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to store clan invite"
                    )
                    .withCause(e)
                    .withContextEntry("clanId", clanId.toString())
                    .withContextEntry("inviter", inviter.toString())
                    .withContextEntry("target", target.toString())
                    .log();
            return false;
        }
    }

    @Override
    public boolean acceptInvite(UUID clanId,
                                UUID playerId) {

        // bereits in Clan?
        if (findByMember(playerId).isPresent()) {
            return false;
        }

        try (Jedis jedis = RedisClient.get().getResource()) {
            String pid = playerId.toString();

            if (!jedis.sismember(invitesKey(clanId), pid)) {
                CoreError.of(
                                CoreErrorCode.CLAN_INVITE_MISSING,
                                CoreErrorSeverity.WARN,
                                "Missing clan invite for accept"
                        )
                        .withContextEntry("clanId", clanId.toString())
                        .withContextEntry("playerId", pid)
                        .log();
                return false;
            }

            long now = System.currentTimeMillis();
            String memberVal = ClanRole.MEMBER.name() + "|" + now;

            Transaction tx = jedis.multi();
            tx.srem(invitesKey(clanId), pid);
            tx.hset(membersKey(clanId), pid, memberVal);
            tx.set(playerClanKey(playerId), clanId.toString());
            tx.exec();

            return true;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CLAN_REDIS_FAILURE,
                            CoreErrorSeverity.ERROR,
                            "Failed to accept clan invite"
                    )
                    .withCause(e)
                    .withContextEntry("clanId", clanId.toString())
                    .withContextEntry("playerId", playerId.toString())
                    .log();
            return false;
        }
    }

    @Override
    public boolean kickMember(UUID clanId,
                              UUID requester,
                              UUID target) {

        Optional<Clan> opt = findById(clanId);
        if (opt.isEmpty()) return false;
        Clan clan = opt.get();

        ClanMember reqMember = clan.members().get(requester);
        ClanMember targetMember = clan.members().get(target);

        if (reqMember == null || targetMember == null) {
            return false;
        }

        // Owner darf alle; Officer darf nur Member; Member darf niemanden
        if (reqMember.role() == ClanRole.MEMBER) {
            return false;
        }
        if (targetMember.role() == ClanRole.OWNER) {
            return false;
        }
        if (reqMember.role() == ClanRole.OFFICER &&
                targetMember.role() == ClanRole.OFFICER) {
            return false;
        }

        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.hdel(membersKey(clanId), target.toString());
            jedis.del(playerClanKey(target));
            return true;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CLAN_REDIS_FAILURE,
                            CoreErrorSeverity.ERROR,
                            "Failed to kick clan member"
                    )
                    .withCause(e)
                    .withContextEntry("clanId", clanId.toString())
                    .withContextEntry("requester", requester.toString())
                    .withContextEntry("target", target.toString())
                    .log();
            return false;
        }
    }

    @Override
    public boolean setRole(UUID clanId,
                           UUID requester,
                           UUID target,
                           ClanRole role) {

        Optional<Clan> opt = findById(clanId);
        if (opt.isEmpty()) return false;
        Clan clan = opt.get();

        ClanMember reqMember = clan.members().get(requester);
        ClanMember tgtMember = clan.members().get(target);
        if (reqMember == null || tgtMember == null) return false;

        // nur Owner darf Rollen vergeben
        if (!clan.isOwner(requester)) {
            return false;
        }

        // Owner-Rolle nicht verändern (kein Set auf andere Owner)
        if (tgtMember.role() == ClanRole.OWNER || role == ClanRole.OWNER) {
            return false;
        }

        long joinedAt = tgtMember.joinedAt();
        String val = role.name() + "|" + joinedAt;

        try (Jedis jedis = RedisClient.get().getResource()) {
            jedis.hset(membersKey(clanId), target.toString(), val);
            return true;
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CLAN_REDIS_FAILURE,
                            CoreErrorSeverity.ERROR,
                            "Failed to set clan member role"
                    )
                    .withCause(e)
                    .withContextEntry("clanId", clanId.toString())
                    .withContextEntry("requester", requester.toString())
                    .withContextEntry("target", target.toString())
                    .withContextEntry("role", role.name())
                    .log();
            return false;
        }
    }

    @Override
    public List<UUID> getMembers(UUID clanId) {
        try (Jedis jedis = RedisClient.get().getResource()) {
            Map<String, String> raw = jedis.hgetAll(membersKey(clanId));
            if (raw == null || raw.isEmpty()) return List.of();
            return raw.keySet().stream()
                    .map(this::parseUuidSafe)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CLAN_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to load clan members"
                    )
                    .withCause(e)
                    .withContextEntry("clanId", clanId.toString())
                    .log();
            return List.of();
        }
    }

    @Override
    public boolean canManageIsland(UUID clanId, UUID playerId) {
        Optional<Clan> opt = findById(clanId);
        if (opt.isEmpty()) return false;
        Clan clan = opt.get();

        ClanMember member = clan.members().get(playerId);
        if (member == null) return false;

        return switch (member.role()) {
            case OWNER, OFFICER -> true;
            case ADMIN -> false;
            case MEMBER -> false;
        };
    }

    @Override
    public List<ClanInvite> pollPendingInvites(UUID memberId) {
        List<ClanInvite> invites = new ArrayList<>();
        try (Jedis jedis = RedisClient.get().getResource()) {
            Set<String> clanIds = jedis.keys("cs:clan:*:invites");
            for (String inviteKey : clanIds) {
                if (jedis.sismember(inviteKey, memberId.toString())) {
                    // Clan ID extrahieren
                    String[] parts = inviteKey.split(":");
                    if (parts.length >= 4) {
                        UUID clanId = UUID.fromString(parts[2]);
                        Optional<Clan> clanOpt = findById(clanId);
                        clanOpt.ifPresent(clan -> invites.add(new ClanInvite(clanId, clan.tag(), clan.name())));
                    }
                }
            }
        } catch (Exception e) {
            CoreError.of(
                            CoreErrorCode.CLAN_REDIS_FAILURE,
                            CoreErrorSeverity.WARN,
                            "Failed to poll pending clan invites"
                    )
                    .withCause(e)
                    .withContextEntry("memberId", memberId.toString())
                    .log();
        }
        return invites;
    }

    // ----------------------------------------------------

    private long parseLongSafe(String val, long def) {
        if (val == null) return def;
        try {
            return Long.parseLong(val);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private UUID parseUuidSafe(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            LOG.warn("Invalid UUID in clan data: {}", s);
            return null;
        }
    }
}
