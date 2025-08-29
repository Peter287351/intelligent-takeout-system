package com.sky.controller.user;

import com.sky.result.Result;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.*;

@RestController("userShopController")
@RequestMapping("/user/shop")
@Slf4j
@Api(tags = "用户模块")
public class ShopController {
    public static final String key = "SHOP_STATUS";
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取店铺营业状态
     *
     * @return
     */
    @ApiOperation("获取店铺营业状态")
    @GetMapping("/status")
    public Result<Integer> getStatus() {
        try {
            Object statusObj = redisTemplate.opsForValue().get(key);
            Integer status = null;

            // 安全地处理从Redis获取的值
            if (statusObj instanceof Integer) {
                status = (Integer) statusObj;
            } else if (statusObj instanceof String) {
                try {
                    status = Integer.parseInt((String) statusObj);
                } catch (NumberFormatException e) {
                    log.warn("Redis中的状态值不是有效整数: {}", statusObj);
                }
            }

            // 如果status仍然为null，设置默认值
            if (status == null) {
                status = 1; // 默认为打烊状态
                log.info("获取店铺的营业状态为:营业中(默认状态)");
            } else {
                log.info("获取店铺的营业状态为:{}", status == 1 ? "营业中" : "打烊中");
            }

            return Result.success(status);
        } catch (Exception e) {
            log.error("获取店铺营业状态时发生异常: ", e);
            // 发生异常时返回默认状态
            return Result.success(1);
        }
    /*public Result<Integer> getStatus() {
        Integer status = (Integer) redisTemplate.opsForValue().get(key);
        log.info("获取店铺的营业状态为:{}",status ==1 ? "营业中" : "打烊中");
        return Result.success(status);
    }*/
    }
}
