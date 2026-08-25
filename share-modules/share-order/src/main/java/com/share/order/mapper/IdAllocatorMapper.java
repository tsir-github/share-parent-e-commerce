package com.share.order.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 号段 ID 分配器 Mapper（乐观锁分段取号，替代 Snowflake 订单号生成）
 *
 * @author share
 */
@Mapper
public interface IdAllocatorMapper {

    @Update("UPDATE id_allocator SET max_id = max_id + #{step}, version = version + 1 "
            + "WHERE biz_tag = #{bizTag} AND version = #{version}")
    int allocate(@Param("bizTag") String bizTag, @Param("step") int step, @Param("version") int version);

    @Select("SELECT max_id, version FROM id_allocator WHERE biz_tag = #{bizTag}")
    IdAllocatorRow selectForUpdate(@Param("bizTag") String bizTag);

    class IdAllocatorRow {
        private long maxId;
        private int version;
        public long getMaxId() { return maxId; }
        public void setMaxId(long maxId) { this.maxId = maxId; }
        public int getVersion() { return version; }
        public void setVersion(int version) { this.version = version; }
    }
}
