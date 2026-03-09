package dev.huntertagog.coresystem.platform.friends;

public enum FriendRelationStatus {
    NONE,           // kein Bezug
    PENDING_IN,     // ich habe eine Anfrage *eingehend* (andere Seite angefragt)
    PENDING_OUT,    // ich habe eine Anfrage *ausgehend*
    FRIENDS,        // akzeptiert
    BLOCKED         // geblockt (später)
}
