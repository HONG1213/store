package com.wang.gulimall.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
//2级分类vo
public class Catelog2Vo {
    private String catalog1Id; //1级父分类id
    private List<Catelog3Vo> catalog3List; //三级子分类id
    private String id;
    private String name;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    //三级分类vo
    public static class Catelog3Vo{
        private String catalog2Id;  //父分类,2级分类id
        private String id;
        private String name;
    }
}
