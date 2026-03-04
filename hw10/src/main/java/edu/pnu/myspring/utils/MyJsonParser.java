package edu.pnu.myspring.utils;

import java.io.IOException;
import java.util.*;

//parsejsonstring->(arraylist 반환)splitjson,stringtoobject->(hashmap으로 파싱해야할 경우)parsejsonstirng,(쉼표 자르는 경우)splitJson
//converthashmaptostring->objecttostring->objecttostring(list일때), converthashmaptostring(hashmap일때)
public class MyJsonParser {
    public static class JsonEscapeCharacterHandler {
        public String handleEscapedCharacters(String str) {

            StringBuilder result = new StringBuilder();

            boolean isEscaping = false;

            for (int i = 0; i < str.length(); i++) {

                char c = str.charAt(i);
                if(isEscaping){
                    switch (c){
                        case '\\':
                            result.append('\\');
                            break;
                        case '/':
                            result.append('/');
                            break;
                        case 'b':
                            result.append('\b');
                            break;
                        case 'f':
                            result.append('\f');
                            break;
                        case 'n':
                            result.append('\n');
                            break;
                        case 'r':
                            result.append('\r');
                            break;
                        case 't':
                            result.append('\t');
                            break;
                        case 'u':
                            String unicode = str.substring(i+1,i+5);
                            i+=4;
                            int ch =  Integer.parseInt(unicode, 16);
                            result.append((char)ch);
                            break;
                    }
                    isEscaping = false;
                }
                else{
                    if(c=='\\'){
                        isEscaping = true;
                    }
                    else{
                        result.append(c);
                    }
                }

            }


            return result.toString();
        }
    }

    private static JsonEscapeCharacterHandler escapeHandler;
    public MyJsonParser(){
        escapeHandler = new JsonEscapeCharacterHandler();
    }
    public static HashMap<String, Object> parse(String jsonstring) throws Exception{
        return parseJSONString(jsonstring);
    }
    public static void main(String[] args)throws IOException {
        String json = readJSONStringFromKeyboard();
        var map = parseJSONString(json);//String to HashMap
        var jsonString =convertHashMapToJsonString(map);
        System.out.println(convertStringToPrettyJson(jsonString));
        /*var jsonString = convertHashMapToJsonString(map);//StringBuilder 생성하여
        key, value에 "붙여서 string 빌드, objecttoString 함수 따로 만들어서 재귀형식으로, treemap사용
        System.out.println(convertStringToPrettyJson(jsonString));
        //띄워쓰기, 개행등 적용하는 함수 괄호 닫을때 스택 적용해서 깎고, 올리는 식 문자열일때와 문자열이 아닐때 구분해서 처리
        */
    }
    //키보드로부터 EOF까지 입력받기

