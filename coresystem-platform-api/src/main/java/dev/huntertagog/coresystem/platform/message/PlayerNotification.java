package dev.huntertagog.coresystem.platform.message;

import java.util.Objects;
import java.util.Optional;

public final class PlayerNotification {

    public enum Channel {
        CHAT,
        ACTION_BAR,
        TITLE,
        SUBTITLE,
        SYSTEM,
        TOAST
    }

    public enum Severity {
        INFO,
        SUCCESS,
        WARNING,
        ERROR
    }

    public static final class TitleTimings {
        private final int fadeInTicks;
        private final int stayTicks;
        private final int fadeOutTicks;

        public TitleTimings(int fadeInTicks, int stayTicks, int fadeOutTicks) {
            this.fadeInTicks = fadeInTicks;
            this.stayTicks = stayTicks;
            this.fadeOutTicks = fadeOutTicks;
        }

        public int fadeInTicks() {
            return fadeInTicks;
        }

        public int stayTicks() {
            return stayTicks;
        }

        public int fadeOutTicks() {
            return fadeOutTicks;
        }
    }

    private final Channel channel;
    private final Severity severity;

    private final PlatformText message;
    private final PlatformText subtitle;     // optional
    private final TitleTimings titleTimings; // optional

    private PlayerNotification(Channel channel,
                               Severity severity,
                               PlatformText message,
                               PlatformText subtitle,
                               TitleTimings titleTimings) {
        this.channel = Objects.requireNonNull(channel, "channel");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.message = Objects.requireNonNull(message, "message");
        this.subtitle = subtitle;
        this.titleTimings = titleTimings;
    }

    public Channel channel() {
        return channel;
    }

    public Severity severity() {
        return severity;
    }

    public PlatformText message() {
        return message;
    }

    public Optional<PlatformText> subtitle() {
        return Optional.ofNullable(subtitle);
    }

    public Optional<TitleTimings> titleTimings() {
        return Optional.ofNullable(titleTimings);
    }

    // -------------------------------------------------
    // Factories (Chat)
    // -------------------------------------------------
    public static PlayerNotification chatInfo(PlatformText msg) {
        return of(Channel.CHAT, Severity.INFO, msg);
    }

    public static PlayerNotification chatSuccess(PlatformText msg) {
        return of(Channel.CHAT, Severity.SUCCESS, msg);
    }

    public static PlayerNotification chatWarn(PlatformText msg) {
        return of(Channel.CHAT, Severity.WARNING, msg);
    }

    public static PlayerNotification chatError(PlatformText msg) {
        return of(Channel.CHAT, Severity.ERROR, msg);
    }

    // -------------------------------------------------
    // Factories (ActionBar)
    // -------------------------------------------------
    public static PlayerNotification actionBarInfo(PlatformText msg) {
        return of(Channel.ACTION_BAR, Severity.INFO, msg);
    }

    public static PlayerNotification actionBarSuccess(PlatformText msg) {
        return of(Channel.ACTION_BAR, Severity.SUCCESS, msg);
    }

    public static PlayerNotification actionBarWarn(PlatformText msg) {
        return of(Channel.ACTION_BAR, Severity.WARNING, msg);
    }

    public static PlayerNotification actionBarError(PlatformText msg) {
        return of(Channel.ACTION_BAR, Severity.ERROR, msg);
    }

    // -------------------------------------------------
    // Factories (Toast)
    // -------------------------------------------------
    public static PlayerNotification toastInfo(PlatformText msg) {
        return of(Channel.TOAST, Severity.INFO, msg);
    }

    public static PlayerNotification toastSuccess(PlatformText msg) {
        return of(Channel.TOAST, Severity.SUCCESS, msg);
    }

    public static PlayerNotification toastWarn(PlatformText msg) {
        return of(Channel.TOAST, Severity.WARNING, msg);
    }

    public static PlayerNotification toastError(PlatformText msg) {
        return of(Channel.TOAST, Severity.ERROR, msg);
    }

    // -------------------------------------------------
    // Factories (Title / Subtitle)
    // -------------------------------------------------
    public static PlayerNotification title(Severity severity,
                                           PlatformText title,
                                           PlatformText subtitle,
                                           TitleTimings timings) {
        return new PlayerNotification(Channel.TITLE, severity, title, subtitle, timings);
    }

    public static PlayerNotification subtitle(Severity severity, PlatformText subtitle) {
        return new PlayerNotification(Channel.SUBTITLE, severity, subtitle, null, null);
    }

    // -------------------------------------------------
    // Base builders
    // -------------------------------------------------
    public static PlayerNotification of(Channel channel, Severity severity, PlatformText msg) {
        return new PlayerNotification(channel, severity, msg, null, null);
    }

    public PlayerNotification withSubtitle(PlatformText subtitle) {
        return new PlayerNotification(this.channel, this.severity, this.message, subtitle, this.titleTimings);
    }

    public PlayerNotification withTimings(TitleTimings timings) {
        return new PlayerNotification(this.channel, this.severity, this.message, this.subtitle, timings);
    }
}
