package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDateTime;

//自定义切面，实现公共字段自动填充逻辑
@Aspect
@Component
@Slf4j
public class AutoFillAspect {

    //定义切入点，拦截mapper里，被标记了@AutoFill的方法
    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill)")
    public void autoFillPointCut(){}

    //使用切入点，前置通知，在方法执行前做公共字段的填充
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        log.info("开始进行公共字段自动填充...");
        //获取value上的数据库操作类型
        MethodSignature signature =(MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        AutoFill autoFill = method.getAnnotation(AutoFill.class);   //拿方法上的AutoFill注解
        OperationType operationType = autoFill.value();

        //获取方法的对象参数，为它写入update_user等信息
        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            return;
        }

        Object entity = args[0];
        LocalDateTime now = LocalDateTime.now();
        Long id = BaseContext.getCurrentId();

        if(OperationType.INSERT == operationType){
            //获取实体参数所属的类 的指定名字的方法（可以用常量代替更规范）   这个方法的参数类型是这样的
            Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
            Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
            Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
            Method setUpdateUser = entity.getClass().getDeclaredMethod("setUpdateUser", Long.class);

            //调用通过反射获取到的方法，要指定主体、参数
            setCreateTime.invoke(entity,now);
            setCreateUser.invoke(entity, id);
            setUpdateTime.invoke(entity,now);
            setUpdateUser.invoke(entity,id);
        }
        else  if(OperationType.UPDATE == operationType){
            Method setUpdateTime = entity.getClass().getDeclaredMethod("setUpdateTime", LocalDateTime.class);
            Method setUpdateUser = entity.getClass().getDeclaredMethod("setUpdateUser", Long.class);

            setUpdateTime.invoke(entity,now);
            setUpdateUser.invoke(entity,id);
        }



    }


}
