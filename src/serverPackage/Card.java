/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package serverPackage;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

/**
 *
 * @author Christopher
 */
public class Card implements Serializable {
    private List<String> list;
    
    public Card(){
        list = new LinkedList<>();
    }
    
    public void addToken(String token){
        if(token == null){
            
        }
        else{
            list.add(token);
        }
    }
    
    public boolean containsT(String token){
        return list.contains(token);
    }
}
