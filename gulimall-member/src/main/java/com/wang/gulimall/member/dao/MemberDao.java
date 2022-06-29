package com.wang.gulimall.member.dao;

import com.wang.gulimall.member.entity.MemberEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 会员
 * 
 * @author wanghong
 * @email 2977055047@qq.com
 * @date 2022-06-09 15:12:08
 */
@Mapper
public interface MemberDao extends BaseMapper<MemberEntity> {
	
}
