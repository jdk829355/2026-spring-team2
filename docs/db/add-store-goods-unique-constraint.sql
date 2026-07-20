-- 적용 전에 중복 데이터가 없는지 확인한다.
SELECT store_id, goods_id, COUNT(*)
FROM store_goods
GROUP BY store_id, goods_id
HAVING COUNT(*) > 1;

-- 위 조회 결과가 없을 때 적용한다.
ALTER TABLE store_goods
    ADD CONSTRAINT "UQ_STORE_GOODS" UNIQUE (store_id, goods_id);
