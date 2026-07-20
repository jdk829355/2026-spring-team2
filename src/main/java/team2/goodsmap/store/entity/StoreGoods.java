package team2.goodsmap.store.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import team2.goodsmap.goods.entity.Goods;

import java.time.LocalDateTime;

@Entity
@Table(name = "store_goods", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_STORE_GOODS", columnNames = {"store_id", "goods_id"})
})
@EntityListeners(AuditingEntityListener.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreGoods {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private int price;

    @Column(nullable = false)
    private int stock;

    @Column(columnDefinition = "TEXT")
    private String imagePath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "goods_id", nullable = false)
    private Goods goods;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public StoreGoods(int price, int stock, String imagePath, Goods goods, Store store) {
        this.price = price;
        this.stock = stock;
        this.imagePath = imagePath;
        this.goods = goods;
        this.store = store;
    }

    public void update(Integer price, Integer stock, String imagePath) {
        // null일 때는 수정 안 함
        if(price != null)
            this.price = price;
        if(stock != null)
            this.stock = stock;
        if(imagePath != null)
            this.imagePath = imagePath;
    }

    public void updateImagePath(@NotBlank String s) {
        this.imagePath = s;
    }
}
