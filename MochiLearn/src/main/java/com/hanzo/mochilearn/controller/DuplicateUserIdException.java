package com.hanzo.mochilearn.controller;

// user id 중복 커스텀 익셉션
public class DuplicateUserIdException extends RuntimeException {

    public DuplicateUserIdException(String message) {
        super(message);
    }
}
