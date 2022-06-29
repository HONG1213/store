package com.wang.gulimall.product.exception;

import com.wang.common.exception.BizCodeEnume;
import com.wang.common.utils.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
//@ControllerAdvice(basePackages = "com.wang.gulimall.product.controller")
//等于@RestController + @ControllerAdvice
@RestControllerAdvice(basePackages = "com.wang.gulimall.product.controller")
public class GulimallExceptionControllerAdvice {

    //@ExceptionHandler(value = Exception.class)
    @ExceptionHandler(value = MethodArgumentNotValidException.class) //更精确一点
    public R handlerVaildException(MethodArgumentNotValidException e){
        log.error("数据校验出错{}, 异常类型{}",e.getMessage(),e.getClass());
        BindingResult result = e.getBindingResult();
        Map<String, String> errorMap = new HashMap<>();
        result.getFieldErrors().forEach((error)->{
            errorMap.put(error.getField(),error.getDefaultMessage());
        });
        return R.error(BizCodeEnume.VAILD_EXCEPTION.getCode(),BizCodeEnume.VAILD_EXCEPTION.getMsg()).put("data",errorMap);
    }

    //其他异常
    @ExceptionHandler(value = Throwable.class)
    public R handleException(Throwable e){
        return R.error(BizCodeEnume.UNKNOW_EXCEPTION.getCode(),BizCodeEnume.UNKNOW_EXCEPTION.getMsg());
    }
}
