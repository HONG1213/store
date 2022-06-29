package com.wang.gulimall.search;

import com.alibaba.fastjson.JSON;
import com.wang.gulimall.search.config.ElasticConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;


@SpringBootTest
public class GulimallSearchApplicationTests {

    @Autowired
    RestHighLevelClient client;

    @Test
    public void contextLoads() throws IOException {
        IndexRequest indexRequest = new IndexRequest("users");
        indexRequest.id("1"); //数据的id
        //indexRequest.source("username","zhangsan","age",18,"gender","男");
        User user = new User("zhangsan", "男", 22);
        String jsonString = JSON.toJSONString(user);
        indexRequest.source(jsonString, XContentType.JSON); //要保存的内容
        //执行操作
        IndexResponse index = client.index(indexRequest, ElasticConfig.COMMON_OPTIONS);
        System.out.println(index);
    }

    @Data
    @AllArgsConstructor
    public class User{
        private String username;
        private String gender;
        private Integer age;
    }

    @Test
    public void searchData() throws IOException {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("users");
        //DSL 条件
        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();
        sourceBuilder.query(QueryBuilders.matchQuery("username","zhangsan"));

        TermsAggregationBuilder tagg = AggregationBuilders.terms("abc").field("age").size(10);
        sourceBuilder.aggregation(tagg);

        searchRequest.source(sourceBuilder);

        //执行检索
        SearchResponse searchResponse = client.search(searchRequest, ElasticConfig.COMMON_OPTIONS);

        //result
        System.out.println(searchResponse.toString());
    }
}
