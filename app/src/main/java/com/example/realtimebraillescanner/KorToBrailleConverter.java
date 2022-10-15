package com.example.realtimebraillescanner;

import android.util.Log;

import com.github.kimkevin.hangulparser.HangulParser;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Pattern;

public class KorToBrailleConverter {

    int BASE_CODE = 44032;
    int CHOSUNG = 588;
    int JUNGSUNG = 28;
    mapping mapping = new mapping();
    String braille = "";
    Boolean flag10 = false;

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

    KorToBrailleConverter(){}

    public ArrayList<String> extract_words(String text){ //문자열 띄어쓰기 기준으로 분리
        String[] words = text.split(" ");
        ArrayList<String> result = new ArrayList<>();

        for(int i = 0; i < words.length; i++){
            result.add(words[i]);
        }
        return result;
    }

    public int check_contraction(String word, int idx){ //check the data set of mapping.contraction
        Set<String> keys = mapping.contractions.keySet();
        for (String key : keys){
            if (word.substring(idx).startsWith(key)){
                braille += mapping.contractions.get(key);
                return key.length();
            }
        }
        return 0;
    }

    public Boolean check_contraction2(String Cho, String Jung, String Jong){
        // 초성 자음 + 약어 (억 || 언 || 얼 || 연 || 열 ... || 인)    등 검사
        ArrayList<String> jasoList= new ArrayList<>();
        jasoList.add("ㅇ");
        jasoList.add(Jung);
        jasoList.add(Jong);

        try{
            String hangul = HangulParser.assemble(jasoList);
            if (mapping.contractions.get(hangul)!=null){
                braille += mapping.CHOSUNG_letters.get(Cho);
                braille += mapping.contractions.get(hangul);
                return true;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public Boolean check_contraction3(String Cho, String Jung, String Jong){
        // 약어 (가 || 나 || 다 || 마 || 바 || 사 ... || 하 || 것) + 종성 자음    등 검사
        ArrayList<String> jasoList= new ArrayList<>();
        jasoList.add(Cho);
        jasoList.add(Jung);

        try{
            String hangul = HangulParser.assemble(jasoList);
            if (mapping.contractions.get(hangul)!=null){
                braille += mapping.contractions.get(hangul);
                braille += mapping.JONGSUNG_letters.get(Jong);
                return true;
            }
        }
        catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    public Boolean check_number(String word, int idx){  //check the data set of numbers
        if (mapping.numbers.get(String.valueOf(word.charAt(idx))) != null){ //숫자일 경우
            if (idx != 0){      //첫 글자 이후일 경우
                if (mapping.numbers.get(String.valueOf(word.charAt(idx-1))) != null){   //직전 글자가 숫자일 경우 수표 추가할 필요 x
                    braille += mapping.numbers.get(String.valueOf(word.charAt(idx)));
                }
                else{   //처음 시작하는 숫자일 시 수표 추가
                    braille += mapping.number_start + mapping.numbers.get(String.valueOf(word.charAt(idx)));
                }
            }
            else{   //첫 인덱스이며 숫자일 경우 수표 추가
                braille += mapping.number_start + mapping.numbers.get(String.valueOf(word.charAt(idx)));
            }
            return true;
        }
        return false;
    }

    public Boolean check_punctuation(String word, int idx){ //check the data set of punctuation
        if (mapping.punctuation.get(String.valueOf(word.charAt(idx)))!=null){
            braille += mapping.punctuation.get(String.valueOf(word.charAt(idx)));
            return true;
        }
        return false;
    }

    public Boolean check_character(String word, int idx){//check the data set of characters in all letters
        String[] keys = new String[1];
        Character key = word.charAt(idx);
        keys[0] = String.valueOf(key);
        int char_code, char1, char2, char3;

        if (Pattern.matches(".*[ㄱ-ㅎㅏ-ㅣ가-힣]+.*", keys[0])){

            char_code = (int)(keys[0].charAt(0)) - BASE_CODE;

            if (char_code >= 0){        //완전한 글자일 때
                char1 = (int)(char_code / CHOSUNG);
                char2 = (int)((char_code - (CHOSUNG * char1)) / JUNGSUNG);
                char3 = (int)((char_code - (CHOSUNG * char1) - (JUNGSUNG * char2)));

                if (!check_contraction2(CHOSUNG_LIST[char1], JUNGSUNG_LIST[char2], JONGSUNG_LIST[char3])){//늘 => ㄴ+'을' 과 같은 약어 경우 없을 시
                    if (!check_contraction3(CHOSUNG_LIST[char1], JUNGSUNG_LIST[char2], JONGSUNG_LIST[char3])){//감 => '가'+ㅁ 과 같은 약어 경우 없을 시

                        //약자, 약어없이 초,중,종 모든 파트 1:1 매핑
                        if (char3 == 0 && flag10 && CHOSUNG_LIST[char1].equals("ㅇ") && JUNGSUNG_LIST[char2].equals("ㅖ")){  //규정 제10항
                            braille += "⠤";
                            braille += mapping.CHOSUNG_letters.get(CHOSUNG_LIST[char1]);
                            braille += mapping.JUNGSUNG_letters.get(JUNGSUNG_LIST[char2]);
                            flag10 = false;
                        }
                        else{   //1:1 매핑 케이스
                            braille += mapping.CHOSUNG_letters.get(CHOSUNG_LIST[char1]);
                            braille += mapping.JUNGSUNG_letters.get(JUNGSUNG_LIST[char2]);
                            if (char3 != 0){    //종성 자음 존재할 경우만 추가
                                braille += mapping.JONGSUNG_letters.get(JONGSUNG_LIST[char3]);
                            }
                            else{
                                flag10 = true;
                            }
                        }


                    }
                }
                return true;
            }
            else{           //자음 혹은 모음 하나만 있을 때
                braille += mapping.CHOSUNG_start;
                if (mapping.CHOSUNG_letters.get(keys[0]) != null){      //초성 자음일 경우
                    braille += mapping.CHOSUNG_letters.get(keys[0]);
                }
                else{      //중성 모음일 경우
                    braille += mapping.JUNGSUNG_letters.get(keys[0]);
                }
                return true;
            }
        }
        return false;
    }

    public String translate(String text){   //Kor --> Braille
        String[] words_token = text.split("\n");

        for(String token : words_token){
            ArrayList<String> words = new ArrayList<>();

            words = extract_words(token);

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
                flag10 = false;
            }
            braille += "\n";
        }
        return braille;
    }

}
