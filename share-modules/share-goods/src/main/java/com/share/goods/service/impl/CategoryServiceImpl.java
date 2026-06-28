package com.share.goods.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.share.common.core.constant.CategoryStatus;
import com.share.common.core.exception.ServiceException;
import com.share.goods.domain.Category;
import com.share.goods.mapper.CategoryMapper;
import com.share.goods.service.ICategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 商品分类Service业务层处理
 *
 * @author share
 */
@Service
@RequiredArgsConstructor
public class CategoryServiceImpl extends ServiceImpl<CategoryMapper, Category> implements ICategoryService {

    private final CategoryMapper categoryMapper;

    public List<Category> selectCategoryList(Category category) {
        return categoryMapper.selectCategoryList(category);
    }

    public List<Category> selectCategoryTree() {
        List<Category> all = categoryMapper.selectList(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getDelFlag, "0")
                        .eq(Category::getStatus, CategoryStatus.NORMAL)
                        .orderByAsc(Category::getSort));
        return buildTree(all);
    }

    /**
     * 删除分类（含子分类校验）
     */
    public boolean removeCategory(Long id) {
        if (hasChildren(id)) {
            throw new ServiceException("存在子分类，不允许删除");
        }
        return this.removeById(id);
    }

    public boolean hasChildren(Long id) {
        Long count = categoryMapper.selectCount(
                new LambdaQueryWrapper<Category>()
                        .eq(Category::getParentId, id)
                        .eq(Category::getDelFlag, "0"));
        return count > 0;
    }

    private List<Category> buildTree(List<Category> list) {
        List<Category> trees = new ArrayList<>();
        for (Category node : list) {
            if (node.getParentId() == null || node.getParentId() == 0) {
                trees.add(findChildren(node, list));
            }
        }
        return trees;
    }

    private Category findChildren(Category node, List<Category> list) {
        List<Category> children = new ArrayList<>();
        for (Category item : list) {
            if (node.getId().equals(item.getParentId())) {
                children.add(findChildren(item, list));
            }
        }
        if (!children.isEmpty()) {
            node.setChildren(children);
        }
        return node;
    }
}
