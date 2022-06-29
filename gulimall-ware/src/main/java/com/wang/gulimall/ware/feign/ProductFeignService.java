package com.wang.gulimall.ware.feign;

import com.wang.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@FeignClient("gulimall-product")
public interface ProductFeignService {

    /**
     * 让所有请求过网关
     *      1.@FeignClient("gulimall-gateway")；给网关机器发请求
     *      2.api/product/skuinfo/{skuId}
     * 直接让后台指定服务处理
     *      1.@FeignClient("gulimall-product")；给网关机器发请求
     *      2./product/skuinfo/{skuId}
     * @param skuId
     * @return
     */
    @RequestMapping("/product/skuinfo/info/{skuId}")
    public R info(@PathVariable("skuId") Long skuId);
}
