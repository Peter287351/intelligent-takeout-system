package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.entity.Dish;
import com.sky.entity.Setmeal;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.exception.SetmealEnableFailedException;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetMealDishMapper;
import com.sky.mapper.SetmealMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SetmealServiceImplTest {

    @Mock
    private SetmealMapper setmealMapper;
    @Mock
    private SetMealDishMapper setmealDishMapper;
    @Mock
    private DishMapper dishMapper;

    @InjectMocks
    private SetmealServiceImpl setmealService;

    @Test
    void deleteBatch_起售中套餐不能删除() {
        List<Long> ids = Arrays.asList(1L);
        Setmeal setmeal = new Setmeal();
        setmeal.setId(1L);
        setmeal.setStatus(StatusConstant.ENABLE);
        when(setmealMapper.getById(1L)).thenReturn(setmeal);

        assertThatThrownBy(() -> setmealService.deleteBatch(ids))
                .isInstanceOf(DeletionNotAllowedException.class)
                .hasMessage(MessageConstant.SETMEAL_ON_SALE);
        verify(setmealMapper, never()).deleteById(any());
    }

    @Test
    void deleteBatch_正常删除() {
        List<Long> ids = Arrays.asList(1L);
        Setmeal setmeal = new Setmeal();
        setmeal.setId(1L);
        setmeal.setStatus(StatusConstant.DISABLE);
        when(setmealMapper.getById(1L)).thenReturn(setmeal);

        setmealService.deleteBatch(ids);

        verify(setmealMapper).deleteById(1L);
        verify(setmealDishMapper).deleteBySetmealId(1L);
    }

    @Test
    void startOrStop_套餐内含停售菜品_无法启售() {
        Dish dish = new Dish();
        dish.setStatus(StatusConstant.DISABLE);
        when(dishMapper.getBySetmealId(1L)).thenReturn(Arrays.asList(dish));

        assertThatThrownBy(() -> setmealService.startOrStop(StatusConstant.ENABLE, 1L))
                .isInstanceOf(SetmealEnableFailedException.class)
                .hasMessage(MessageConstant.SETMEAL_ENABLE_FAILED);
        verify(setmealMapper, never()).update(any());
    }

    @Test
    void startOrStop_套餐内均为启售菜品_正常启售() {
        Dish dish = new Dish();
        dish.setStatus(StatusConstant.ENABLE);
        when(dishMapper.getBySetmealId(1L)).thenReturn(Arrays.asList(dish));

        setmealService.startOrStop(StatusConstant.ENABLE, 1L);

        verify(setmealMapper).update(any(Setmeal.class));
    }
}
