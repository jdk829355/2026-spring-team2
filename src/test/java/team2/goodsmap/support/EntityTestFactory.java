package team2.goodsmap.support;

import org.springframework.test.util.ReflectionTestUtils;
import team2.goodsmap.goods.entity.Animation;
import team2.goodsmap.goods.entity.Goods;
import team2.goodsmap.store.entity.Store;
import team2.goodsmap.store.entity.StoreGoods;
import team2.goodsmap.store.enums.StoreType;

import java.math.BigDecimal;

/**
 * Animation, Goods, Store, StoreGoods는 @Builder가 없고 생성자가 protected라
 * 테스트에서 값을 채운 인스턴스를 만들 방법이 없다.
 * 리플렉션으로 protected 기본 생성자를 호출하고, 필드는 ReflectionTestUtils로 직접 주입한다.
 * 프로덕션 코드에는 영향이 없는 테스트 전용 유틸이다.
 */
public class EntityTestFactory {

    private static <T> T newInstance(Class<T> clazz) {
        try {
            var constructor = clazz.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("테스트용 엔티티 생성 실패: " + clazz.getSimpleName(), e);
        }
    }

    public static Animation animation(Long id, String title) {
        Animation animation = newInstance(Animation.class);
        ReflectionTestUtils.setField(animation, "id", id);
        ReflectionTestUtils.setField(animation, "title", title);
        return animation;
    }

    public static Goods goods(Long id, String name, Animation animation) {
        Goods goods = newInstance(Goods.class);
        ReflectionTestUtils.setField(goods, "id", id);
        ReflectionTestUtils.setField(goods, "name", name);
        ReflectionTestUtils.setField(goods, "animation", animation);
        return goods;
    }

    public static Store store(Long id, String name, StoreType type, String address,
                               BigDecimal lat, BigDecimal lng) {
        Store store = newInstance(Store.class);
        ReflectionTestUtils.setField(store, "id", id);
        ReflectionTestUtils.setField(store, "name", name);
        ReflectionTestUtils.setField(store, "type", type);
        ReflectionTestUtils.setField(store, "address", address);
        ReflectionTestUtils.setField(store, "lat", lat);
        ReflectionTestUtils.setField(store, "lng", lng);
        return store;
    }

    public static StoreGoods storeGoods(Long id, int price, int stock, String imagePath,
                                          Goods goods, Store store) {
        StoreGoods storeGoods = newInstance(StoreGoods.class);
        ReflectionTestUtils.setField(storeGoods, "id", id);
        ReflectionTestUtils.setField(storeGoods, "price", price);
        ReflectionTestUtils.setField(storeGoods, "stock", stock);
        ReflectionTestUtils.setField(storeGoods, "imagePath", imagePath);
        ReflectionTestUtils.setField(storeGoods, "goods", goods);
        ReflectionTestUtils.setField(storeGoods, "store", store);
        return storeGoods;
    }
}
