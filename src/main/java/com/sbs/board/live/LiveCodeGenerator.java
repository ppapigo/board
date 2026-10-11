package com.sbs.board.live;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.Locale;

@Component
public class LiveCodeGenerator {
    public static final String ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ123456789";
    public static final int LENGTH = 6;

    private final SecureRandom random = new SecureRandom();

    public String generate(){
        StringBuilder code = new StringBuilder(LENGTH);
        for(int i =0; i<LENGTH; i++){
            code.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        }
        return code.toString();
    }

    public static String normalize(String code){
        return code == null ? "" : code.strip().toUpperCase(Locale.ROOT);
    }

}
