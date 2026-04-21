package edu.pnu.myspring.utils;

import java.io.IOException;
import java.util.*;

public class MyJsonParser {

    private static class JsonEscapeCharacterHandler{
        public String handle(String str){
            StringBuilder result = new StringBuilder();
            boolean isEscaping = false;
            for(int i=0;i<str.length();i++){
                char c = str.charAt(i);
                if(isEscaping){
                    switch(c){
                        case '\"':
                            result.append('\"');
                            break;
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

    private static JsonEscapeCharacterHandler escapeHandler = new JsonEscapeCharacterHandler();

    public MyJsonParser(){
    }

    public static HashMap<String, Object> parse(String jsonstring) throws Exception{
        return parseJSONString(jsonstring);
    }

    public static void main(String[] args) throws IOException {
        String json = readJSONStringFromKeyboard();
        HashMap<String, Object> map = parseJSONString(json);
        String jsonString = convertHashMapToJsonString(map);
        System.out.println(convertStringToPrettyJson(jsonString));
    }

    private static String readJSONStringFromKeyboard(){
        Scanner scanner = new Scanner(System.in);
        StringBuilder sb = new StringBuilder();

        while(scanner.hasNext()){
            sb.append(scanner.nextLine());
        }
        return sb.toString();
    }

    private static HashMap<String,Object> parseJSONString(String json){
        HashMap<String, Object> parsedMap = new HashMap<>();

        json = json.trim();
        if (json.startsWith("{") && json.endsWith("}")) {
            json = json.substring(1, json.length() - 1);
        }
        
        for(String keyValue : splitJson(json)){
            String[] tokens = keyValue.split(":",2);
            if (tokens.length < 2) continue;

            String key = tokens[0].trim();
            if (key.startsWith("\"") && key.endsWith("\"")) {
                key = key.substring(1, key.length()-1);
            }
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
            switch(c){
                case '{':
                    if(!isString) braces++;
                    break;
                case '}':
                    if(!isString) braces--;
                    break;
                case '[':
                    if(!isString) brackets++;
                    break;
                case ']':
                    if(!isString) brackets--;
                    break;
                case '"':
                    isString = !isString;
                    break;
            }

            if(brackets ==0 && braces == 0 && !isString && c == ','){
                sb.deleteCharAt(sb.length()-1);
                list.add(sb.toString().trim());
                sb.setLength(0);
            }
        }
        if (sb.length() > 0) {
            list.add(sb.toString().trim());
        }
        return list;
    }

    private static Object stringToObject(String value){
        value = value.trim();
        if (value.isEmpty()) return null;
        char c = value.charAt(0);

        switch (c){
            case '{':
                return parseJSONString(value);
            case '[':
                ArrayList<Object> valueList = new ArrayList<>();
                String inner = value.substring(1, value.length()-1);
                for(String token : splitJson(inner)){
                    valueList.add(stringToObject(token));
                }
                return valueList;
            case '"':
                return value.substring(1,value.length()-1);
            default:
                return parseIntOrDoubleOrBooleanOrRaw(value);
        }
    }

    private static Object parseIntOrDoubleOrBooleanOrRaw(String value){
        if ("true".equalsIgnoreCase(value)) return true;
        if ("false".equalsIgnoreCase(value)) return false;
        if ("null".equalsIgnoreCase(value)) return null;
        try{
            return Integer.parseInt(value);
        }catch (NumberFormatException e1){
            try{
                return Double.parseDouble(value);
            }catch (NumberFormatException e2){
                return value;
            }
        }
    }

    private static String convertHashMapToJsonString(HashMap<String, Object> map){
        StringBuilder sb = new StringBuilder("{");
        Map<String, Object> treeMap = getTreeMap(map);
        boolean first = true;
        for(String key : treeMap.keySet()){
            if (!first) sb.append(",");
            sb.append(String.format("\"%s\":%s", key, objectToString(map.get(key))));
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }

    public static <K,V> Map<K, V> getTreeMap(Map<K, V> hashMap){
        return new TreeMap<>(hashMap);
    }

    private static String objectToString(Object o){
        if(o == null) return "null";
        if(o instanceof ArrayList){
            StringBuilder sb =new StringBuilder();
            sb.append('[');
            ArrayList<?> list = (ArrayList<?>)o;
            for(int i=0; i<list.size(); i++){
                sb.append(objectToString(list.get(i)));
                if (i < list.size() - 1) sb.append(',');
            }
            sb.append(']');
            return sb.toString();
        }
        else if(o instanceof HashMap){
            return convertHashMapToJsonString((HashMap<String, Object>)o);
        }
        else if(o instanceof String){
            return "\"" + o.toString() + "\"";
        }
        else{
            return o.toString();
        }
    }

    private static String repeat(String str, int count) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(str);
        }
        return sb.toString();
    }

    private static String convertStringToPrettyJson(String jsonString){
        StringBuilder sb = new StringBuilder();
        int spaces = 0;
        boolean isString = false;
        for (int i = 0; i < jsonString.length(); i++) {
            char c = jsonString.charAt(i);
            if(!isString){
                switch (c){
                    case '{':
                    case '[':
                        spaces += 2;
                        sb.append(c);
                        sb.append('\n');
                        sb.append(repeat(" ", spaces));
                        break;
                    case '}':
                    case ']':
                        spaces -= 2;
                        sb.append('\n');
                        sb.append(repeat(" ", spaces));
                        sb.append(c);
                        break;
                    case '"':
                        isString = true;
                        sb.append(c);
                        break;
                    case ',':
                        sb.append(c);
                        sb.append('\n');
                        sb.append(repeat(" ", spaces));
                        break;
                    case ':':
                        sb.append(c);
                        sb.append(' ');
                        break;
                    default:
                        sb.append(c);
                        break;
                }
            }
            else{
                if(c == '"'){
                    isString = false;
                }
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
