package com.share.goods.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.share.goods.domain.Category;

import java.util.List;

/**
 * 分类Mapper接口
 *
 * @author share
 */
public interface CategoryMapper extends BaseMapper<Category> {

    /**
     * 查询分类列表
     */
    List<Category> selectCategoryList(Category category);
}
