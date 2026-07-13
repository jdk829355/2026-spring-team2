package team2.goodsmap.global.location.exception;

import lombok.Getter;

@Getter
public abstract class KakaoApiException extends RuntimeException {

    private final int upstreamStatus;
    private final Integer kakaoCode;

    protected KakaoApiException(
            String message,
            int upstreamStatus,
            Integer kakaoCode
    ) {
        super(message);
        this.upstreamStatus = upstreamStatus;
        this.kakaoCode = kakaoCode;
    }

    protected KakaoApiException(
            String message,
            int upstreamStatus,
            Integer kakaoCode,
            Throwable cause
    ) {
        super(message, cause);
        this.upstreamStatus = upstreamStatus;
        this.kakaoCode = kakaoCode;
    }
}