package com.btsheng.erp.business.crm.hr.employee.util;

import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Predicate;

/**
 * HR 建档自动登录名：姓名全拼 + 随机 2 位数字；无法生成时回退 bts+工号。
 */
public final class LoginUsernameGenerator {

    private static final HanyuPinyinOutputFormat PINYIN_FORMAT = new HanyuPinyinOutputFormat();

    static {
        PINYIN_FORMAT.setCaseType(HanyuPinyinCaseType.LOWERCASE);
        PINYIN_FORMAT.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
    }

    private LoginUsernameGenerator() {
    }

    public static String generate(String name, String employeeNo, Predicate<String> usernameTaken) {
        String base = nameToLoginBase(name);
        if (base != null && !base.isBlank()) {
            for (int i = 0; i < 20; i++) {
                int suffix = ThreadLocalRandom.current().nextInt(10, 100);
                String candidate = trimUsername(base + suffix);
                if (candidate != null && (usernameTaken == null || !usernameTaken.test(candidate))) {
                    return candidate;
                }
            }
        }
        return btsEmployeeNoLogin(employeeNo);
    }

    static String nameToLoginBase(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (char c : name.trim().toCharArray()) {
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c >= 0x4E00 && c <= 0x9FFF) {
                try {
                    String[] py = PinyinHelper.toHanyuPinyinStringArray(c, PINYIN_FORMAT);
                    if (py != null && py.length > 0 && py[0] != null && !py[0].isBlank()) {
                        sb.append(py[0]);
                    }
                } catch (BadHanyuPinyinOutputFormatCombination ignored) {
                    // skip char
                }
            } else if (Character.isLetterOrDigit(c) || c == '_' || c == '.') {
                sb.append(Character.toLowerCase(c));
            }
        }
        if (sb.isEmpty()) {
            return null;
        }
        String base = sb.toString().replaceAll("[^a-z0-9._]", "");
        if (base.length() > 18) {
            base = base.substring(0, 18);
        }
        return base.isBlank() ? null : base;
    }

    static String btsEmployeeNoLogin(String employeeNo) {
        if (employeeNo == null || employeeNo.isBlank()) {
            return null;
        }
        String slug = employeeNo.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
        if (slug.isBlank()) {
            return null;
        }
        return trimUsername("bts" + slug);
    }

    private static String trimUsername(String username) {
        if (username == null || username.isBlank()) {
            return null;
        }
        String trimmed = username.toLowerCase();
        if (trimmed.length() > 20) {
            trimmed = trimmed.substring(0, 20);
        }
        return trimmed.length() >= 3 ? trimmed : null;
    }
}
