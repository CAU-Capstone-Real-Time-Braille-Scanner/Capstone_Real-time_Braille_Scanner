package com.example.realtimebraillescanner;

import java.util.HashMap;

public class mapping {

    HashMap<String, String> CHOSUNG_letters = new HashMap<>();
    HashMap<String, String> JUNGSUNG_letters = new HashMap<>();
    HashMap<String, String> JONGSUNG_letters = new HashMap<>();
    HashMap<String, String> contractions = new HashMap<>();
    HashMap<String, String> punctuation = new HashMap<>();
    HashMap<String, String> numbers = new HashMap<>();
    String number_start = new String();


    mapping(){
        CHOSUNG_letters.put("ㄱ",String.valueOf((char)(10248)));
        CHOSUNG_letters.put("ㄲ",String.valueOf((char)(10272))+String.valueOf((char)(10248)));
        CHOSUNG_letters.put("ㄴ",String.valueOf((char)(10249)));
        CHOSUNG_letters.put("ㄷ",String.valueOf((char)(10250)));
        CHOSUNG_letters.put("ㄸ",String.valueOf((char)(10272))+String.valueOf((char)(10250)));
        CHOSUNG_letters.put("ㄹ",String.valueOf((char)(10256)));
        CHOSUNG_letters.put("ㅁ",String.valueOf((char)(10257)));
        CHOSUNG_letters.put("ㅂ",String.valueOf((char)(10264)));
        CHOSUNG_letters.put("ㅃ",String.valueOf((char)(10272))+String.valueOf((char)(10264)));
        CHOSUNG_letters.put("ㅅ",String.valueOf((char)(10272)));
        CHOSUNG_letters.put("ㅆ",String.valueOf((char)(10272))+String.valueOf((char)(10272)));
        CHOSUNG_letters.put("ㅇ","");
        CHOSUNG_letters.put("ㅈ",String.valueOf((char)(10280)));
        CHOSUNG_letters.put("ㅉ",String.valueOf((char)(10272))+String.valueOf((char)(10280)));
        CHOSUNG_letters.put("ㅊ",String.valueOf((char)(10288)));
        CHOSUNG_letters.put("ㅋ",String.valueOf((char)(10251)));
        CHOSUNG_letters.put("ㅌ",String.valueOf((char)(10259)));
        CHOSUNG_letters.put("ㅍ",String.valueOf((char)(10265)));
        CHOSUNG_letters.put("ㅎ",String.valueOf((char)(10266)));

        JUNGSUNG_letters.put("ㅏ",String.valueOf((char)(10275)));
        JUNGSUNG_letters.put("ㅐ",String.valueOf((char)(10263)));
        JUNGSUNG_letters.put("ㅑ",String.valueOf((char)(10268)));
        JUNGSUNG_letters.put("ㅒ",String.valueOf((char)(10268))+String.valueOf((char)(10263)));
        JUNGSUNG_letters.put("ㅓ",String.valueOf((char)(10240 + 8 + 4 + 2)));
        JUNGSUNG_letters.put("ㅔ",String.valueOf((char)(10240 + 16 + 8 + 4 + 1)));
        JUNGSUNG_letters.put("ㅕ",String.valueOf((char)(10240 + 32 + 16 + 1)));
        JUNGSUNG_letters.put("ㅖ",String.valueOf((char)(10240 + 8 + 4)));
        JUNGSUNG_letters.put("ㅗ",String.valueOf((char)(10240 + 32 + 4 + 1)));
        JUNGSUNG_letters.put("ㅘ",String.valueOf((char)(10240 + 32 + 4 + 2 + 1)));
        JUNGSUNG_letters.put("ㅙ",String.valueOf((char)(10240 + 32 + 4 + 2 + 1))+String.valueOf((char)(10240 + 16 + 4 + 2 + 1)));
        JUNGSUNG_letters.put("ㅚ",String.valueOf((char)(10240 + 32 + 16 + 8 + 4 + 1)));
        JUNGSUNG_letters.put("ㅛ",String.valueOf((char)(10240 + 32 + 8 + 4)));
        JUNGSUNG_letters.put("ㅜ",String.valueOf((char)(10240 + 8 + 4 + 1)));
        JUNGSUNG_letters.put("ㅝ",String.valueOf((char)(10240 + 8 + 4 + 2 + 1)));
        JUNGSUNG_letters.put("ㅞ",String.valueOf((char)(10240 + 8 + 4 + 2 + 1))+String.valueOf((char)(10240 + 16 + 4 + 2 + 1)));
        JUNGSUNG_letters.put("ㅟ",String.valueOf((char)(10240 + 8 + 4 + 1))+String.valueOf((char)(10240 + 16 + 4 + 2 + 1)));
        JUNGSUNG_letters.put("ㅠ",String.valueOf((char)(10240 + 32 + 8 + 1)));
        JUNGSUNG_letters.put("ㅡ",String.valueOf((char)(10240 + 32 + 8 + 2)));
        JUNGSUNG_letters.put("ㅢ",String.valueOf((char)(10240 + 32 + 16 + 8 + 2)));
        JUNGSUNG_letters.put("ㅣ",String.valueOf((char)(10240 + 16 + 4 + 1)));

        JONGSUNG_letters.put("ㄱ",String.valueOf((char)(10240 + 1)));
        JONGSUNG_letters.put("ㄴ",String.valueOf((char)(10240 + 16 + 2)));
        JONGSUNG_letters.put("ㄷ",String.valueOf((char)(10240 + 16 + 4)));
        JONGSUNG_letters.put("ㄹ",String.valueOf((char)(10240 + 4)));
        JONGSUNG_letters.put("ㅁ",String.valueOf((char)(10240 + 32 + 4)));
        JONGSUNG_letters.put("ㅂ",String.valueOf((char)(10240 + 2 + 1)));
        JONGSUNG_letters.put("ㅅ",String.valueOf((char)(10240 + 4)));
        JONGSUNG_letters.put("ㅇ",String.valueOf((char)(10240 + 32 + 16 + 4 + 2)));
        JONGSUNG_letters.put("ㅈ",String.valueOf((char)(10240 + 4 + 1)));
        JONGSUNG_letters.put("ㅊ",String.valueOf((char)(10240 + 4 + 2)));
        JONGSUNG_letters.put("ㅋ",String.valueOf((char)(10240 + 16 + 4 + 2)));
        JONGSUNG_letters.put("ㅌ",String.valueOf((char)(10240 + 32 + 4 + 2)));
        JONGSUNG_letters.put("ㅍ",String.valueOf((char)(10240 + 32 + 16 + 2)));
        JONGSUNG_letters.put("ㅎ",String.valueOf((char)(10240 + 32 + 16 + 4)));

        JONGSUNG_letters.put("ㄲ",String.valueOf((char)(10240 + 1))+String.valueOf((char)(10240 + 1)));
        JONGSUNG_letters.put("ㄳ",String.valueOf((char)(10240 + 1))+String.valueOf((char)(10240 + 4)));
        JONGSUNG_letters.put("ㄵ",String.valueOf((char)(10240 + 16 + 2))+String.valueOf((char)(10240 + 4 + 1)));
        JONGSUNG_letters.put("ㄶ",String.valueOf((char)(10240 + 16 + 2))+String.valueOf((char)(10240 + 32 + 16 + 4)));
        JONGSUNG_letters.put("ㄺ",String.valueOf((char)(10240 + 4))+String.valueOf((char)(10240 + 1)));
        JONGSUNG_letters.put("ㄻ",String.valueOf((char)(10240 + 4))+String.valueOf((char)(10240 + 32 + 4)));
        JONGSUNG_letters.put("ㄼ",String.valueOf((char)(10240 + 4))+String.valueOf((char)(10240 + 2 + 1)));
        JONGSUNG_letters.put("ㄽ",String.valueOf((char)(10240 + 4))+String.valueOf((char)(10240 + 4)));
        JONGSUNG_letters.put("ㄾ",String.valueOf((char)(10240 + 4))+String.valueOf((char)(10240 + 32 + 4 + 2)));
        JONGSUNG_letters.put("ㄿ",String.valueOf((char)(10240 + 4))+String.valueOf((char)(10240 + 32 + 16 + 2)));
        JONGSUNG_letters.put("ㅀ",String.valueOf((char)(10240 + 4))+String.valueOf((char)(10240 + 32 + 16 + 4)));
        JONGSUNG_letters.put("ㅄ",String.valueOf((char)(10240 + 2 + 1))+String.valueOf((char)(10240 + 4)));
        JONGSUNG_letters.put("ㅆ",String.valueOf((char)(10240 + 8 + 4)));

        contractions.put("가", String.valueOf((char)(10240 + 32 + 8 + 2 + 1)));
        contractions.put("나", String.valueOf((char)(10240 + 8 + 1)));
        contractions.put("다", String.valueOf((char)(10240 + 8 + 2)));
        contractions.put("마", String.valueOf((char)(10240 + 16 + 1)));
        contractions.put("바", String.valueOf((char)(10240 + 16 + 8)));
        contractions.put("사", String.valueOf((char)(10240 + 4 + 2 + 1)));
        contractions.put("자", String.valueOf((char)(10240 + 32 + 8)));
        contractions.put("카", String.valueOf((char)(10240 + 8 + 2 + 1)));
        contractions.put("타", String.valueOf((char)(10240 + 16 + 2 + 1)));
        contractions.put("파", String.valueOf((char)(10240 + 16 + 8 + 1)));
        contractions.put("하", String.valueOf((char)(10240 + 16 + 8 + 2)));
        contractions.put("것", String.valueOf((char)(10240 + 32 + 16 + 8)));
        contractions.put("억", String.valueOf((char)(10240 + 32 + 16 + 8 + 1)));
        contractions.put("언", String.valueOf((char)(10240 + 63 - 1)));
        contractions.put("얼", String.valueOf((char)(10240 + 63 - 1 - 32)));
        contractions.put("연", String.valueOf((char)(10240 + 32 + 1)));
        contractions.put("열", String.valueOf((char)(10240 + 63 - 4 - 8)));
        contractions.put("영", String.valueOf((char)(10240 + 63 - 4)));
        contractions.put("옥", String.valueOf((char)(10240 + 63 - 16 - 2)));
        contractions.put("온", String.valueOf((char)(10240 + 63 - 8)));
        contractions.put("옹", String.valueOf((char)(10240 + 63)));
        contractions.put("운", String.valueOf((char)(10240 + 63 - 32 - 4)));
        contractions.put("울", String.valueOf((char)(10240 + 63 - 16)));
        contractions.put("은", String.valueOf((char)(10240 + 63 - 2 - 8)));
        contractions.put("을", String.valueOf((char)(10240 + 63 - 16 - 1)));
        contractions.put("인", String.valueOf((char)(10240 + 31)));
        contractions.put("그래서", String.valueOf((char)(10240 + 1))+String.valueOf((char)(10240 + 2 + 4 + 8)));
        contractions.put("그러나",String.valueOf((char)(10240 + 1))+String.valueOf((char)(10240 + 1 + 8)));
        contractions.put("그러면", String.valueOf((char)(10240 + 1))+String.valueOf((char)(10240 + 2 + 16)));
        contractions.put("그러므로", String.valueOf((char)(10240 + 1))+String.valueOf((char)(10240 + 2 + 32)));
        contractions.put("그런데", String.valueOf((char)(10240 + 1))+String.valueOf((char)(10240 + 63 - 32 - 2)));
        contractions.put("그리고", String.valueOf((char)(10240 + 1))+String.valueOf((char)(10240 + 1 + 4 + 32)));
        contractions.put("그리하여", String.valueOf((char)(10240 + 1))+String.valueOf((char)(10240 + 1 + 16 + 32)));

        punctuation.put(",", String.valueOf((char)(10240 + 16)));
        punctuation.put(";", String.valueOf((char)(10240 + 32 + 16))+String.valueOf((char)(10240 + 4 + 2)));
        punctuation.put(":", String.valueOf((char)(10240 + 16))+String.valueOf((char)(10240 + 2)));
        punctuation.put(".", String.valueOf((char)(10240 + 32 + 16 + 2)));
        punctuation.put("!", String.valueOf((char)(10240 + 16 + 4 + 2)));
        punctuation.put("(", String.valueOf((char)(10240 + 32 + 4 + 2))+String.valueOf((char)(10240 + 4)));
        punctuation.put(")", String.valueOf((char)(10240 + 32))+String.valueOf((char)(10240 + 32 + 16 + 4)));
        punctuation.put("\"", String.valueOf((char)(10240 + 32 + 4 + 2)));
        punctuation.put("\"", String.valueOf((char)(10240 + 32 + 16 + 4)));
        punctuation.put("?", String.valueOf((char)(10240 + 32 + 4 + 2)));
        punctuation.put("/", String.valueOf((char)(10240 + 32 + 16 + 8))+String.valueOf((char)(10240 + 8 + 4)));
        punctuation.put("...", String.valueOf((char)(10240 + 32 + 16 + 2))+String.valueOf((char)(10240 + 32 + 16 + 2)));
        punctuation.put("", String.valueOf((char)(10240 + 32))+String.valueOf((char)(10240 + 32 + 4 + 2)));
        punctuation.put("-", String.valueOf((char)(10240 + 32 + 4)));

        numbers.put("1",String.valueOf((char)(10240 + 8)));
        numbers.put("2",String.valueOf((char)(10240 + 16 + 8)));
        numbers.put("3",String.valueOf((char)(10240 + 8 + 1)));
        numbers.put("4",String.valueOf((char)(10240 + 8 + 2 + 1)));
        numbers.put("5",String.valueOf((char)(10240 + 8 + 2)));
        numbers.put("6",String.valueOf((char)(10240 + 16 + 8 + 1)));
        numbers.put("7",String.valueOf((char)(10240 + 16 + 8 + 2 + 1)));
        numbers.put("8",String.valueOf((char)(10240 + 16 + 8 + 2)));
        numbers.put("9",String.valueOf((char)(10240 + 16 + 1)));
        numbers.put("0",String.valueOf((char)(10240 + 16 + 2 + 1)));

        number_start = String.valueOf((char)(10240 + 32 + 4 + 2 + 1));

    }

}
