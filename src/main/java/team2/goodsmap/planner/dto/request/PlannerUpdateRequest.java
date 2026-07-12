package team2.goodsmap.planner.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.time.LocalDate;

/**
 * PATCH(부분 수정)용 DTO.
 * 생성(POST)과 달리 title/date는 "안 보낼 수 있다"(= null이면 그 필드는 안 바꿈).
 * 그래서 @NotBlank/@NotNull을 걸면 안 되고,
 * "보냈다면 그 값은 유효해야 한다"는 조건만 검증한다.
 */
@Getter
public class PlannerUpdateRequest {

    @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
    private String title;

    private LocalDate date;

    // title을 보내긴 했는데 빈 문자열/공백만 보낸 경우 차단
    // (@NotBlank는 null도 막아버려서 PATCH에는 못 씀)
    @JsonIgnore
    @AssertTrue(message = "제목은 공백일 수 없습니다.")
    public boolean isTitleValidIfPresent() {
        return title == null || !title.isBlank();
    }

    // 아무 필드도 안 보낸 빈 요청 차단
    @JsonIgnore
    @AssertTrue(message = "수정할 항목(title 또는 date)이 최소 하나는 필요합니다.")
    public boolean isAnyFieldPresent() {
        return title != null || date != null;
    }
}
