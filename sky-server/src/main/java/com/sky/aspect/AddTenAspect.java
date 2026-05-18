package com.sky.aspect;


import com.sky.annotation.AddTen;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Aspect
@Component
public class AddTenAspect {

    @Pointcut("@annotation(com.sky.annotation.AddTen)")
    public void addTenPointCut(){}

    @Before("addTenPointCut()")
    public void addTen(JoinPoint joinPoint) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {

        MethodSignature signature =(MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        AddTen addTen = (AddTen)method.getAnnotation(AddTen.class);
        int howmany = addTen.howMany();

        Object[] args = joinPoint.getArgs();
        if(args == null || args.length == 0){
            return;
        }

        Object entity = args[0];

        Method setNum = entity.getClass().getDeclaredMethod("setNum",Integer.class);
        Method getNum = entity.getClass().getDeclaredMethod("getNum");

        Integer numVal = (Integer) getNum.invoke(entity);
        setNum.invoke(entity,numVal+10*howmany);

    }

}
