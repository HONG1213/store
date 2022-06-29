package com.wang.gulimall.product.service.impl;

import com.alibaba.fastjson.TypeReference;
import com.wang.common.constant.ProductConstant;
import com.wang.common.to.SkuHasStockVo;
import com.wang.common.to.SkuReductionTo;
import com.wang.common.to.SpuBoundTo;
import com.wang.common.to.es.SkuEsModel;
import com.wang.common.utils.R;
import com.wang.gulimall.product.entity.*;
import com.wang.gulimall.product.feign.CouponFeignService;
import com.wang.gulimall.product.feign.SearchFeignService;
import com.wang.gulimall.product.feign.WareFeignService;
import com.wang.gulimall.product.service.*;
import com.wang.gulimall.product.vo.*;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wang.common.utils.PageUtils;
import com.wang.common.utils.Query;

import com.wang.gulimall.product.dao.SpuInfoDao;
import org.springframework.transaction.annotation.Transactional;


@Service("spuInfoService")
public class SpuInfoServiceImpl extends ServiceImpl<SpuInfoDao, SpuInfoEntity> implements SpuInfoService {

    @Autowired
    SpuInfoDescService spuInfoDescService;
    @Autowired
    SpuImagesService spuImagesService;
    @Autowired
    AttrService attrService;
    @Autowired
    ProductAttrValueService productAttrValueService;
    @Autowired
    SkuInfoService skuInfoService;
    @Autowired
    SkuImagesService skuImagesService;
    @Autowired
    SkuSaleAttrValueService skuSaleAttrValueService;
    @Autowired
    CouponFeignService couponFeignService;
    @Autowired
    BrandService brandService;
    @Autowired
    CategoryService categoryService;
    @Autowired
    WareFeignService wareFeignService;
    @Autowired
    SearchFeignService searchFeignService;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<SpuInfoEntity> page = this.page(
                new Query<SpuInfoEntity>().getPage(params),
                new QueryWrapper<SpuInfoEntity>()
        );
        return new PageUtils(page);
    }

    @Transactional
    @Override
    public void saveSpuInfo(SpuSaveVo vo) {
        //1.保存spu基本信息 pms_spu_info
        SpuInfoEntity infoEntity = new SpuInfoEntity();
        BeanUtils.copyProperties(vo,infoEntity);
        infoEntity.setCreateTime(new Date());
        infoEntity.setUpdateTime(new Date());
        this.saveBaseSpuInfo(infoEntity);

        //2.保存spu的描述图片 pms_spu_info_desc
        List<String> decript = vo.getDecript();
        SpuInfoDescEntity descEntity = new SpuInfoDescEntity();
        descEntity.setSpuId(infoEntity.getId());
        descEntity.setDecript(String.join(",",decript));
        spuInfoDescService.saveSpuInfoDesc(descEntity);


        //3.保存spu的图片集 pms_spu_images
        List<String> images = vo.getImages();
        spuImagesService.saveImages(infoEntity.getId(),images);


        //4.保存spu的规格参数 pms_product_attr_value
        List<BaseAttrs> list = vo.getBaseAttrs();
        List<ProductAttrValueEntity> collect = list.stream().map((attr) -> {
            ProductAttrValueEntity entity = new ProductAttrValueEntity();
            entity.setSpuId(infoEntity.getId());
            entity.setAttrId(attr.getAttrId());
            AttrEntity byId = attrService.getById(attr.getAttrId());
            entity.setAttrName(byId.getAttrName());
            entity.setAttrValue(attr.getAttrValues());
            entity.setQuickShow(attr.getShowDesc());

            return entity;
        }).collect(Collectors.toList());
        productAttrValueService.saveProductAttr(collect);


        //5. 保存spu的积分信息：gulimall_sms--sms_spu_bounds
        Bounds bounds = vo.getBounds();
        SpuBoundTo spuBoundTo = new SpuBoundTo();
        BeanUtils.copyProperties(bounds,spuBoundTo);
        spuBoundTo.setSpuId(infoEntity.getId());
        R r = couponFeignService.saveSpuBounds(spuBoundTo);
        if(r.getCode()!=0) log.error("远程保存sku积分信息失败");

        //5.保存当前spu对应的sku信息
        //5.1 sku的基本信息；pms_sku_info
        List<Skus> skus = vo.getSkus();
        if(skus.size()>0 && skus!=null){
             skus.forEach((item)->{
                 String defaultImg="";
                 for (Images image : item.getImages()) {
                     if(image.getDefaultImg() == 1){
                         defaultImg=image.getImgUrl();
                     }
                 }

                 //5.1 保存当前spu对应的sku信息
                 SkuInfoEntity skuInfoEntity = new SkuInfoEntity();
                 BeanUtils.copyProperties(item,skuInfoEntity);
                 skuInfoEntity.setBrandId(infoEntity.getBrandId());
                 skuInfoEntity.setCatalogId(infoEntity.getCatalogId());
                 skuInfoEntity.setSaleCount(0L);
                 skuInfoEntity.setSpuId(infoEntity.getId());
                 skuInfoEntity.setSkuDefaultImg(defaultImg);
                 skuInfoService.saveSkuInfo(skuInfoEntity);
                 //sku保存完，自增 id 就出来了
                 Long skuId = skuInfoEntity.getSkuId();
                 List<SkuImagesEntity> imagesEntitys = item.getImages().stream().map((img) -> {
                     SkuImagesEntity skuImagesEntity = new SkuImagesEntity();
                     skuImagesEntity.setSkuId(skuId);
                     skuImagesEntity.setImgUrl(img.getImgUrl());
                     skuImagesEntity.setDefaultImg(img.getDefaultImg());
                     return skuImagesEntity;
                 }).filter(en->{
                     return StringUtils.isNotEmpty(en.getImgUrl());
                 }).collect(Collectors.toList());
                 //5.2 sku的图片信息：pms_spu_images
                 skuImagesService.saveBatch(imagesEntitys);
                 //TODO 没有图片路径的无需保存

                 //5.3 sku的销售属性信息：pms_sku_sale_attr_value
                 List<Attr> attr = item.getAttr();
                 List<SkuSaleAttrValueEntity> collect1 = attr.stream().map((a) -> {
                     SkuSaleAttrValueEntity attrValueEntity = new SkuSaleAttrValueEntity();
                     BeanUtils.copyProperties(a, attrValueEntity);
                     attrValueEntity.setSkuId(skuId);
                     return attrValueEntity;
                 }).collect(Collectors.toList());
                 skuSaleAttrValueService.saveBatch(collect1);

                 //5.4 sku的优惠满减信息 gulimall_sms --> sms_sku_ladder \ sms_sku_full_reduction \ sms_member_price
                 SkuReductionTo skuReductionTo = new SkuReductionTo();
                 BeanUtils.copyProperties(item,skuReductionTo);
                 skuReductionTo.setSkuId(skuId);
                 if(skuReductionTo.getFullCount()>0 || skuReductionTo.getFullPrice().compareTo(new BigDecimal("0"))==1){
                     R r1 = couponFeignService.saveSkuReduction(skuReductionTo);
                     if(r1.getCode()!=0) log.error("远程保存sku优惠信息失败");
                 }
             });
        }


    }


    @Override
    public void saveBaseSpuInfo(SpuInfoEntity infoEntity) {
        this.baseMapper.insert(infoEntity);
    }

    @Override
    public PageUtils queryPageByCondition(Map<String, Object> params) {
        QueryWrapper<SpuInfoEntity> wrapper = new QueryWrapper<>();
        String key = (String) params.get("key");
        if(StringUtils.isNotEmpty(key)){
            wrapper.and((w)->{
                w.eq("id",key).or().like("spu_name",key);
            });
        }
        String status = (String) params.get("status");
        if(StringUtils.isNotEmpty(status)){
            wrapper.eq("publish_status",status);
        }
        String brandId = (String) params.get("brandId");
        if(StringUtils.isNotEmpty(brandId) && !"0".equalsIgnoreCase(brandId)){
            wrapper.eq("brand_id",brandId);
        }
        String catelogId = (String) params.get("catelogId");
        if(StringUtils.isNotEmpty(catelogId) && !"0".equalsIgnoreCase(catelogId)){
            wrapper.eq("catalog_id",catelogId);
        }
        IPage<SpuInfoEntity> page = this.page(new Query<SpuInfoEntity>().getPage(params),wrapper);
        return new PageUtils(page);
    }

    //上架
    @Override
    public void up(Long spuId) {
        //查出当前spuid对应的所有sku信息,品牌名字
        List<SkuInfoEntity> skus=skuInfoService.getSkuBySpuId(spuId);
        List<Long> skuIdList = skus.stream().map((item) -> {
            return item.getSkuId();
        }).collect(Collectors.toList());

        //TODO 4.查询sku可以被检索的规格属性
        List<ProductAttrValueEntity> baseAttrs = productAttrValueService.baseAttrlistforspu(spuId);
        //找到productAttr的所有attr_id
        List<Long> attrIds = baseAttrs.stream().map((attr) -> {
            return attr.getAttrId();
        }).collect(Collectors.toList());
        //通过属性表找出可以检索的attrId
        List<Long> searchAttrIds=attrService.selectSearchAttrs(attrIds);
        Set<Long> idSet=new HashSet<>(searchAttrIds);

        //productAttr 过滤掉不能检索的，并且封装成 SkuEsModel.Attrs
        List<SkuEsModel.Attrs> collect = baseAttrs.stream().filter((item) -> {
            return idSet.contains(item.getAttrId());
        }).map((item) -> {
            SkuEsModel.Attrs attrs1 = new SkuEsModel.Attrs();
            BeanUtils.copyProperties(item, attrs1);
            return attrs1;
        }).collect(Collectors.toList());

        //设置库存
        //TODO 1.远程调用，是否有库存
        Map<Long, Boolean> stockMap=null;
        try{
            R r = wareFeignService.getSkusHasStock(skuIdList);
            //TODO **********************
            TypeReference<List<SkuHasStockVo>> typeReference = new TypeReference<List<SkuHasStockVo>>() {};
            stockMap = r.getData(typeReference)
                    .stream().collect(Collectors.toMap(SkuHasStockVo::getSkuId, item -> item.getHasStock()));
        }catch (Exception e){
            log.error("上架时库存服务查询异常  ：{}",e);
        }

        //封装每个sku信息
        Map<Long, Boolean> finalStockMap = stockMap;
        List<SkuEsModel> list = skus.stream().map((item) -> {
            SkuEsModel esModel = new SkuEsModel();
            BeanUtils.copyProperties(item,esModel);
            //skuPrice skuImg
            esModel.setSkuPrice(item.getPrice());
            esModel.setSkuImg(item.getSkuDefaultImg());
            //hasStock hotScore brandName; brangImg; catalogName
            if(finalStockMap ==null){
                esModel.setHasStock(true);
            }else {
                esModel.setHasStock(finalStockMap.get(item.getSkuId()));
            }
            //TODO 2.热度评分
            esModel.setHotScore(0L);
            //TODO 3.查询品牌和分类的名字信息
            BrandEntity byId = brandService.getById(esModel.getBrandId());
            esModel.setBrandName(byId.getName());
            esModel.setBrangImg(byId.getLogo());
            CategoryEntity categoryEntity = categoryService.getById(esModel.getCatalogId());
            esModel.setCatalogName(categoryEntity.getName());
            //设置属性信息
            esModel.setAttrs(collect);
            return esModel;
        }).collect(Collectors.toList());

        //TODO 5.将数据发送给es进行保存，gulimall-search
        R r = searchFeignService.productStatusUp(list);
        if(r.getCode()==0){
            //远程调用成功
            System.out.println("---------------------------");
            //TODO 6. 修改spu的状态
            this.baseMapper.updateSpuStatus(spuId, ProductConstant.SpuStatusEnum.SPU_UP.getCode());
        }else {
            //远程调用失败
            //TODO 7. 重复调用？接口幂等性：重试机制？
        }
    }
}