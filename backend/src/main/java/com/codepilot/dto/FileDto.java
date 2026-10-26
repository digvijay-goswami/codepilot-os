package com.codepilot.dto;
import lombok.*;
@Data @AllArgsConstructor @NoArgsConstructor @Builder
public class FileDto {
    private String path;
    private String content;
}