package team2.goodsmap.store.dto.response;

import team2.goodsmap.store.entity.StoreAdmin;

public record StoreAdminListResponse(
        Long adminId,
        Long userId,
        String name,
        String email
) {
    public static StoreAdminListResponse from(StoreAdmin storeAdmin) {
        return new StoreAdminListResponse(
                storeAdmin.getId(),
                storeAdmin.getUser().getId(),
                storeAdmin.getUser().getName(),
                storeAdmin.getUser().getEmail()
        );
    }
}
