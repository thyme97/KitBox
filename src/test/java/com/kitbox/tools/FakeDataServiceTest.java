package com.kitbox.tools;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FakeDataServiceTest {

    private static final int SAMPLES = 200;

    @Test
    void idCardFormatAndChecksum() {
        Pattern p = Pattern.compile("\\d{17}[\\dX]");
        for (int i = 0; i < SAMPLES; i++) {
            String id = FakeDataService.idCard();
            assertTrue(p.matcher(id).matches(), "格式不合法：" + id);
            int province = (id.charAt(0) - '0') * 10 + (id.charAt(1) - '0');
            assertTrue(province >= 11 && province <= 65, "省份代码不合法：" + id);
            int month = Integer.parseInt(id.substring(10, 12));
            int day = Integer.parseInt(id.substring(12, 14));
            assertTrue(month >= 1 && month <= 12, "月份不合法：" + id);
            assertTrue(day >= 1 && day <= 31, "日期不合法：" + id);
            assertEquals(FakeDataService.checkDigit(id.substring(0, 17)), id.charAt(17),
                    "校验位不正确：" + id);
        }
    }

    @Test
    void idCardWithAreaCode() {
        // 指定区县代码：生成号码属地固定为该区
        for (String code : new String[]{"310104", "440304", "110101"}) {
            for (int i = 0; i < 50; i++) {
                String id = FakeDataService.idCard(code);
                assertTrue(id.startsWith(code), "属地不符：" + id);
                assertEquals(FakeDataService.checkDigit(id.substring(0, 17)), id.charAt(17));
            }
        }
        // 完全随机：属地必须在码表内
        java.util.Set<String> all = new java.util.HashSet<>();
        for (String province : FakeDataService.provinces()) {
            for (String city : FakeDataService.citiesOf(province)) {
                all.addAll(FakeDataService.districtsOf(province, city).values());
            }
        }
        assertTrue(all.size() >= 60, "码表规模不足：" + all.size());
        for (int i = 0; i < 100; i++) {
            String id = FakeDataService.idCard();
            assertTrue(all.contains(id.substring(0, 6)), "属地不在码表：" + id);
        }
        // 三级联动数据：市级查区县、省级合并区县均非空
        List<String> provinces = FakeDataService.provinces();
        Map<String, String> cityDistricts =
                FakeDataService.districtsOf(provinces.get(0), FakeDataService.citiesOf(provinces.get(0)).get(0));
        assertTrue(!cityDistricts.isEmpty());
        Map<String, String> provinceDistricts = FakeDataService.districtsOf(provinces.get(0), null);
        assertTrue(provinceDistricts.size() >= cityDistricts.size());
    }

    @Test
    void knownIdVectorChecksum() {
        // 公开示例号段 11010519491231002X：独立手工验算校验位
        assertEquals('X', FakeDataService.checkDigit("11010519491231002"));
        // 顺序号 000 与 999 不生成（1~998），出生年范围 1955~2004
        for (int i = 0; i < SAMPLES; i++) {
            String id = FakeDataService.idCard();
            int year = Integer.parseInt(id.substring(6, 10));
            assertTrue(year >= 1955 && year <= 2004);
            int seq = Integer.parseInt(id.substring(14, 17));
            assertTrue(seq >= 1 && seq <= 998);
        }
    }

    @Test
    void phoneFormat() {
        Pattern p = Pattern.compile("1[3-9]\\d{9}");
        String[] allowed = {"133", "135", "136", "137", "138", "139", "147", "150", "151", "152",
                "155", "156", "157", "158", "159", "166", "176", "177", "178", "180", "181",
                "182", "183", "185", "186", "187", "188", "189", "191", "198", "199"};
        for (int i = 0; i < SAMPLES; i++) {
            String phone = FakeDataService.phone();
            assertTrue(p.matcher(phone).matches(), "格式不合法：" + phone);
            boolean prefixOk = false;
            for (String prefix : allowed) {
                if (phone.startsWith(prefix)) {
                    prefixOk = true;
                    break;
                }
            }
            assertTrue(prefixOk, "号段不在在网列表：" + phone);
        }
    }

    @Test
    void nameFormat() {
        for (int i = 0; i < SAMPLES; i++) {
            String name = FakeDataService.name();
            assertTrue(name.length() >= 2 && name.length() <= 3, "姓名长度不合法：" + name);
            assertTrue(name.chars().allMatch(c -> c >= 0x4E00 && c <= 0x9FA5), "包含非汉字：" + name);
        }
    }

    @Test
    void emailFormat() {
        Pattern p = Pattern.compile("[a-z][a-z0-9]{5,13}@(example\\.com|example\\.org|test\\.com|163\\.com|126\\.com|qq\\.com|gmail\\.com|outlook\\.com)");
        for (int i = 0; i < SAMPLES; i++) {
            assertTrue(p.matcher(FakeDataService.email()).matches());
        }
    }

    @Test
    void bankCardLuhn() {
        Pattern p = Pattern.compile("62\\d{14}(\\d{3})?");
        for (int i = 0; i < SAMPLES; i++) {
            String card = FakeDataService.bankCard();
            assertTrue(p.matcher(card).matches(), "格式不合法：" + card);
            assertTrue(card.length() == 16 || card.length() == 19);
            assertTrue(luhnValid(card), "Luhn 校验未通过：" + card);
        }
    }

    private static boolean luhnValid(String card) {
        int sum = 0;
        boolean doubling = false;
        for (int i = card.length() - 1; i >= 0; i--) {
            int d = card.charAt(i) - '0';
            if (doubling) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubling = !doubling;
        }
        return sum % 10 == 0;
    }

    @Test
    void ipv4Format() {
        Pattern p = Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");
        for (int i = 0; i < SAMPLES; i++) {
            String ip = FakeDataService.ipv4();
            java.util.regex.Matcher m = p.matcher(ip);
            assertTrue(m.matches(), "格式不合法：" + ip);
            for (int g = 1; g <= 4; g++) {
                int octet = Integer.parseInt(m.group(g));
                assertTrue(octet >= 0 && octet <= 255, "八位组越界：" + ip);
            }
            int a = Integer.parseInt(m.group(1));
            assertTrue(a >= 1 && a <= 223 && a != 127, "首八位组不合法：" + ip);
        }
    }

    @Test
    void macFormatAndLocalBit() {
        Pattern p = Pattern.compile("[0-9A-F]{2}(:[0-9A-F]{2}){5}");
        for (int i = 0; i < SAMPLES; i++) {
            String mac = FakeDataService.macAddress();
            assertTrue(p.matcher(mac).matches(), "格式不合法：" + mac);
            int first = Integer.parseInt(mac.substring(0, 2), 16);
            assertEquals(2, first & 0x02, "未置本地管理位：" + mac);
        }
    }
}
