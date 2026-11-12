package org.vwtfafa.quicktrash.model;

import java.util.UUID;

public record RecycleEntry(
    long id,
    UUID playerId,
    String playerName,
    String material,
    int amount,
    String itemData,
    long timestamp,
    String reason
) { }
