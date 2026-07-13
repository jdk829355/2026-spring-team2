package team2.goodsmap.store.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team2.goodsmap.store.enums.StoreType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "store")
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StoreType type;

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(nullable = false)
    private String address;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lat;

    @Column(nullable = false, precision = 10, scale = 7)
    private BigDecimal lng;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Store(String name, String description, StoreType type, LocalDate startDate, LocalDate endDate, String address, BigDecimal lat, BigDecimal lng){
        this.name = name;
        this.description = description;
        this.type = type;
        this.startDate = startDate;
        this.endDate = endDate;
        this.address = address;
        this.lat = lat;
        this.lng = lng;
    }

    public void update(String name, String description, StoreType type, LocalDate startDate, LocalDate endDate, String address, BigDecimal lat, BigDecimal lng) {
        // patch용 업데이트이므로 null이 아닌 필드만 업데이트
        if (name != null) {
            this.name = name;
        }
        if (description != null) {
            this.description = description;
        }
        if (type != null) {
            this.type = type;
        }

        // date끼리는 관계가 중요하니까 갱신 시 검증 필요
        if (startDate != null && endDate != null) {
            if (startDate.isAfter(endDate)) {
                throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
            }
            this.startDate = startDate;
            this.endDate = endDate;
        } else if (startDate != null) {
            if (this.endDate != null && startDate.isAfter(this.endDate)) {
                throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
            }
            this.startDate = startDate;
        } else if (endDate != null) {
            if (this.startDate != null && endDate.isBefore(this.startDate)) {
                throw new IllegalArgumentException("시작일은 종료일보다 늦을 수 없습니다.");
            }
            this.endDate = endDate;
        }

        if (address != null) {
            this.address = address;
        }
        if (lat != null) {
            this.lat = lat;
        }
        if (lng != null) {
            this.lng = lng;
        }
    }
}