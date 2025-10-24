package com.titanic00.cloud.dto;

import com.titanic00.cloud.enumeration.ObjectType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
public class MinioObjectDTO {
    private String path;
    private String name;
    private ObjectType type;
}
