package com.wang.gulimall.product.dao;

import com.wang.common.constant.ProductConstant;
import com.wang.gulimall.product.entity.SpuInfoEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * spu信息
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2022-06-09 14:48:11
 */
@Mapper
public interface SpuInfoDao extends BaseMapper<SpuInfoEntity> {
    void updateSpuStatus(@Param("spuId") Long spuId, @Param("code") int code);

}
