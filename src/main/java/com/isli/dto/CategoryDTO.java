package com.isli.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Builder;

@Builder
public class CategoryDTO {
    private Long id;

    @NotBlank
    private String name;

    public CategoryDTO() {}

    public CategoryDTO(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public @NotBlank String getName() {
        return name;
    }

    public void setName(@NotBlank String name) {
        this.name = name;
    }
}
