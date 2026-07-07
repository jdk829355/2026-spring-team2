package team2.goodsmap.store.dto.response;

import team2.goodsmap.store.entity.StoreAdmin;

public record StoreAdminResponse(
        Long id,
        Long storeId,
        Long userId,
        String name,
        String email
) {
    public static StoreAdminResponse from(StoreAdmin storeAdmin) {
        return new StoreAdminResponse(
                storeAdmin.getId(),
                storeAdmin.getStore().getId(),
                storeAdmin.getUser().getId(),
                storeAdmin.getUser().getName(),
                storeAdmin.getUser().getEmail()
        );
    }
}
