package com.playflow.service;

import com.playflow.dto.ApiDtos.*;
import com.playflow.entity.Entities.User;
import com.playflow.entity.Entities.Wallet;
import com.playflow.entity.Entities.SpendingLimit;
import com.playflow.repository.UserRepository;
import com.playflow.repository.WalletRepository;
import com.playflow.repository.SpendingLimitRepository;
import com.playflow.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service public class AuthService {
    private final UserRepository users; private final WalletRepository wallets; private final SpendingLimitRepository spendingLimits; private final PasswordEncoder encoder; private final JwtService jwt;
    public AuthService(UserRepository users,WalletRepository wallets,SpendingLimitRepository spendingLimits,PasswordEncoder encoder,JwtService jwt){this.users=users;this.wallets=wallets;this.spendingLimits=spendingLimits;this.encoder=encoder;this.jwt=jwt;}
    @Transactional public AuthResponse register(RegisterRequest request){
        if(request.name()==null||request.name().isBlank()||request.email()==null||request.password()==null||request.password().length()<8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Name, email and a password of at least 8 characters are required");
        String email=request.email().trim().toLowerCase(); if(users.existsByEmailIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this email already exists");
        String phone=normalizePhone(request.phoneNumber()); if(users.existsByPhoneNumber(phone)) throw new ResponseStatusException(HttpStatus.CONFLICT,"An account with this phone number already exists");
        User user=users.save(new User(request.name().trim(),email,encoder.encode(request.password()),phone)); wallets.save(new Wallet(user.id)); spendingLimits.save(new SpendingLimit(user.id,new java.math.BigDecimal("5000.00"),new java.math.BigDecimal("50000.00"))); return response(user);
    }
    public AuthResponse login(LoginRequest request){
        User user=users.findByEmailIgnoreCase(request.email()==null?"":request.email().trim()).orElseThrow(()->new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password"));
        if(!encoder.matches(request.password(),user.passwordHash)) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Invalid email or password"); return response(user);
    }
    private String normalizePhone(String value){String digits=value==null?"":value.replaceAll("\\D","");if(digits.startsWith("91")&&digits.length()==12)digits=digits.substring(2);return digits;}
    private AuthResponse response(User user){return new AuthResponse(jwt.create(user.email,user.role),new UserView(user.id,user.name,user.email,user.role,user.kycStatus,user.phoneNumber));}
}
