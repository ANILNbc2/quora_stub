package com.upgrad.quora.service.business;
import com.upgrad.quora.service.dao.UserDao;
import com.upgrad.quora.service.entity.QuestionEntity;
import com.upgrad.quora.service.dao.QuestionDao;
import com.upgrad.quora.service.entity.UserEntity;
import com.upgrad.quora.service.exception.AuthorizationFailedException;
import com.upgrad.quora.service.exception.InvalidQuestionException;
import com.upgrad.quora.service.exception.SignUpRestrictedException;
import com.upgrad.quora.service.exception.UserNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class QuestionService {
    @Autowired
    private QuestionDao questionDao;

    @Autowired
    private PasswordCryptographyProvider passwordCryptographyProvider;
    @Transactional(propagation = Propagation.REQUIRED)
    public QuestionEntity createQuestion(QuestionEntity questionEntity) {
        return questionDao.createQuestion(questionEntity);
    }
    
    @Transactional(propagation = Propagation.REQUIRED)
    public QuestionEntity editQuestion(String content, long userId, String questionUUID) throws AuthorizationFailedException, InvalidQuestionException{
        QuestionEntity questionEntity = questionDao.getQuestion(questionUUID);
        if(questionEntity == null)
        {
            throw new InvalidQuestionException("QUES-001", "Entered question uuid does not exist");
        }
        if(userId != questionEntity.getUser().getId())
        {
            throw new AuthorizationFailedException("ATHR-003", "Only the question owner can edit the question");
        }
        questionEntity.setContent(content);
        return questionDao.getQuestion(questionUUID);
    }

    public QuestionEntity getQuestionById(String questionUUID) throws InvalidQuestionException {
        QuestionEntity questionEntity = questionDao.getQuestion(questionUUID);
        if(questionEntity == null)
        {
            throw new InvalidQuestionException("QUES-001", "The question entered is invalid");
        }
        return questionEntity;
    }

}