    private static String readJSONStringFromKeyboard(){
        Scanner scanner = new Scanner(System.in);
        StringBuilder sb = new StringBuilder(); //한정적

        while(scanner.hasNext()){
            sb.append(scanner.nextLine());
        }
        return sb.toString();
    }
    //컨트롤 d쓰면 멈춤
    //중괄호, 대괄호 스택 만듬. 쉼표가 왔는데 스택이 0 0이고 문자열도 아니라면 제거하고 리스트에 추가
    //key와 value를 : 기준으로 나눔.
    //try catch
    private static HashMap<String,Object> parseJSONString(String json){//콤마 예외 array, :예외 시간
        HashMap<String, Object> parsedMap = new HashMap<>();

        json = json.substring(1, json.length() - 1);
        for(String keyValue : splitJson(json)){
            String[] tokens = keyValue.split(":",2);

            String key = tokens[0].trim();
            key = key.substring(1,key.length()-1);
            Object value = stringToObject(tokens[1]);

            parsedMap.put(key, value);
        }
        return parsedMap;
    }
    private static ArrayList<String> splitJson(String json){
        json = json.trim();
        ArrayList<String> list = new ArrayList<>();
        StringBuilder sb = new StringBuilder();

        int brackets = 0;
        int braces = 0;
        boolean isString = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            sb.append(c);

            switch (c){
                case '{':
                    brackets++;
                    break;
                case '}':
                    brackets--;
                    break;
                case '[':
                    braces++;
                    break;
                case ']':
                    braces--;
                    break;
                case '"':
                    isString = !isString;
                    break;
            }

            if(brackets ==0 && braces == 0 && !isString && c == ','){
                sb.deleteCharAt(sb.length()-1);
                list.add(sb.toString());
                sb.setLength(0);
            }
        }
        list.add(sb.toString());
        return list;
    }
    private static Object stringToObject(String value){
        value = value.trim();
        char c = value.charAt(0);

        switch (c){
            case '{' -> {
                return parseJSONString(value);
            }
            case '[' ->{
                ArrayList<Object> valueList = new ArrayList<>();
                value = value.substring(1, value.length()-1);
                for(String token : splitJson(value)){
                    valueList.add(stringToObject(token));
                }
                return valueList;
            }
            case '"'->{
                return value.substring(1,value.length()-1);
            }
            default -> {
                return parseIntOrDoubleOrBooleanOrRaw(value);
            }
        }
    }
    private static Object parseIntOrDoubleOrBooleanOrRaw(String value){
        try{
            return Integer.parseInt(value);
        }catch (NumberFormatException e1){
            try{
                return Double.parseDouble(value);
            }catch (NumberFormatException e2){
                try{
                    return Boolean.parseBoolean(value);
                }catch (Exception e3){
                    e3.printStackTrace();
                    return value;
                }
            }
        }
    }

    private static String convertHashMapToJsonString(HashMap<String, Object> map){
        StringBuilder sb = new StringBuilder("{");
        for(String key : getTreeMap(map).keySet()){
            sb.append(String.format("\"%s\":%s,", key, objectToString(map.get(key))));
        }
        sb.deleteCharAt(sb.length()-1);
        sb.append('}');
        return sb.toString();
    }
    public static <K,V> Map<K, V> getTreeMap(Map<K, V> hashMap){
        Map<K, V> treeMap = new TreeMap<>();
        for(Map.Entry<K,V> kvEntry : hashMap.entrySet()){
            treeMap.put(kvEntry.getKey(), kvEntry.getValue());
        }
        return treeMap;
    }
    private static String objectToString(Object o){
        if(o instanceof ArrayList){
            StringBuilder sb =new StringBuilder();
            sb.append('[');
            for(Object value : (ArrayList<Object>)o){
                sb.append(objectToString(value));
                sb.append(',');
            }
            sb.deleteCharAt(sb.length()-1);
            sb.append(']');
            return sb.toString();
        }
        else if(o instanceof HashMap){
            return convertHashMapToJsonString((HashMap<String, Object>)o);
        }
        else if(o instanceof String){
            return String.format("\"%s\"", o);
        }
        else{
            return o.toString();
        }
    }
    private static String convertStringToPrettyJson(String jsonString){
        StringBuilder sb = new StringBuilder();
        int spaces = 0;
        boolean isString = false;
        for (int i = 0; i < jsonString.length(); i++) {
            char c = jsonString.charAt(i);
            if(!isString){
                switch (c){
                    case'{','['->{
                        spaces += 2;
                        sb.append(c);
                        sb.append('\n');
                        sb.append(" ".repeat(spaces));
                    }
                    case '}', ']'->{
                        spaces -= 2;
                        if(spaces == -2) System.out.println(sb.toString());
                        sb.append('\n');
                        sb.append(" ".repeat(spaces));
                        sb.append(c);
                    }
                    case '"'->{
                        isString = false;
                        sb.append(c);
                    }
                    case ',' ->{
                        sb.append(c);
                        sb.append('\n');
                        sb.append(" ".repeat(spaces));
                    }
                    case ':'->{
                        sb.append(c);
                        sb.append(' ');
                    }
                    default -> sb.append(c);
                }
            }
            else{
                if(c == '"'){
                    isString = true;
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }



}


