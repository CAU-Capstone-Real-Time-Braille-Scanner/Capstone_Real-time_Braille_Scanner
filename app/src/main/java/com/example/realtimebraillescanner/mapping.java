package com.example.realtimebraillescanner;

import java.util.ArrayList;
import java.util.HashMap;

public class mapping {

    HashMap<String, String> CHOSUNG_letters = new HashMap<>();
    HashMap<String, String> JUNGSUNG_letters = new HashMap<>();
    HashMap<String, String> JONGSUNG_letters = new HashMap<>();
    HashMap<String, String> contractions = new HashMap<>();
    HashMap<String, String> punctuation = new HashMap<>();
    HashMap<String, String> numbers = new HashMap<>();
    HashMap<String, String> decompose = new HashMap<>();
    String number_start = new String();
    String CHOSUNG_start = new String();
    ArrayList<String> quotation = new ArrayList<>();


    mapping(){
        CHOSUNG_letters.put("ㄱ","⠈");
        CHOSUNG_letters.put("ㄲ","⠠⠈");
        CHOSUNG_letters.put("ㄴ","⠉");
        CHOSUNG_letters.put("ㄷ","⠊");
        CHOSUNG_letters.put("ㄸ","⠠⠊");
        CHOSUNG_letters.put("ㄹ","⠐");
        CHOSUNG_letters.put("ㅁ","⠑");
        CHOSUNG_letters.put("ㅂ","⠘");
        CHOSUNG_letters.put("ㅃ","⠠⠘");
        CHOSUNG_letters.put("ㅅ","⠠");
        CHOSUNG_letters.put("ㅆ","⠠⠠");
        CHOSUNG_letters.put("ㅇ","");
        CHOSUNG_letters.put("ㅈ","⠨");
        CHOSUNG_letters.put("ㅉ","⠠⠨");
        CHOSUNG_letters.put("ㅊ","⠰");
        CHOSUNG_letters.put("ㅋ","⠋");
        CHOSUNG_letters.put("ㅌ","⠓");
        CHOSUNG_letters.put("ㅍ","⠙");
        CHOSUNG_letters.put("ㅎ","⠚");

        CHOSUNG_letters.put("ㄳ","⠈⠠");
        CHOSUNG_letters.put("ㄵ","⠉⠨");
        CHOSUNG_letters.put("ㄶ","⠉⠚");
        CHOSUNG_letters.put("ㄺ","⠐⠈");
        CHOSUNG_letters.put("ㄻ","⠐⠑");
        CHOSUNG_letters.put("ㄼ","⠐⠘");
        CHOSUNG_letters.put("ㄽ","⠐⠠");
        CHOSUNG_letters.put("ㄾ","⠐⠓");
        CHOSUNG_letters.put("ㄿ","⠐⠙");
        CHOSUNG_letters.put("ㅀ","⠐⠚");
        CHOSUNG_letters.put("ㅄ","⠘⠠");

        CHOSUNG_start = "⠿";

        JUNGSUNG_letters.put("ㅏ","⠣");
        JUNGSUNG_letters.put("ㅐ","⠗");
        JUNGSUNG_letters.put("ㅑ","⠜");
        JUNGSUNG_letters.put("ㅒ","⠜⠗");
        JUNGSUNG_letters.put("ㅓ","⠎");
        JUNGSUNG_letters.put("ㅔ","⠝");
        JUNGSUNG_letters.put("ㅕ","⠱");
        JUNGSUNG_letters.put("ㅖ","⠌");
        JUNGSUNG_letters.put("ㅗ","⠥");
        JUNGSUNG_letters.put("ㅘ","⠧");
        JUNGSUNG_letters.put("ㅙ","⠧⠗");
        JUNGSUNG_letters.put("ㅚ","⠽");
        JUNGSUNG_letters.put("ㅛ","⠬");
        JUNGSUNG_letters.put("ㅜ","⠍");
        JUNGSUNG_letters.put("ㅝ","⠏");
        JUNGSUNG_letters.put("ㅞ","⠏⠗");
        JUNGSUNG_letters.put("ㅟ","⠍⠗");
        JUNGSUNG_letters.put("ㅠ","⠩");
        JUNGSUNG_letters.put("ㅡ","⠪");
        JUNGSUNG_letters.put("ㅢ","⠺");
        JUNGSUNG_letters.put("ㅣ","⠕");

        JONGSUNG_letters.put("ㄱ","⠁");
        JONGSUNG_letters.put("ㄴ","⠒");
        JONGSUNG_letters.put("ㄷ","⠔");
        JONGSUNG_letters.put("ㄹ","⠂");
        JONGSUNG_letters.put("ㅁ","⠢");
        JONGSUNG_letters.put("ㅂ","⠃");
        JONGSUNG_letters.put("ㅅ","⠄");
        JONGSUNG_letters.put("ㅇ","⠶");
        JONGSUNG_letters.put("ㅈ","⠅");
        JONGSUNG_letters.put("ㅊ","⠆");
        JONGSUNG_letters.put("ㅋ","⠖");
        JONGSUNG_letters.put("ㅌ","⠦");
        JONGSUNG_letters.put("ㅍ","⠲");
        JONGSUNG_letters.put("ㅎ","⠴");

        JONGSUNG_letters.put("ㄲ","⠁⠁");
        JONGSUNG_letters.put("ㄳ","⠁⠄");
        JONGSUNG_letters.put("ㄵ","⠒⠅");
        JONGSUNG_letters.put("ㄶ","⠒⠴");
        JONGSUNG_letters.put("ㄺ","⠂⠁");
        JONGSUNG_letters.put("ㄻ","⠂⠢");
        JONGSUNG_letters.put("ㄼ","⠂⠃");
        JONGSUNG_letters.put("ㄽ","⠂⠄");
        JONGSUNG_letters.put("ㄾ","⠂⠦");
        JONGSUNG_letters.put("ㄿ","⠂⠲");
        JONGSUNG_letters.put("ㅀ","⠂⠴");
        JONGSUNG_letters.put("ㅄ","⠃⠄");
        JONGSUNG_letters.put("ㅆ","⠌");

        decompose.put("ㄲ", "ㄱㄱ");
        decompose.put("ㄳ", "ㄱㅅ");
        decompose.put("ㄵ", "ㄴㅈ");
        decompose.put("ㄶ", "ㄴㅎ");
        decompose.put("ㄺ", "ㄹㄱ");
        decompose.put("ㄻ", "ㄹㅁ");
        decompose.put("ㄼ", "ㄹㅂ");
        decompose.put("ㄽ", "ㄹㅅ");
        decompose.put("ㄾ", "ㄹㅌ");
        decompose.put("ㄿ", "ㄹㅍ");
        decompose.put("ㅀ", "ㄹㅎ");
        decompose.put("ㅄ", "ㅂㅅ");
        decompose.put("ㅆ", "ㅅㅅ");

        contractions.put("따", "⠠⠊");
        contractions.put("빠", "⠠⠘");
        contractions.put("짜", "⠠⠨");
        contractions.put("가", "⠫");
        contractions.put("나", "⠉");
        contractions.put("다", "⠊");
        contractions.put("마", "⠑");
        contractions.put("바", "⠘");
        contractions.put("사", "⠇");
        contractions.put("자", "⠨");
        contractions.put("카", "⠋");
        contractions.put("타", "⠓");
        contractions.put("파", "⠙");
        contractions.put("하", "⠚");
        contractions.put("것", "⠸⠎");
        contractions.put("억", "⠹");
        contractions.put("언", "⠾");
        contractions.put("얼", "⠞");
        contractions.put("연", "⠡");
        contractions.put("열", "⠳");
        contractions.put("영", "⠻");
        contractions.put("옥", "⠭");
        contractions.put("온", "⠷");
        contractions.put("옹", "⠿");
        contractions.put("운", "⠛");
        contractions.put("울", "⠯");
        contractions.put("은", "⠵");
        contractions.put("을", "⠮");
        contractions.put("인", "⠟");
        contractions.put("까", "⠠⠫");
        contractions.put("싸", "⠠⠇");
        contractions.put("껏", "⠠⠸⠎");
        contractions.put("그래서", "⠁⠎");
        contractions.put("그러나", "⠁⠉");
        contractions.put("그러면", "⠁⠒");
        contractions.put("그러므로", "⠁⠢");
        contractions.put("그런데", "⠁⠝");
        contractions.put("그리고", "⠁⠥");
        contractions.put("그리하여", "⠁⠱");

        punctuation.put(",", "⠐");
        punctuation.put("·", "⠐⠆");
        punctuation.put(";", "⠰⠆");
        punctuation.put(":", "⠐⠂");
        punctuation.put(".", "⠲");
        punctuation.put("!", "⠖");
        punctuation.put("(", "⠦⠄");
        punctuation.put(")", "⠠⠴");
        punctuation.put("{", "⠦⠂");
        punctuation.put("}", "⠐⠴");
        punctuation.put("[", "⠦⠆");
        punctuation.put("]", "⠰⠴");
//        punctuation.put("“", "⠦");
//        punctuation.put("”", "⠴");
//        punctuation.put("'", "⠠⠦");
//        punctuation.put("'", "⠴⠄");
        punctuation.put("?", "⠦");
        punctuation.put("/", String.valueOf((char)(10240 + 32 + 16 + 8))+String.valueOf((char)(10240 + 8 + 4)));
        punctuation.put("...", String.valueOf((char)(10240 + 32 + 16 + 2))+String.valueOf((char)(10240 + 32 + 16 + 2)));
        punctuation.put("", String.valueOf((char)(10240 + 32))+String.valueOf((char)(10240 + 32 + 4 + 2)));
        punctuation.put("-", "⠤");
        punctuation.put("~", "⠤⠤");

        quotation.add("⠦");
        quotation.add("⠴");
        quotation.add("⠠⠦");
        quotation.add("⠴⠄");

        numbers.put("1","⠁");
        numbers.put("2","⠃");
        numbers.put("3","⠉");
        numbers.put("4","⠙");
        numbers.put("5","⠑");
        numbers.put("6","⠋");
        numbers.put("7","⠛");
        numbers.put("8","⠓");
        numbers.put("9","⠊");
        numbers.put("0","⠚");

        number_start = "⠼";

    }

}
