package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.DishFlavorMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DishServiceImplTest {

    @Mock
    private DishMapper dishMapper;
    @Mock
    private DishFlavorMapper dishFlavorMapper;
    @Mock
    private SetMealDishMapper setMealDishMapper;
    @Mock
    private com.sky.mapper.SetmealMapper setmealMapper;

    @InjectMocks
    private DishServiceImpl dishService;

    @Test
    void deleteBatch_起售中菜品不能删除() {
        List<Long> ids = Arrays.asList(1L);
        Dish dish = new Dish();
        dish.setId(1L);
        dish.setStatus(StatusConstant.ENABLE);
        when(dishMapper.getById(1L)).thenReturn(dish);

        assertThatThrownBy(() -> dishService.deleteBatch(ids))
                .isInstanceOf(DeletionNotAllowedException.class)
                .hasMessage(MessageConstant.DISH_ON_SALE);
        verify(dishMapper, never()).deleteByIDs(ids);
    }

    @Test
    void deleteBatch_关联套餐的菜品不能删除() {
        List<Long> ids = Arrays.asList(1L);
        Dish dish = new Dish();
        dish.setId(1L);
        dish.setStatus(StatusConstant.DISABLE);
        when(dishMapper.getById(1L)).thenReturn(dish);
        when(setMealDishMapper.getSetMealIdsByDishIds(ids)).thenReturn(Arrays.asList(100L));

        assertThatThrownBy(() -> dishService.deleteBatch(ids))
                .isInstanceOf(DeletionNotAllowedException.class)
                .hasMessage(MessageConstant.DISH_BE_RELATED_BY_SETMEAL);
        verify(dishMapper, never()).deleteByIDs(ids);
    }

    @Test
    void deleteBatch_正常删除() {
        List<Long> ids = Arrays.asList(1L, 2L);
        Dish dish1 = new Dish();
        dish1.setId(1L);
        dish1.setStatus(StatusConstant.DISABLE);
        Dish dish2 = new Dish();
        dish2.setId(2L);
        dish2.setStatus(StatusConstant.DISABLE);
        when(dishMapper.getById(1L)).thenReturn(dish1);
        when(dishMapper.getById(2L)).thenReturn(dish2);
        when(setMealDishMapper.getSetMealIdsByDishIds(ids)).thenReturn(Collections.emptyList());

        dishService.deleteBatch(ids);

        verify(dishMapper).deleteByIDs(ids);
        verify(dishFlavorMapper).deleteByDishIds(ids);
    }
}
