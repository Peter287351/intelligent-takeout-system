package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.exception.DeletionNotAllowedException;
import com.sky.mapper.CategoryMapper;
import com.sky.mapper.DishMapper;
import com.sky.mapper.SetmealMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private DishMapper dishMapper;
    @Mock
    private SetmealMapper setmealMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    @Test
    void deleteById_关联菜品不能删除() {
        when(dishMapper.countByCategoryId(1L)).thenReturn(3);

        assertThatThrownBy(() -> categoryService.deleteById(1L))
                .isInstanceOf(DeletionNotAllowedException.class)
                .hasMessage(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
        verify(categoryMapper, never()).deleteById(1L);
    }

    @Test
    void deleteById_关联套餐不能删除() {
        when(dishMapper.countByCategoryId(1L)).thenReturn(0);
        when(setmealMapper.countByCategoryId(1L)).thenReturn(2);

        assertThatThrownBy(() -> categoryService.deleteById(1L))
                .isInstanceOf(DeletionNotAllowedException.class)
                .hasMessage(MessageConstant.CATEGORY_BE_RELATED_BY_SETMEAL);
        verify(categoryMapper, never()).deleteById(1L);
    }

    @Test
    void deleteById_正常删除() {
        when(dishMapper.countByCategoryId(1L)).thenReturn(0);
        when(setmealMapper.countByCategoryId(1L)).thenReturn(0);

        categoryService.deleteById(1L);

        verify(categoryMapper).deleteById(1L);
    }
}
