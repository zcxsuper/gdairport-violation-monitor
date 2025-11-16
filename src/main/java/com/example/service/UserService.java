package com.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.IService;
import com.example.domain.dto.LoginDto;
import com.example.domain.dto.RegisterDto;
import com.example.domain.dto.UserUpdateDto;
import com.example.domain.entity.User;
import com.example.domain.vo.UserInfoVo;

public interface UserService extends IService<User> {

    User findEnabledUserId(String email);

    User checkPassword(LoginDto loginDto);

    String register(RegisterDto registerDto);

    void updateUserInfo(Long userId, UserUpdateDto userUpdateDto);

    void removeUser(Long userId, Long id);

    User getUserByEmail(String email);

    PageDTO<UserInfoVo> getAllUserInfo(Integer pageNum, Integer pageSize);


}
