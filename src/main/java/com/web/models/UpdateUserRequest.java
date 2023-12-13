package com.web.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private long id;
    private String fullname;
    private String phone;
    private String email;
}
