package com.hanzo.mochilearn.controller;

public class DuplicateNicknameException extends RuntimeException {

    public DuplicateNicknameException(String message) {
        super(message);
    }
}
