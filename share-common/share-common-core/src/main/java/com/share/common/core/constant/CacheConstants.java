package com.share.common.core.constant;

/**
 * 缓存常量信息
 *
 * @author share
 */
public class CacheConstants
{
    /**
     * 缓存有效期，默认720（分钟）
     */
    public final static long EXPIRATION = 720;

    /**
     * 缓存刷新时间，默认120（分钟）
     */
    public final static long REFRESH_TIME = 120;

    /**
     * 密码最大错误次数
     */
    public final static int PASSWORD_MAX_RETRY_COUNT = 5;

    /**
     * 密码锁定时间，默认10（分钟）
     */
    public final static long PASSWORD_LOCK_TIME = 10;

    /**
     * 权限缓存前缀
     */
    public final static String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 登录IP黑名单 cache key
     */
    public static final String SYS_LOGIN_BLACKIPLIST = SYS_CONFIG_KEY + "sys.login.blackIPList";

    // ==================== 业务缓存 key ====================

    /** 商品详情缓存（读多写少，30min TTL） */
    public static final String PRODUCT_DETAIL_KEY = "product:detail:";

    /** 分类树缓存（几乎不变，1h TTL） */
    public static final String CATEGORY_TREE_KEY = "category:tree";

    /** 分类列表缓存 */
    public static final String CATEGORY_LIST_KEY = "category:list";

    /** 首页聚合数据缓存（5min TTL） */
    public static final String HOMEPAGE_DATA_KEY = "homepage:data";

    // ==================== 库存缓存 key ====================

    /** 商品 SKU 库存缓存前缀 */
    public static final String STOCK_SKU_KEY = "stock:sku:";

    /** 商品 SKU 库存锁前缀 */
    public static final String STOCK_SKU_LOCK_KEY = "stock:sku:lock:";

    /** 秒杀库存缓存前缀 */
    public static final String SECKILL_STOCK_KEY = "seckill:stock:";

    /** 库存扣减幂等前缀（goods 侧 per-order + per-sku 防重复扣） */
    public static final String STOCK_DEDUCT_DEDUP_KEY = "stock:deduct:";

    /** 库存归还幂等前缀 */
    public static final String STOCK_RELEASE_DEDUP_KEY = "stock:release:";

    // ==================== P0 读缓存 key ====================

    /** 评价统计缓存前缀 (10min TTL) */
    public static final String REVIEW_STATS_KEY = "review:stats:";

    /** 商家信息缓存前缀 (30min TTL) */
    public static final String MERCHANT_INFO_KEY = "merchant:info:";

    /** SKU 信息缓存前缀 (30min TTL) */
    public static final String SKU_INFO_KEY = "sku:info:";

    /** 秒杀活动列表缓存 (1min TTL) */
    public static final String SECKILL_LIST_KEY = "seckill:list";

    /** 秒杀活动详情缓存前缀 (1min TTL) */
    public static final String SECKILL_DETAIL_KEY = "seckill:detail:";

    // ==================== 支付缓存 key ====================

    /** 支付锁前缀 */
    public static final String PAYMENT_LOCK_KEY = "payment:lock:";

    // ==================== 幂等 Token ====================

    /** 幂等 Token 缓存前缀（5min TTL，单次有效） */
    public static final String IDEMPOTENT_TOKEN_KEY = "idempotent:token:";
}
