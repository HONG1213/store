package com.wang.gulimall.product.feign;

import com.wang.common.to.SkuReductionTo;
import com.wang.common.to.SpuBoundTo;
import com.wang.common.utils.R;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient("gulimall-coupon")
public interface CouponFeignService {
    /**
     * CouponFeignService.saveSpuBounds(spuBoundTo)
     *      1. @RequestBody 将这个对象转为json
     *      2.找到gulimall-coupon服务，给/coupon/spubounds/save发送请求
     *      3. 对方服务收到请求。请求体里有json数据
     *          (@RequestBody SpuBoundTo spuBoundTo);将请求体里的对象转化为 SpuBoundsEntity。
     *  只要json数据模型是兼容的。双方服务无需使用同一个to
     *
     */
    @PostMapping("/coupon/spubounds/save")
    public R saveSpuBounds(@RequestBody SpuBoundTo spuBoundTo);

    @PostMapping("coupon/skufullreduction/saveinfo")
    R saveSkuReduction(@RequestBody SkuReductionTo skuReductionTo);
}
