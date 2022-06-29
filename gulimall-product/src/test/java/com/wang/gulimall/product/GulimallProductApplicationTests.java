package com.wang.gulimall.product;

import com.aliyun.oss.*;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.wang.gulimall.product.entity.BrandEntity;
import com.wang.gulimall.product.service.BrandService;
import com.wang.gulimall.product.service.CategoryService;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Slf4j
@SpringBootTest
class GulimallProductApplicationTests {

    @Autowired
    BrandService brandService;

    @Autowired
    StringRedisTemplate stringRedisTemplate;
//    @Autowired
//    OSSClient ossClient;

    @Autowired
    CategoryService categoryService;

    @Autowired
    RedissonClient redissonClient;

    @Test
    void rediss(){
        System.out.println(redissonClient);
    }

    @Test
    void contextLoads() {
//        BrandEntity brandEntity = new BrandEntity();
//        brandEntity.setName("苹果");
//        brandService.save(brandEntity);
        List<BrandEntity> brand_id = brandService.list(new QueryWrapper<BrandEntity>().eq("brand_id", 1L));
        brand_id.forEach(item->{
            System.out.println(item);
        });
    }

    @Test
    public void testt(){
        ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
        ops.set("aaaa", UUID.randomUUID().toString());
        System.out.println(ops.get("aaaa"));
    }

    @Test
    public void test(){
        // Endpoint以华东1（杭州）为例，其它Region请按实际情况填写。
        String endpoint = "oss-cn-beijing.aliyuncs.com";
        // 阿里云账号AccessKey拥有所有API的访问权限，风险很高。强烈建议您创建并使用RAM用户进行API访问或日常运维，请登录RAM控制台创建RAM用户。
        String accessKeyId = "LTAI5tJ5MY4gcbeTXR3M3Uxq";
        String accessKeySecret = "JsnkzCBHf4eHyU0c2lqUeHMR5RpvXI";
        // 填写Bucket名称，例如examplebucket。
        String bucketName = "gulimall-wanghong";
        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        String objectName = "8bf441260bffa42f.jpg";

        // 如果未指定本地路径，则默认从示例程序所属项目对应本地路径中上传文件流。
        String filePath= "C:\\Users\\wanghong\\Desktop\\docs\\pics\\8bf441260bffa42f.jpg";

        // 创建OSSClient实例。
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);

        try {
            InputStream inputStream = new FileInputStream(filePath);
            // 创建PutObject请求。
            ossClient.putObject(bucketName, objectName, inputStream);
        } catch (OSSException oe) {
            System.out.println("Caught an OSSException, which means your request made it to OSS, "
                    + "but was rejected with an error response for some reason.");
            System.out.println("Error Message:" + oe.getErrorMessage());
            System.out.println("Error Code:" + oe.getErrorCode());
            System.out.println("Request ID:" + oe.getRequestId());
            System.out.println("Host ID:" + oe.getHostId());
        } catch (ClientException ce) {
            System.out.println("Caught an ClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with OSS, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message:" + ce.getMessage());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (ossClient != null) {
                ossClient.shutdown();
            }
        }
    }

    //@Test
    public void test2(){
//        String bucketName = "gulimall-wanghong";
//        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
//        String objectName = "749d8efdff062fb0.jpg";
//        String filePath= "C:\\Users\\wanghong\\Desktop\\docs\\pics\\749d8efdff062fb0.jpg";
//        InputStream inputStream = null;
//        try {
//            inputStream = new FileInputStream(filePath);
//            ossClient.putObject(bucketName, objectName, inputStream);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    @Test
    void findtest(){
        Long[] catelogPath = categoryService.findCatelogPath(225L);
            log.info("---------->{}", Arrays.asList(catelogPath));
    }

}
