package org.example.company.tcs.techcellshop.controller.dto.response;

public record UserResponse (
        Long idUser,
        String nameUser,
        String emailUserMasked,
        String phoneUser,
        String addressUser,
        String roleUser
){}