package org.example.company.tcs.techcellshop.dto.response;

public record UserResponse (
        Long idUser,
        String nameUser,
        String emailUserMasked,
        String phoneUser,
        String addressUser,
        String roleUser
){}