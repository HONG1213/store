package com.wang.gulimall.product.dao;

import com.wang.gulimall.product.entity.CategoryEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品三级分类
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2022-06-09 14:48:11
 */
@Mapper
public interface CategoryDao extends BaseMapper<CategoryEntity> {
	
}
