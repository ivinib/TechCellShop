package org.example.company.tcs.techcellshop.exception;

public class InvalidOrderStatusTransitionException extends RuntimeException{
    public InvalidOrderStatusTransitionException(String message){
        super(message);
    }
}
