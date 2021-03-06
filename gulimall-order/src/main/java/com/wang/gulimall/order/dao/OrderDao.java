package com.wang.gulimall.order.dao;

import com.wang.gulimall.order.entity.OrderEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单
 * 
 * @author wanghong
 * @email 2977055047@qq.com
 * @date 2022-06-09 15:23:56
 */
@Mapper
public interface OrderDao extends BaseMapper<OrderEntity> {
	
}
