package com.titanic00.cloud.dto;

import com.titanic00.cloud.entity.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserDTO {
    private String username;

    public static UserDTO from(User user) {
        return UserDTO.builder().username(user.getUsername()).build();
    }
}
