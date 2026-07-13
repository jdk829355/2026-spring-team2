package team2.goodsmap.planner.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import team2.goodsmap.user.entity.User;
import lombok.Builder;

import java.time.LocalDate;

@Entity
@Table(name = "planner")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Planner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate date;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 플래너 수정 (전달된 값만 변경)
    public void update(String title, LocalDate date) {
        if (title != null) this.title = title;
        if (date != null) this.date = date;
    }

    @Builder //'내가 살 것 담기'에서 추가 시 사용해야
    public Planner(String title, LocalDate date, User user) {
        this.title = title;
        this.date = date;
        this.user = user;
    }
}