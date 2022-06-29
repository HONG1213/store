package com.wang.gulimall.product.app;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.wang.gulimall.product.entity.ProductAttrValueEntity;
import com.wang.gulimall.product.service.ProductAttrValueService;
import com.wang.gulimall.product.vo.AttrRespVo;
import com.wang.gulimall.product.vo.AttrVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.wang.gulimall.product.service.AttrService;
import com.wang.common.utils.PageUtils;
import com.wang.common.utils.R;



/**
 * 商品属性
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2022-06-09 14:48:11
 */
@RestController
@RequestMapping("product/attr")
public class AttrController {
    @Autowired
    private AttrService attrService;

    @Autowired
    ProductAttrValueService productAttrValueService;


    //参数回显
    @RequestMapping("/base/listforspu/{spuId}")
    public R baseAttrlistforspu(@PathVariable("spuId") Long spuId){
        List<ProductAttrValueEntity> list=productAttrValueService.baseAttrlistforspu(spuId);

        return R.ok().put("data", list);
    }

    @RequestMapping("/sale/list/{catelog}")
    public R saleAttrlist(@RequestParam Map<String, Object> params,@PathVariable("catelog") Long catelog){
        PageUtils page = attrService.querySaleAttrPage(params,catelog);

        return R.ok().put("page", page);
    }

    @RequestMapping("/base/list/{catelog}")
    public R baseAttrlist(@RequestParam Map<String, Object> params,@PathVariable("catelog") Long catelog){
        PageUtils page = attrService.queryBaseAttrPage(params,catelog);

        return R.ok().put("page", page);
    }

    /**
     * 列表
     */
    @RequestMapping("/list")
    //@RequiresPermissions("product:attr:list")
    public R list(@RequestParam Map<String, Object> params){
        PageUtils page = attrService.queryPage(params);

        return R.ok().put("page", page);
    }


    /**
     * 信息
     */
    @RequestMapping("/info/{attrId}")
    //@RequiresPermissions("product:attr:info")
    public R info(@PathVariable("attrId") Long attrId){
		//AttrEntity attr = attrService.getById(attrId);
		AttrRespVo attr = attrService.getAttrInfo(attrId);

        return R.ok().put("attr", attr);
    }

    /**
     * 保存
     */
    @RequestMapping("/save")
    //@RequiresPermissions("product:attr:save")
    public R save(@RequestBody AttrVo attr){
		attrService.saveAttr(attr);
        return R.ok();
    }

    /**
     * 修改
     */
    @RequestMapping("/update")
    //@RequiresPermissions("product:attr:update")
    public R update(@RequestBody AttrVo attr){
		//attrService.updateById(attr);
		attrService.updateAttr(attr);

        return R.ok();
    }

    //更新规格维护的参数
    @RequestMapping("/update/{spuId}")
    public R updateSpuAddr(@PathVariable("spuId") Long spuId,
                          @RequestBody List<ProductAttrValueEntity> entities){
        productAttrValueService.updateSpuAttr(spuId,entities);

        return R.ok();
    }

    /**
     * 删除
     */
    @RequestMapping("/delete")
    //@RequiresPermissions("product:attr:delete")
    public R delete(@RequestBody Long[] attrIds){
		attrService.removeByIds(Arrays.asList(attrIds));

        return R.ok();
    }

}
