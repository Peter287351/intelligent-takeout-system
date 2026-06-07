package com.sky.service.impl;

import com.sky.constant.MessageConstant;
import com.sky.constant.StatusConstant;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceImplTest {

    @Mock
    private EmployeeMapper employeeMapper;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeServiceImpl employeeService;

    private EmployeeLoginDTO loginDTO;

    @BeforeEach
    void setUp() {
        loginDTO = new EmployeeLoginDTO();
        loginDTO.setUsername("admin");
        loginDTO.setPassword("123456");
    }

    @Test
    void login_账号不存在_抛异常() {
        when(employeeMapper.getByUsername("admin")).thenReturn(null);

        assertThatThrownBy(() -> employeeService.login(loginDTO))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage(MessageConstant.ACCOUNT_NOT_FOUND);
    }

    @Test
    void login_BCrypt密码正确_登录成功() {
        Employee employee = Employee.builder()
                .username("admin")
                .password("$2a$10$hashedpassword")
                .status(StatusConstant.ENABLE)
                .build();
        when(employeeMapper.getByUsername("admin")).thenReturn(employee);
        when(passwordEncoder.matches("123456", "$2a$10$hashedpassword")).thenReturn(true);

        Employee result = employeeService.login(loginDTO);

        assertThat(result.getUsername()).isEqualTo("admin");
        verify(employeeMapper, never()).update(any());
    }

    @Test
    void login_BCrypt密码错误_抛异常() {
        Employee employee = Employee.builder()
                .username("admin")
                .password("$2a$10$hashedpassword")
                .status(StatusConstant.ENABLE)
                .build();
        when(employeeMapper.getByUsername("admin")).thenReturn(employee);
        when(passwordEncoder.matches("123456", "$2a$10$hashedpassword")).thenReturn(false);

        assertThatThrownBy(() -> employeeService.login(loginDTO))
                .isInstanceOf(PasswordErrorException.class)
                .hasMessage(MessageConstant.PASSWORD_ERROR);
    }

    @Test
    void login_MD5密码正确_自动升级为BCrypt() {
        // "123456" 的 MD5 值
        String md5Password = "e10adc3949ba59abbe56e057f20f883e";
        Employee employee = Employee.builder()
                .username("admin")
                .password(md5Password)
                .status(StatusConstant.ENABLE)
                .build();
        when(employeeMapper.getByUsername("admin")).thenReturn(employee);
        when(passwordEncoder.encode("123456")).thenReturn("$2a$10$newhashed");

        Employee result = employeeService.login(loginDTO);

        assertThat(result.getUsername()).isEqualTo("admin");
        assertThat(result.getPassword()).isEqualTo("$2a$10$newhashed");
        verify(employeeMapper).update(employee);
        verify(passwordEncoder, never()).matches(any(), any());
    }

    @Test
    void login_MD5密码错误_抛异常() {
        Employee employee = Employee.builder()
                .username("admin")
                .password("e10adc3949ba59abbe56e057f20f883e")
                .status(StatusConstant.ENABLE)
                .build();
        when(employeeMapper.getByUsername("admin")).thenReturn(employee);

        loginDTO.setPassword("wrong_password");

        assertThatThrownBy(() -> employeeService.login(loginDTO))
                .isInstanceOf(PasswordErrorException.class)
                .hasMessage(MessageConstant.PASSWORD_ERROR);
        verify(employeeMapper, never()).update(any());
    }

    @Test
    void login_账号被锁定_抛异常() {
        Employee employee = Employee.builder()
                .username("admin")
                .password("$2a$10$hashedpassword")
                .status(StatusConstant.DISABLE)
                .build();
        when(employeeMapper.getByUsername("admin")).thenReturn(employee);
        when(passwordEncoder.matches("123456", "$2a$10$hashedpassword")).thenReturn(true);

        assertThatThrownBy(() -> employeeService.login(loginDTO))
                .isInstanceOf(AccountLockedException.class)
                .hasMessage(MessageConstant.ACCOUNT_LOCKED);
    }
}
