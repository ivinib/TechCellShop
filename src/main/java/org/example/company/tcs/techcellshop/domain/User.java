package org.example.company.tcs.techcellshop.domain;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Entity(name = "tb_user")
@Data
@EqualsAndHashCode
@ToString
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_user")
    private Long idUser;

    @Column(name = "name_user")
    private String nameUser;

    @Column(name = "email_user")
    private String emailUser;

    @Column(name = "password_user")
    private String passwordUser;

    @Column(name = "phone_user")
    private String phoneUser;

    @Column(name = "address_user")
    private String addressUser;

    @Column(name = "role_user")
    private String roleUser;


}
