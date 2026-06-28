package com.share.goods.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.share.goods.domain.Category;

import java.util.List;

/**
 * 商品分类 Service 接口
 *
 * @author share
 */
public interface ICategoryService extends IService<Category> {

    /**
     * 查询分类列表
     */
    List<Category> selectCategoryList(Category category);

    /**
     * 查询分类树
     */
    List<Category> selectCategoryTree();

    /**
     * 删除分类（含子分类校验）
     */
    boolean removeCategory(Long id);
}
