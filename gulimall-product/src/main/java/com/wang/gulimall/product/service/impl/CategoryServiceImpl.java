package com.wang.gulimall.product.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.wang.gulimall.product.service.CategoryBrandRelationService;
import com.wang.gulimall.product.vo.Catelog2Vo;
import org.apache.commons.lang.StringUtils;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.wang.common.utils.PageUtils;
import com.wang.common.utils.Query;

import com.wang.gulimall.product.dao.CategoryDao;
import com.wang.gulimall.product.entity.CategoryEntity;
import com.wang.gulimall.product.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;


@Service("categoryService")
public class CategoryServiceImpl extends ServiceImpl<CategoryDao, CategoryEntity> implements CategoryService {

//    @Autowired
//    CategoryDao categoryDao;

    Map<String,Object> cache=new HashMap<>();

    @Autowired
    CategoryBrandRelationService categoryBrandRelationService;
    @Autowired
    StringRedisTemplate stringRedisTemplate;
    @Autowired
    RedissonClient redisson;


    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<CategoryEntity> page = this.page(
                new Query<CategoryEntity>().getPage(params),
                new QueryWrapper<CategoryEntity>()
        );

        return new PageUtils(page);
    }

    @Override
    public List<CategoryEntity> listWithTree() {
        //categoryDao
        List<CategoryEntity> list = baseMapper.selectList(null);

        //找到所有一级分类
        List<CategoryEntity> collect = list.stream().filter((categoryEntity) ->
            categoryEntity.getParentCid() == 0
        ).map((menu)->{
            menu.setChildren(getChildrenList(menu,list));
            return menu;
        }).sorted((menu1,menu2)->{
                return (menu1.getSort()==null?0:menu1.getSort())-(menu2.getSort()==null?0:menu2.getSort());
            }
        ).collect(Collectors.toList());

        return collect;
    }

    public List<CategoryEntity> getChildrenList(CategoryEntity root, List<CategoryEntity> all) {
        List<CategoryEntity> list = all.stream().
                filter((categoryEntity) ->
                        //TODO 这这5种包装类默认创建了数值[-128，127]的相应类型的缓存数据，但是超出此范围仍然会去创建新的对象。
                        // 在范围之内，这个时候就存放在缓存中，当再创建a2时，
                        // java发现缓存中存在127这个数了，就直接取出来赋值给a2，
                        // 所以a1 == a2的。当超过范围就是来new一个对象了，
                        // 所以它们不等
                        // 所以使用equals
                        root.getCatId().equals(categoryEntity.getParentCid())
                ).map(categoryEntity -> {
                categoryEntity.setChildren(getChildrenList(categoryEntity,all));
                return categoryEntity;
            }).sorted((menu1,menu2)->{
                return (menu1.getSort()==null?0:menu1.getSort())-(menu2.getSort()==null?0:menu2.getSort());
            }
        ).collect(Collectors.toList());

        return list;
    }

    @Override
    public void removeMenuByIds(List<Long> asList) {
        //TODO 1.检查当前删除的菜单，是否被引用
        //逻辑删除
        baseMapper.deleteBatchIds(asList);
    }

    //[2,34,225]
    @Override
    public Long[] findCatelogPath(Long catelogId) {
        List<Long> paths=new ArrayList();
        List<Long> parentpaths=findParentPath(catelogId,paths);
        Collections.reverse(parentpaths);
        return (Long[]) parentpaths.toArray(new Long[parentpaths.size()]);
    }

    //级联更新所有关联的数据
    @Transactional
    @Override
    public void updateCascade(CategoryEntity category) {
        this.updateById(category);
        categoryBrandRelationService.updateCategory(category.getCatId(),category.getName());

        //同时修改缓存数据

    }

    @Override
    public List<CategoryEntity> getLevel1Categorys() {
        List<CategoryEntity> parent_cid = baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", 0));
        return parent_cid;
    }

    //TODO
    @Override
    public Map<String, List<Catelog2Vo>> getCatalogJson() {

        //1.加入缓存逻辑，缓存中的是json数据 JSON跨平台
        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
        if(StringUtils.isEmpty(catalogJson)){
            //2.缓存中没有，查询数据库
            Map<String, List<Catelog2Vo>> fromDb = getCatalogJsonFromDbWithRedissonLock();
            return fromDb;
        }
        //json转对象
        Map<String, List<Catelog2Vo>> result=JSON.parseObject(catalogJson,new TypeReference<Map<String, List<Catelog2Vo>>>(){});
        return result;
    }


    //如何保证缓存与数据库 数据一致性
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedissonLock() {
        //锁名字。锁的粒度，越细越快
        RLock lock = redisson.getLock("CatalogJson-lock");
        lock.lock();
        Map<String, List<Catelog2Vo>> dataFromDb = null;
        try{
            dataFromDb = getDataFromDb();
        }finally {
            lock.unlock();
        }
            return dataFromDb;
    }

    //从数据库查询并封装分类数据  使用分布式锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithRedisLock() {
        //1.占分布式锁，去redis占坑 setnx
        String uuid = UUID.randomUUID().toString();
        //加时间防止死锁 原子性
        Boolean lock = stringRedisTemplate.opsForValue().setIfAbsent("lock", uuid,300,TimeUnit.SECONDS);
        if(lock){
            System.out.println("获取分布式锁成功");
            Map<String, List<Catelog2Vo>> dataFromDb = null;
            try{
                dataFromDb = getDataFromDb();
            }finally {
                //如果a删锁前，锁失效，b占锁,a执行删锁时，b的也让删了
                //String lock1 = stringRedisTemplate.opsForValue().get("lock"); a在redis返回的过程中，虽然是自己的锁，b占锁时，a删除b也被删除了
                //if(uuid.equals(lock1)) stringRedisTemplate.delete("lock");
                //使用Lua脚本 获取值对比+成功删除 原子性

                //stringRedisTemplate.delete("lock");
                String script="if redis.call('get',KEY[1]) == ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";
                //删除锁
                stringRedisTemplate.execute(new DefaultRedisScript<Long>(script,Long.class),Arrays.asList("lock"),uuid); //?  ?
            }
            return dataFromDb;
        }else{
            //加锁失败。。。重试
            System.out.println("获取分布式锁失败。。。重试");
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return getCatalogJsonFromDbWithRedisLock();  //自旋的方式
        }
    }


    //从数据库查询并封装分类数据 使用本地锁
    public Map<String, List<Catelog2Vo>> getCatalogJsonFromDbWithLocalLock() {
        synchronized (this) {
            //拿上锁，先去缓存确定一下，没有才查询
            String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
            if(StringUtils.isNotEmpty(catalogJson)){
                //json转对象
                Map<String, List<Catelog2Vo>> result=JSON.parseObject(catalogJson,new TypeReference<Map<String, List<Catelog2Vo>>>(){});
                return result;
            }
            /**
             * 1.将数据库的多次查询变为一次
             */
            List<CategoryEntity> selectList = baseMapper.selectList(null);
            //查1级分类
            List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);

            Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
                //查当前1级分类的所有二级分类
                List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
                //封装上面的结果
                List<Catelog2Vo> catelog2Vos = null;
                if (categoryEntities != null) {
                    catelog2Vos = categoryEntities.stream().map((l2) -> {
                        Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                        //找到当前二级分类的三级分类封装成vo
                        List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                        if (level3Catelog != null) {
                            List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map((l3) -> {
                                Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                                return catelog3Vo;
                            }).collect(Collectors.toList());
                            catelog2Vo.setCatalog3List(collect);
                        }
                        return catelog2Vo;
                    }).collect(Collectors.toList());
                }
                return catelog2Vos;
            }));
            //3.查到的数据再放入缓存,将对象转化为json放在缓存中
            String jsonString = JSON.toJSONString(parent_cid);
            stringRedisTemplate.opsForValue().set("catalogJson",jsonString,1, TimeUnit.DAYS);
            return parent_cid;
        }
    }

    public Map<String, List<Catelog2Vo>>  getDataFromDb(){
        //拿上锁，先去缓存确定一下，没有才查询
        String catalogJson = stringRedisTemplate.opsForValue().get("catalogJson");
        if(StringUtils.isNotEmpty(catalogJson)){
            //json转对象
            Map<String, List<Catelog2Vo>> result=JSON.parseObject(catalogJson,new TypeReference<Map<String, List<Catelog2Vo>>>(){});
            return result;
        }
        //1.将数据库的多次查询变为一次
        List<CategoryEntity> selectList = baseMapper.selectList(null);
        //查1级分类
        List<CategoryEntity> level1Categorys = getParent_cid(selectList, 0L);
        Map<String, List<Catelog2Vo>> parent_cid = level1Categorys.stream().collect(Collectors.toMap(k -> k.getCatId().toString(), v -> {
            //查当前1级分类的所有二级分类
            List<CategoryEntity> categoryEntities = getParent_cid(selectList, v.getCatId());
            //封装上面的结果
            List<Catelog2Vo> catelog2Vos = null;
            if (categoryEntities != null) {
                catelog2Vos = categoryEntities.stream().map((l2) -> {
                    Catelog2Vo catelog2Vo = new Catelog2Vo(v.getCatId().toString(), null, l2.getCatId().toString(), l2.getName());
                    //找到当前二级分类的三级分类封装成vo
                    List<CategoryEntity> level3Catelog = getParent_cid(selectList, l2.getCatId());
                    if (level3Catelog != null) {
                        List<Catelog2Vo.Catelog3Vo> collect = level3Catelog.stream().map((l3) -> {
                            Catelog2Vo.Catelog3Vo catelog3Vo = new Catelog2Vo.Catelog3Vo(l2.getCatId().toString(), l3.getCatId().toString(), l3.getName());
                            return catelog3Vo;
                        }).collect(Collectors.toList());
                        catelog2Vo.setCatalog3List(collect);
                    }
                    return catelog2Vo;
                }).collect(Collectors.toList());
            }
            return catelog2Vos;
        }));
        //3.查到的数据再放入缓存,将对象转化为json放在缓存中
        String jsonString = JSON.toJSONString(parent_cid);
        stringRedisTemplate.opsForValue().set("catalogJson",jsonString,1, TimeUnit.DAYS);
        return parent_cid;
    }

    private List<CategoryEntity> getParent_cid(List<CategoryEntity> selectList,Long parent_cid) {
        List<CategoryEntity> collect = selectList.stream().filter((item) -> {
            return item.getParentCid().equals(parent_cid);
        }).collect(Collectors.toList());
        //return baseMapper.selectList(new QueryWrapper<CategoryEntity>().eq("parent_cid", v.getCatId()));
        return collect;
    }

    //225,34,2
    private List<Long> findParentPath(Long catelogId,List<Long> paths){
        //1,.收集当前节点id
        paths.add(catelogId);
        CategoryEntity byId = this.getById(catelogId);
        if(byId.getParentCid()!=0){
            findParentPath(byId.getParentCid(),paths);
        }
        return paths;
    }

}