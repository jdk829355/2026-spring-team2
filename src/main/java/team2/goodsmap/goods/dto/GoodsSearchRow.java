package team2.goodsmap.goods.dto;

public record GoodsSearchRow(
        Long goodsId,
        String goodsName,
        Long animationId,
        String animationTitle,
        String imagePath
) {
}
