package com.example.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.domain.dto.LoginDto;
import com.example.domain.dto.RegisterDto;
import com.example.domain.dto.UserUpdateDto;
import com.example.domain.entity.User;
import com.example.domain.vo.UserInfoVo;
import com.example.enums.UserRoleEnum;
import com.example.exception.UnauthorizedException;
import com.example.exception.UserException;
import com.example.mapper.UserMapper;
import com.example.service.UserService;
import com.example.util.BCryptUtil;
import lombok.RequiredArgsConstructor;
import net.sf.jsqlparser.util.validation.metadata.DatabaseException;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;


@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final BCryptUtil bCryptUtil;

    @Override
    public User findEnabledUserId(String email) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(User::getEmail, email);
        return this.getOne(queryWrapper);
    }

    @Override
    public User checkPassword(LoginDto loginDto) {
        User user = findEnabledUserId(loginDto.getEmail());
        if (user == null) {
            // 用户不存在
            return null;
        }
        if (!bCryptUtil.matches(loginDto.getPassword().trim(), user.getPassword())) {
            // 密码错误
            return null;
        }
        return user;
    }

    @Override
    public String register(RegisterDto registerDto) throws DatabaseException {
        String email = registerDto.getEmail();
        String password = bCryptUtil.hashPassword(registerDto.getPassword().trim()); // 去掉字符串开头和结尾的空格
        User user = User.builder().email(email).password(password).build();
        if (!this.save(user)) {
            throw new DatabaseException("用户信息保存失败");
        }
        return user.getEmail();
    }

    @Override
    public void updateUserInfo(Long userId, UserUpdateDto userUpdateDto) throws UserException, DatabaseException {
        // 先判断是否存在
        if (!this.exists(Wrappers.<User>lambdaQuery()
                .eq(User::getId, userId))) {
            throw new UserException("用户ID不存在或不可用");
        }
        User user = new User();
        user.setId(userId);
        BeanUtils.copyProperties(userUpdateDto, user);
        if (userUpdateDto.getPassword() != null && userUpdateDto.getConfirmPassword() != null) {
            if (!userUpdateDto.getPassword().equals(userUpdateDto.getConfirmPassword())) {
                throw new UserException("两次输入的新密码不一致");
            }
            user.setPassword(bCryptUtil.hashPassword(userUpdateDto.getPassword()));
        }
        if (!this.updateById(user)) {
            throw new DatabaseException("MybatisPlus更新数据库失败");
        }
    }

    @Override
    public void removeUser(Long userId, Long id) throws DatabaseException {
        UserRoleEnum auth = this.getById(userId).getAuth();
        if (!auth.equals(UserRoleEnum.ADMIN)) {
            throw new UnauthorizedException("权限不足");
        }
        if (userId.equals(id)) {
            throw new UnauthorizedException("无法删除已登录账号");
        }
        if (!this.removeById(id)) {
            throw new DatabaseException("删除用户失败");
        }
    }

    @Override
    public User getUserByEmail(String email) {
        return lambdaQuery()
                .like(User::getEmail, email)
                .one();
    }

    @Override
    public PageDTO<UserInfoVo> getAllUserInfo(Integer pageNum, Integer pageSize) {
        // 分页查询
        Page<User> page = this.lambdaQuery().page(new Page<>(pageNum, pageSize));

        // 获取分页记录
        List<User> records = page.getRecords();

        // 如果没有记录，返回空的 PageDTO
        if (records.isEmpty()) {
            return new PageDTO<>();
        }

        // 转换结果
        List<UserInfoVo> userInfoVo = records.stream()
                .map(this::getUserInfoVo)
                .filter(Objects::nonNull)
                .toList();

        // 用 PageDTO 包装
        PageDTO<UserInfoVo> result = new PageDTO<>(pageNum, pageSize, page.getTotal());
        result.setRecords(userInfoVo);

        return result;
    }

    private UserInfoVo getUserInfoVo(User user) {
        if (user != null) {
            // 封装vo
            UserInfoVo userInfoVo = new UserInfoVo();
            BeanUtils.copyProperties(user, userInfoVo);
            return userInfoVo;
        }
        return null;
    }
}
