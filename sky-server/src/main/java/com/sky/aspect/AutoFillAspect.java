package com.sky.aspect;

import com.sky.annotation.AutoFill;
import com.sky.constant.AutoFillConstant;
import com.sky.context.BaseContext;
import com.sky.enumeration.OperationType;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 自定义切面，实现公共字段自动填充处理逻辑
 *
 * @author beach
 */

@Aspect
@Component
@Slf4j
public class AutoFillAspect {
    /**
     * 切入点
     */

    @Pointcut("execution(* com.sky.mapper.*.*(..)) && @annotation(com.sky.annotation.AutoFill) ")
    public void autoFillPointCut() {
    }

    /**
     * 前置通知，在方法执行前进行调用
     * 这个通知指：在方法执行前，为公共字段赋值
     */
    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        log.info("开始进行公共字段填充");
        //1.获取当前被拦截的方法上的数据库操作类型
        //先获取签名，由于签名是Object，这里拦截的是一个方法，所以需要强转成MethodSignature
        //方法签名对象
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        //获取方法上的注解对象
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        //通过注解可以获取数据库操作类型
        OperationType operationType = autoFill.value();
        //2.通过连接点获取方法中的参数（实体对象），因为要为这个实体的公共字段赋值
        Object[] args = joinPoint.getArgs();
        //现在做一个约定，如果想实现自动填充，要保证实体放在第一个位置，因为接下来获取的话，获取第一个即可
        //然后上面是获取了方法中的所有的参数，防止出现空指针，获取参数的数组不为空，并且注意是||不是&&
        if (args == null || args.length == 0) {
            return;
        }
        //然后取得第一个参数，但注意接收类型是 Object，不是Employee，因为这个参数可能是任意类型的参数，以后其他分类的实体都能用object接收,不是写死了
        Object entity = args[0];
        //3.准备赋值的数据，时间通过数据库的函数获取，登录用户id从ThreadLocal中获取
        LocalDateTime now = LocalDateTime.now();
        Long currentId = BaseContext.getCurrentId();

        //4.根据当前不同的操作类型，为对应的字段通过反射来赋值
        if (operationType.equals(OperationType.INSERT)) {
            //用反射为4个公共字段赋值,这样可以不需要分类讨论是 employee 还是其他类型了
            try {
                //然后为了防止方法名写错，定义了常量类
                //通过常量来引用方法名，防止写错且规范，方便后续修改
                Method setCreateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_TIME, LocalDateTime.class);
                Method setCreateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_CREATE_USER, Long.class);
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                // 调用setter方法赋值
                setCreateTime.invoke(entity, now);
                setCreateUser.invoke(entity, currentId);
                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (operationType.equals(OperationType.UPDATE)) {
            //为2个公共字段赋值
            try {
                Method setUpdateTime = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_TIME, LocalDateTime.class);
                Method setUpdateUser = entity.getClass().getDeclaredMethod(AutoFillConstant.SET_UPDATE_USER, Long.class);

                setUpdateTime.invoke(entity, now);
                setUpdateUser.invoke(entity, currentId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

        }


    }
}
