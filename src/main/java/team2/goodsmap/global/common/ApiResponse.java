package team2.goodsmap.global.common;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private final boolean success;
    private final T data;
    private final String message;

    // 성공 (데이터 있을 때)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 성공 (데이터 없을 때)
    public static <T> ApiResponse<T> success() {
        return new ApiResponse<>(true, null, null);
    }

    // 실패
    public static <T> ApiResponse<T> fail(String message) {
        return new ApiResponse<>(false, null, message);
    }
}