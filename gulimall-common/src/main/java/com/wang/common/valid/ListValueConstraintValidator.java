package com.wang.common.valid;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.HashSet;
import java.util.Set;

public class ListValueConstraintValidator implements ConstraintValidator<ZiDingYi,Integer> {

    private Set<Integer> set=new HashSet<>();
    //初始化
    @Override
    public void initialize(ZiDingYi constraintAnnotation) {
        //获取自定义注解详细信息
        int[] vals = constraintAnnotation.vals();
        for (int val : vals) {
            set.add(val);
        }
    }

    //判断是否校验成功
    @Override
    public boolean isValid(Integer value, ConstraintValidatorContext context) {
        //校验属性值
        return set.contains(value);
    }
}
