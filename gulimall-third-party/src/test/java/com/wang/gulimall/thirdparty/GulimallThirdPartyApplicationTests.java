package com.wang.gulimall.thirdparty;

import com.aliyun.oss.OSSClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

@SpringBootTest
class GulimallThirdPartyApplicationTests {

    @Autowired
    OSSClient ossClient;

    @Test
    void contextLoads() {
        String bucketName = "gulimall-wanghong";
        // 填写Object完整路径，完整路径中不能包含Bucket名称，例如exampledir/exampleobject.txt。
        String objectName = "e07b540657023162.jpg";
        String filePath= "C:\\Users\\wanghong\\Desktop\\docs\\pics\\e07b540657023162.jpg";
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(filePath);
            ossClient.putObject(bucketName, objectName, inputStream);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

}
