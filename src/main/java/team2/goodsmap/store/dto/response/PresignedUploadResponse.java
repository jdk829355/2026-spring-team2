package team2.goodsmap.store.dto.response;

import java.time.Instant;

public record PresignedUploadResponse(
        String uploadUrl,
        String objectKey,
        Instant expiresAt
) {
}