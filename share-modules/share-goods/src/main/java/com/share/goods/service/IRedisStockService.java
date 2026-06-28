package com.share.goods.service;

/**
 * Redis Lua 库存预扣服务
 *
 * <p>高并发场景下，先通过 Redis Lua 原子扣减，再异步同步到数据库，
 * 避免数据库行锁成为瓶颈。</p>
 *
 * <pre>{@code
 *   // 使用示例
 *   if (redisStockService.preDeduct(skuId, quantity)) {
 *       productSkuService.deductStock(skuId, quantity); // DB 落盘
 *   } else {
 *       throw new ServiceException("库存不足");
 *   }
 * }</pre>
 *
 * @author share
 */
public interface IRedisStockService {

    /**
     * Redis Lua 预扣库存（原子操作）
     *
     * @param skuId    SKU ID
     * @param quantity 扣减数量
     * @return true-扣减成功 false-库存不足
     */
    boolean preDeduct(Long skuId, int quantity);

    /**
     * Redis Lua 归还库存
     *
     * @param skuId    SKU ID
     * @param quantity 归还数量
     */
    void preRelease(Long skuId, int quantity);

    /**
     * 同步 DB 库存到 Redis（系统启动或缓存失效时调用）
     *
     * @param skuId  SKU ID
     * @param stock  DB 当前库存
     */
    void syncStock(Long skuId, int stock);
}
