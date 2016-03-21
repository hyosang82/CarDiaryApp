package kr.hyosang.drivediary.client;

public class Util {
    
    public static int parseInt(String str, int def) {
        try {
            return Integer.parseInt(str, 10);
        }catch(NumberFormatException e) {
            return def;
        }
    }
    
    public static double parseDouble(String str, double def) {
        try {
            return Double.parseDouble(str);
        }catch(NumberFormatException e) {
            return def;
        }
    }
}
