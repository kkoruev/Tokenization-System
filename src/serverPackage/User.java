/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverPackage;

import java.io.Serializable;

/**
 *
 * @author Christopher
 */
public class User implements Serializable{
    private String userName;
    private String password;
    private Character rightForTokens;
    private Character rightForCreditCardNumber;
    
    public User(String userName,String password,Character rightForTokens,Character rightFotCreditCardNumber){
        setUserName(userName);
        setPassword(password);
        setRightForCreditCardNumber(rightForCreditCardNumber);
        setRightForTokens(rightForTokens);
    }

    public void setRightForTokens(Character rightForTokens) {
        this.rightForTokens = rightForTokens;
    }

    public void setRightForCreditCardNumber(Character rightForCreditCardNumber) {
        this.rightForCreditCardNumber = rightForCreditCardNumber;
    }
    
    

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getUserName() {
        return userName;
    }
    
    public boolean checkPassword(String password){
        if(this.password.equals(password)){
            return true;
        }
        return false;
    }
}
