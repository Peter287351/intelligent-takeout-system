package com.sky.controller.user;

import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.result.Result;
import com.sky.service.DishService;
import com.sky.utils.RedisUtil;
import com.sky.vo.DishVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@RestController("userDishController")
@RequestMapping("/user/dish")
@Slf4j
@Api(tags = "C端-菜品浏览接口")
public class DishController {
    @Autowired
    private DishService dishService;

    @Autowired
    private RedisUtil redisUtil;

    private static final long CACHE_TTL = 30;
    private static final TimeUnit CACHE_TTL_UNIT = TimeUnit.MINUTES;
    private static final long NULL_TTL = 5;
    private static final TimeUnit NULL_TTL_UNIT = TimeUnit.MINUTES;

    /**
     * 根据分类id查询菜品
     *
     * @param categoryId
     * @return
     */
    @GetMapping("/list")
    @ApiOperation("根据分类id查询菜品")
    public Result<List<DishVO>> list(Long categoryId) {
        String key = "dish_" + categoryId;

        // 查询Redis缓存（空列表也算命中，防缓存穿透）
        List<DishVO> list = redisUtil.getList(key, DishVO.class);
        if (list != null) {
            return Result.success(list);
        }

        Dish dish = new Dish();
        dish.setCategoryId(categoryId);
        dish.setStatus(StatusConstant.ENABLE);
        list = dishService.listWithFlavor(dish);

        if (list == null || list.isEmpty()) {
            // 空值缓存，TTL较短，防止不存在的分类反复穿透DB
            redisUtil.set(key, new ArrayList<>(), NULL_TTL, NULL_TTL_UNIT);
        } else {
            redisUtil.set(key, list, CACHE_TTL, CACHE_TTL_UNIT);
        }

        return Result.success(list);
    }

}
