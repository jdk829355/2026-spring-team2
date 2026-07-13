package team2.goodsmap.store.dto.request;

public record PresignedUploadRequest(
        String fileName,
        String contentType,
        Long fileSize
) {
}