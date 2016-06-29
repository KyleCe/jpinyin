package com.github.stuxuhai.jpinyin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public final class PinyinHelper {
    private static final Map<String, String> PINYIN_TABLE = getResource("/data/pinyin.db");
    private static final Map<String, String> MUTIL_PINYIN_TABLE = getResource("/data/mutil_pinyin.db");
    private static final String PINYIN_SEPARATOR = ","; // 拼音分隔符
    private static final char CHINESE_LING = '〇';
    private static final String ALL_UNMARKED_VOWEL = "aeiouv";
    private static final String ALL_MARKED_VOWEL = "āáǎàēéěèīíǐìōóǒòūúǔùǖǘǚǜ"; // 所有带声调的拼音字母

    private static final String CHINESE_REGEX = "[\\u4e00-\\u9fa5]";
    private static final Map<String, String> CHINESE_MAP = getResource("/data/chinese.db");

    private enum PinyinFormat {
        WITH_TONE_MARK, WITHOUT_TONE, WITH_TONE_NUMBER
    }

    /**
     * 获取字符串对应拼音的首字母
     *
     * @param str 需要转换的字符串
     * @return 对应拼音的首字母
     */
    public static String getShortPinyin(String str) {
        String separator = "#"; // 使用#作为拼音分隔符
        StringBuilder sb = new StringBuilder();

        char[] charArray = new char[str.length()];
        for (int i = 0, len = str.length(); i < len; i++) {
            char c = str.charAt(i);

            // 首先判断是否为汉字或者〇，不是的话直接将该字符返回
            if (!isChinese(c) && c != CHINESE_LING) {
                charArray[i] = c;
            } else {
                int j = i + 1;
                sb.append(c);

                // 搜索连续的汉字字符串
                while (j < len && (isChinese(str.charAt(j)) || str.charAt(j) == CHINESE_LING)) {
                    sb.append(str.charAt(j));
                    j++;
                }
                String hanziPinyin = convertToPinyinString(sb.toString(), separator, PinyinFormat.WITHOUT_TONE);
                String[] pinyinArray = hanziPinyin.split(separator);
                for (String string : pinyinArray) {
                    charArray[i] = string.charAt(0);
                    i++;
                }
                i--;
                sb.setLength(0);
            }
        }
        return String.valueOf(charArray);
    }

    /**
     * 将带声调格式的拼音转换为数字代表声调格式的拼音
     *
     * @param pinyinArrayString 带声调格式的拼音
     * @return 数字代表声调格式的拼音
     */
    private static String[] convertWithToneNumber(String pinyinArrayString) {
        String[] pinyinArray = pinyinArrayString.split(PINYIN_SEPARATOR);
        for (int i = pinyinArray.length - 1; i >= 0; i--) {
            boolean hasMarkedChar = false;
            String originalPinyin = pinyinArray[i].replace("ü", "v"); // 将拼音中的ü替换为v

            for (int j = originalPinyin.length() - 1; j >= 0; j--) {
                char originalChar = originalPinyin.charAt(j);

                // 搜索带声调的拼音字母，如果存在则替换为对应不带声调的英文字母
                if (originalChar < 'a' || originalChar > 'z') {
                    int indexInAllMarked = ALL_MARKED_VOWEL.indexOf(originalChar);
                    int toneNumber = indexInAllMarked % 4 + 1; // 声调数
                    char replaceChar = ALL_UNMARKED_VOWEL.charAt(((indexInAllMarked - indexInAllMarked % 4)) / 4);
                    pinyinArray[i] = originalPinyin.replace(String.valueOf(originalChar), String.valueOf(replaceChar)) + toneNumber;
                    hasMarkedChar = true;
                    break;
                }
            }
            if (!hasMarkedChar) {
                // 找不到带声调的拼音字母说明是轻声，用数字5表示
                pinyinArray[i] = originalPinyin + "5";
            }
        }

        return pinyinArray;
    }

    /**
     * 将带声调格式的拼音转换为不带声调格式的拼音
     *
     * @param pinyinArrayString 带声调格式的拼音
     * @return 不带声调的拼音
     */
    private static String[] convertWithoutTone(String pinyinArrayString) {
        String[] pinyinArray;
        for (int i = ALL_MARKED_VOWEL.length() - 1; i >= 0; i--) {
            char originalChar = ALL_MARKED_VOWEL.charAt(i);
            char replaceChar = ALL_UNMARKED_VOWEL.charAt(((i - i % 4)) / 4);
            pinyinArrayString = pinyinArrayString.replace(String.valueOf(originalChar), String.valueOf(replaceChar));
        }
        // 将拼音中的ü替换为v
        pinyinArray = pinyinArrayString.replace("ü", "v").split(PINYIN_SEPARATOR);

        // 去掉声调后的拼音可能存在重复，做去重处理
        LinkedHashSet<String> pinyinSet = new LinkedHashSet<String>();
        for (String pinyin : pinyinArray) {
            pinyinSet.add(pinyin);
        }

        return pinyinSet.toArray(new String[pinyinSet.size()]);
    }

    /**
     * 将带声调的拼音格式化为相应格式的拼音
     *
     * @param pinyinString 带声调的拼音
     * @param pinyinFormat 拼音格式：WITH_TONE_NUMBER--数字代表声调，WITHOUT_TONE--不带声调，WITH_TONE_MARK--带声调
     * @return 格式转换后的拼音
     */
    private static String[] formatPinyin(String pinyinString, PinyinFormat pinyinFormat) {
        if (pinyinFormat == PinyinFormat.WITH_TONE_MARK) {
            return pinyinString.split(PINYIN_SEPARATOR);
        } else if (pinyinFormat == PinyinFormat.WITH_TONE_NUMBER) {
            return convertWithToneNumber(pinyinString);
        } else if (pinyinFormat == PinyinFormat.WITHOUT_TONE) {
            return convertWithoutTone(pinyinString);
        }
        return new String[0];
    }

    /**
     * 将单个汉字转换为相应格式的拼音
     *
     * @param c            需要转换成拼音的汉字
     * @param pinyinFormat 拼音格式：WITH_TONE_NUMBER--数字代表声调，WITHOUT_TONE--不带声调，WITH_TONE_MARK--带声调
     * @return 汉字的拼音
     */
    public static String[] convertToPinyinArray(char c, PinyinFormat pinyinFormat) {
        String pinyin = PINYIN_TABLE.get(String.valueOf(c));
        if ((pinyin != null) && (!pinyin.equals("null"))) {
            return formatPinyin(pinyin, pinyinFormat);
        }
        return new String[0];
    }

    /**
     * 将字符串转换成相应格式的拼音
     *
     * @param str          需要转换的字符串
     * @param separator    拼音分隔符
     * @param pinyinFormat 拼音格式：WITH_TONE_NUMBER--数字代表声调，WITHOUT_TONE--不带声调，WITH_TONE_MARK--带声调
     * @return 字符串的拼音
     */
    public static String convertToPinyinString(String str, String separator, PinyinFormat pinyinFormat) {
        str = convertToSimplifiedChinese(str);
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = str.length(); i < len; i++) {
            char c = str.charAt(i);
            // 判断是否为汉字或者〇
            if (isChinese(c) || c == CHINESE_LING) {
                // 多音字识别处理
                boolean isFoundFlag = false;
                int rightMove = 3;

                // 将当前汉字依次与后面的3个、2个、1个汉字组合，判断下是否存在多音字词组
                for (int rightIndex = (i + rightMove) < len ? (i + rightMove) : (len - 1); rightIndex > i; rightIndex--) {
                    String cizu = str.substring(i, rightIndex + 1);
                    if (MUTIL_PINYIN_TABLE.containsKey(cizu)) {
                        String[] pinyinArray = formatPinyin(MUTIL_PINYIN_TABLE.get(cizu), pinyinFormat);
                        for (int j = 0, l = pinyinArray.length; j < l; j++) {
                            sb.append(pinyinArray[j]);
                            if (j < l - 1) {
                                sb.append(separator);
                            }
                        }
                        i = rightIndex;
                        isFoundFlag = true;
                        break;
                    }
                }
                if (!isFoundFlag) {
                    String[] pinyinArray = convertToPinyinArray(str.charAt(i), pinyinFormat);
                    if (pinyinArray != null) {
                        sb.append(pinyinArray[0]);
                    } else {
                        sb.append(str.charAt(i));
                    }
                }
                if (i < len - 1) {
                    sb.append(separator);
                }
            } else {
                sb.append(c);
                if ((i + 1) < len && isChinese(str.charAt(i + 1))) {
                    sb.append(separator);
                }
            }

        }
        return sb.toString();
    }

    protected static Map<String, String> getResource(String resourceName) {
        Map<String, String> map = new HashMap<String, String>();
        try {
            InputStream is = PinyinHelper.class.getResourceAsStream(resourceName);
            BufferedReader br = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String line = null;
            while ((line = br.readLine()) != null) {
                String[] tokens = line.trim().split("=");
                map.put(tokens[0], tokens[1]);
            }
            br.close();
        } catch (IOException e) {

        }

        return map;
    }

    public static char convertToSimplifiedChinese(char c) {
        String simplifiedChinese = CHINESE_MAP.get(String.valueOf(c));
        if (simplifiedChinese != null) {
            return simplifiedChinese.charAt(0);
        }
        return c;
    }

    public static String convertToSimplifiedChinese(String str) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0, len = str.length(); i < len; i++) {
            char c = str.charAt(i);
            sb.append(convertToSimplifiedChinese(c));
        }
        return sb.toString();
    }

    public static boolean isChinese(char c) {
        return '〇' == c || String.valueOf(c).matches(CHINESE_REGEX);
    }

    public static boolean containsChinese(String str) {
        for (int i = 0, len = str.length(); i < len; i++) {
            if (isChinese(str.charAt(i))) {
                return true;
            }
        }
        return false;
    }
}
