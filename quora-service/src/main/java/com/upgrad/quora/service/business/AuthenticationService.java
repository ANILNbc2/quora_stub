package com.upgrad.quora.service.business;


import com.upgrad.quora.service.dao.UserDao;
import com.upgrad.quora.service.entity.UserAuthEntity;
import com.upgrad.quora.service.entity.UserEntity;
import com.upgrad.quora.service.exception.AuthenticationFailedException;
import com.upgrad.quora.service.exception.AuthorizationFailedException;
import com.upgrad.quora.service.exception.SignOutRestrictedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

@Service
public class AuthenticationService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private PasswordCryptographyProvider CryptographyProvider;

    @Transactional(propagation = Propagation.REQUIRED)
    public UserAuthEntity authenticate(final String username, final String password) throws AuthenticationFailedException {
        UserEntity userEntity = userDao.getUserByUsername(username);
        if (userEntity == null) {
            throw new AuthenticationFailedException("ATH-001", "This username does not exist");
        }

        final String encryptedPassword = CryptographyProvider.encrypt(password, userEntity.getSalt());
        if (encryptedPassword.equals(userEntity.getPassword())) {
            JwtTokenProvider jwtTokenProvider = new JwtTokenProvider(encryptedPassword);
            UserAuthEntity userAuthEntity = new UserAuthEntity();
            userAuthEntity.setUser(userEntity);
            final ZonedDateTime now = ZonedDateTime.now();
            final ZonedDateTime expiresAt = now.plusHours(8);

            userAuthEntity.setAccessToken(jwtTokenProvider.generateToken(userEntity.getUuid(), now, expiresAt));
            userAuthEntity.setUuid(userEntity.getUuid());

            userAuthEntity.setLoginAt(now);
            userAuthEntity.setExpiresAt(expiresAt);
            userAuthEntity.setLogoutAt(null);//case of relogin

            userDao.createAuthToken(userAuthEntity);

            userDao.updateUser(userEntity);
            return userAuthEntity;
        } else {
            throw new AuthenticationFailedException("ATH-002", "Password failed");
        }
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserAuthEntity logoff(final String acessToken) throws SignOutRestrictedException {
        UserAuthEntity userAuthEntity = userDao.getUserByToken(acessToken);
        if (userAuthEntity == null || ZonedDateTime.now().compareTo(userAuthEntity.getExpiresAt()) >= 0 ) {
            throw new SignOutRestrictedException("SGR-001", "User is not Signed in");
        }
        userAuthEntity.setExpiresAt(ZonedDateTime.now());
        userAuthEntity.setLogoutAt(ZonedDateTime.now());
        userDao.updateUserAuthEntity(userAuthEntity);
        return userAuthEntity;
    }

    @Transactional(propagation = Propagation.REQUIRED)
    public UserAuthEntity validateBearerAuthentication(final String accessToken, final String context) throws AuthorizationFailedException {
        UserAuthEntity userAuthEntity = userDao.getUserByToken(accessToken);
        if (userAuthEntity == null ) {
            throw new AuthorizationFailedException("ATHR-001", "User has not signed in");
        }
        else if(userAuthEntity.getLogoutAt() !=null ){
            throw new AuthorizationFailedException("ATHR-002", "User is signed out.Sign in first " + context);
        }
        return userAuthEntity;
    }

    public String getBearerAccessToken(final String authorization) throws AuthenticationFailedException{
        String[] tokens = authorization.split("Bearer ");
        String accessToken = null;
        try{
            accessToken = tokens[1];
        }catch(IndexOutOfBoundsException ie){
            accessToken = tokens[0]; //for scenarios where those users don't adhere to adding prefix of Bearer like test cases
            if (accessToken==null){
               throw new AuthenticationFailedException("ATH-005","Use format: 'Bearer accessToken'");
            }
        }

        return accessToken;
    }


    @Transactional(propagation = Propagation.REQUIRED)
    public UserAuthEntity checkAuthenticationforCreateQuestion(final String accessToken) throws AuthorizationFailedException {
        UserAuthEntity userAuthEntity = userDao.getUserByToken(accessToken);
        if (userAuthEntity == null ) {
            throw new AuthorizationFailedException("ATHR-001", "User has not signed in");
        }
        else if(userAuthEntity.getLogoutAt() !=null ){
            throw new AuthorizationFailedException("ATHR-002", "User is signed out.Sign in first to post a question");
        }
        return userAuthEntity;
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public UserAuthEntity checkAuthenticationEditQuestion(final String accessToken) throws AuthorizationFailedException {
        UserAuthEntity userAuthEntity = userDao.getUserByToken(accessToken);
        if (userAuthEntity == null ) {
            throw new AuthorizationFailedException("ATHR-001", "User has not signed in");
        }
        else if(userAuthEntity.getLogoutAt() !=null ){
            throw new AuthorizationFailedException("ATHR-002", "User is signed out.Sign in first to edit the question");
        }
        return userAuthEntity;
    }

}
