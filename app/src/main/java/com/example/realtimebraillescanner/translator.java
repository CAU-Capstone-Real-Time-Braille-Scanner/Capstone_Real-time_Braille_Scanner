package com.example.realtimebraillescanner;

import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

public class translator {

    int BASE_CODE = 44032;
    int CHOSUNG = 588;
    int JUNGSUNG = 28;
    mapping mapping = new mapping();
    String braille = "";

    String[] CHOSUNG_LIST = {"ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ",
            "ㅂ", "ㅃ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ",
            "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"};
    String[] JUNGSUNG_LIST = {"ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ",
            "ㅖ", "ㅗ", "ㅘ", "ㅙ", "ㅚ", "ㅛ", "ㅜ",
            "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"};
    String[] JONGSUNG_LIST = {" ", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ",
            "ㄹ", "ㄺ", "ㄻ", "ㄼ", "ㄽ", "ㄾ", "ㄿ", "ㅀ",
            "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ",
            "ㅋ", "ㅌ", "ㅍ", "ㅎ"};

    translator(){}

    public ArrayList<String> extract_words(String text){
        String[] words = text.split(" ");
        ArrayList<String> result = new ArrayList<>();
        String[] temp;

        for(int i = 0; i < words.length; i++){
            temp = words[i].split("\n");
            for(int j = 0; j < temp.length; j++){
                result.add(temp[j]);
            }
        }
        return result;
    }

    public int check_contraction(String word, int idx){
        Set<String> keys = mapping.contractions.keySet();
        for (String key : keys){
            if (word.substring(idx).startsWith(key)){
                braille += mapping.contractions.get(key);
                return key.length();
            }
        }
        return 0;
    }

    public Boolean check_number(String word, int idx){
        if (Character.isDigit(word.charAt(idx))){
            if (idx != 0){
                if (Character.isDigit(word.charAt(idx-1))){
                    braille += mapping.numbers.get(word.charAt(idx));
                }
                else{
                    braille += mapping.number_start + mapping.numbers.get(word.charAt(idx));
                }
            }
            else{
                braille += mapping.number_start + mapping.numbers.get(word.charAt(idx));
            }
            return true;
        }
        return false;
    }

    public Boolean check_punctuation(String word, int idx){
        Set<String> keys = mapping.punctuation.keySet();
        for (String key : keys) {
            if (key.equals(word.charAt(idx))){
                braille += mapping.punctuation.get(key);
                return true;
            }
        }
        return false;
    }

    public Boolean check_character(String word, int idx){
        String[] keys = new String[1];
        Character key = word.charAt(idx);
        keys[0] = String.valueOf(key);
        int char_code, char1, char2, char3;

        if (Pattern.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*", keys[0])){


            char_code = (int)(keys[0].charAt(0)) - BASE_CODE;
            char1 = (char_code / CHOSUNG);
            char2 = ((char_code - (CHOSUNG * char1)) / JUNGSUNG);
            char3 = ((char_code - (CHOSUNG * char1) - (JUNGSUNG * char2)));
            braille += mapping.CHOSUNG_letters.get(CHOSUNG_LIST[char1]);
            braille += mapping.JUNGSUNG_letters.get(JUNGSUNG_LIST[char2]);
            if (char3 != 0){
                braille += mapping.JONGSUNG_letters.get(JONGSUNG_LIST[char3]);
            }
            return true;
        }
        return false;
    }

    public String translate(String text){
        ArrayList<String> words = new ArrayList<>();
        words = extract_words(text);

        for(String word : words){
            int i = 0;
            while (i < word.length()){
                int check_cont = check_contraction(word, i);

                if (check_cont != 0){
                    i += check_cont;
                    continue;
                }
                if (check_number(word, i)){
                    i+=1;
                    continue;
                }
                if (check_punctuation(word, i)){
                    i+=1;
                    continue;
                }
                check_character(word, i);
                i += 1;

            }
            braille += " ";
        }
        return braille;
    }

}
