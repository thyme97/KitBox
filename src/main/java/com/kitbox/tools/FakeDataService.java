package com.kitbox.tools;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 假测试数据生成：全部由随机算法合成，仅供开发/测试环境填充使用，
 * 不包含任何真实个人信息。身份证号与银行卡号为「校验位合法」的格式数据。
 */
public final class FakeDataService {

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * 省 → 市 → 区县代码（GB/T 2260 区县级行政区划代码，均真实存在，
     * 直辖市按惯例用「市辖区」作为城市层）。
     */
    private static final Map<String, Map<String, List<String>>> AREA_TABLE = new LinkedHashMap<>();
    private static List<String> allAreaCodes;

    private static void area(String province, String city, String... districts) {
        AREA_TABLE.computeIfAbsent(province, k -> new LinkedHashMap<>())
                .put(city, Collections.unmodifiableList(Arrays.asList(districts)));
    }

    static {
        area("北京市", "市辖区", "110101 东城区", "110102 西城区", "110105 朝阳区", "110106 丰台区", "110108 海淀区");
        area("天津市", "市辖区", "120101 和平区", "120102 河东区", "120103 河西区", "120104 南开区");
        area("河北省", "石家庄市", "130102 长安区", "130104 桥西区", "130105 新华区");
        area("河北省", "唐山市", "130202 路南区", "130203 路北区");
        area("山西省", "太原市", "140105 迎泽区", "140107 杏花岭区");
        area("内蒙古自治区", "呼和浩特市", "150102 新城区", "150103 回民区", "150104 玉泉区");
        area("辽宁省", "沈阳市", "210102 和平区", "210103 沈河区", "210106 铁西区");
        area("辽宁省", "大连市", "210202 中山区", "210203 西岗区");
        area("吉林省", "长春市", "220102 南关区", "220104 朝阳区");
        area("吉林省", "吉林市", "220202 昌邑区");
        area("黑龙江省", "哈尔滨市", "230102 道里区", "230103 南岗区", "230104 道外区");
        area("上海市", "市辖区", "310101 黄浦区", "310104 徐汇区", "310105 长宁区", "310106 静安区", "310115 浦东新区");
        area("江苏省", "南京市", "320102 玄武区", "320104 秦淮区", "320106 鼓楼区");
        area("江苏省", "苏州市", "320506 吴中区", "320508 姑苏区");
        area("浙江省", "杭州市", "330102 上城区", "330106 西湖区", "330108 滨江区");
        area("浙江省", "宁波市", "330203 海曙区", "330205 江北区");
        area("浙江省", "温州市", "330302 鹿城区");
        area("安徽省", "合肥市", "340103 庐阳区", "340104 蜀山区");
        area("福建省", "福州市", "350102 鼓楼区", "350103 台江区");
        area("福建省", "厦门市", "350203 思明区", "350206 湖里区");
        area("江西省", "南昌市", "360102 东湖区", "360103 西湖区");
        area("山东省", "济南市", "370102 历下区", "370103 市中区");
        area("山东省", "青岛市", "370202 市南区", "370203 市北区", "370212 崂山区");
        area("河南省", "郑州市", "410102 中原区", "410103 二七区", "410105 金水区");
        area("河南省", "洛阳市", "410302 老城区");
        area("湖北省", "武汉市", "420102 江岸区", "420106 武昌区", "420111 洪山区");
        area("湖北省", "宜昌市", "420502 西陵区");
        area("湖南省", "长沙市", "430102 芙蓉区", "430103 天心区", "430104 岳麓区");
        area("广东省", "广州市", "440103 荔湾区", "440104 越秀区", "440105 海珠区", "440106 天河区");
        area("广东省", "深圳市", "440303 罗湖区", "440304 福田区", "440305 南山区", "440306 宝安区", "440307 龙岗区");
        area("广西壮族自治区", "南宁市", "450102 兴宁区", "450103 青秀区");
        area("广西壮族自治区", "桂林市", "450302 秀峰区");
        area("海南省", "海口市", "460105 秀英区", "460106 龙华区");
        area("重庆市", "市辖区", "500103 渝中区", "500105 江北区", "500106 沙坪坝区", "500108 南岸区");
        area("四川省", "成都市", "510104 锦江区", "510105 青羊区", "510106 金牛区", "510107 武侯区");
        area("四川省", "绵阳市", "510703 涪城区");
        area("贵州省", "贵阳市", "520102 南明区", "520103 云岩区");
        area("云南省", "昆明市", "530102 五华区", "530103 盘龙区");
        area("西藏自治区", "拉萨市", "540102 城关区");
        area("陕西省", "西安市", "610102 新城区", "610103 碑林区", "610113 雁塔区");
        area("甘肃省", "兰州市", "620102 城关区", "620103 七里河区");
        area("青海省", "西宁市", "630102 城东区", "630103 城中区");
        area("宁夏回族自治区", "银川市", "640103 兴庆区");
        area("新疆维吾尔自治区", "乌鲁木齐市", "650102 天山区", "650103 沙依巴克区", "650104 新市区");
    }

    private static final String[] PHONE_PREFIXES = {
            "133", "135", "136", "137", "138", "139", "147", "150", "151", "152",
            "155", "156", "157", "158", "159", "166", "176", "177", "178", "180",
            "181", "182", "183", "185", "186", "187", "188", "189", "191", "198",
            "199"
    };

    private static final String SURNAMES =
            "王李张刘陈杨赵黄周吴徐孙马朱胡郭何林罗高郑梁谢宋唐许韩冯邓曹彭曾肖田董潘袁蔡蒋余杜叶程苏魏吕丁任沈姚卢姜崔钟谭陆汪范金石廖贾夏方白邹孟熊秦邱江尹薛段雷侯龙陶贺顾毛郝龚邵万钱严武戴莫孔向";

    private static final String GIVEN_CHARS =
            "伟芳娜敏静丽强磊军洋勇艳杰娟涛明超霞平刚华玉红梅晓东文博欣怡浩宇轩然佳琪慧颖志远建国立小晨曦子涵思梦洁雪松海燕兰春秋实冬光辉荣富强文明礼智信雅静安和平顺康泰昌盛";

    private static final String[] MAIL_DOMAINS = {
            "example.com", "example.org", "test.com", "163.com", "126.com",
            "qq.com", "gmail.com", "outlook.com"
    };

    private FakeDataService() {
    }

    // ---------------- 属地（省/市/区） ----------------

    /** 全部省份名（有序）。 */
    public static List<String> provinces() {
        return new ArrayList<>(AREA_TABLE.keySet());
    }

    /** 指定省份下的城市名（有序）。 */
    public static List<String> citiesOf(String province) {
        Map<String, List<String>> cities = AREA_TABLE.get(province);
        return cities == null ? new ArrayList<>() : new ArrayList<>(cities.keySet());
    }

    /**
     * 区县名 → 代码 的映射（有序）。city 为 null 时返回该省全部区县。
     */
    public static Map<String, String> districtsOf(String province, String city) {
        Map<String, String> merged = new LinkedHashMap<>();
        if (city != null) {
            List<String> list = AREA_TABLE.getOrDefault(province, Collections.emptyMap()).get(city);
            if (list != null) {
                for (String entry : list) {
                    merged.put(entry.substring(7), entry.substring(0, 6));
                }
            }
            return merged;
        }
        for (List<String> list : AREA_TABLE.getOrDefault(province, Collections.emptyMap()).values()) {
            for (String entry : list) {
                merged.put(entry.substring(7), entry.substring(0, 6));
            }
        }
        return merged;
    }

    /** 全部区县代码（供完全随机）。 */
    private static List<String> allAreaCodes() {
        if (allAreaCodes == null) {
            List<String> all = new ArrayList<>();
            for (Map<String, List<String>> cities : AREA_TABLE.values()) {
                for (List<String> list : cities.values()) {
                    for (String entry : list) {
                        all.add(entry.substring(0, 6));
                    }
                }
            }
            allAreaCodes = Collections.unmodifiableList(all);
        }
        return allAreaCodes;
    }

    // ---------------- 生成 ----------------

    /** 18 位身份证号（校验位合法，GB 11643-1999 MOD 11-2）。areaCode 为空时随机取属地。 */
    public static String idCard(String areaCode) {
        String area = areaCode == null || areaCode.isEmpty()
                ? allAreaCodes().get(RANDOM.nextInt(allAreaCodes().size()))
                : areaCode;
        int year = 1955 + RANDOM.nextInt(50);
        int month = 1 + RANDOM.nextInt(12);
        int day = 1 + RANDOM.nextInt(daysInMonth(year, month));
        String birth = String.format("%04d%02d%02d", year, month, day);
        String seq = String.format("%03d", 1 + RANDOM.nextInt(998));
        String body = area + birth + seq;
        return body + checkDigit(body);
    }

    /** 18 位身份证号（属地完全随机）。 */
    public static String idCard() {
        return idCard(null);
    }

    /** 身份证号校验位（前 17 位 → 第 18 位） */
    public static char checkDigit(String body17) {
        int[] weights = {7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2};
        char[] map = {'1', '0', 'X', '9', '8', '7', '6', '5', '4', '3', '2'};
        int sum = 0;
        for (int i = 0; i < 17; i++) {
            sum += (body17.charAt(i) - '0') * weights[i];
        }
        return map[sum % 11];
    }

    /** 11 位手机号 */
    public static String phone() {
        String prefix = PHONE_PREFIXES[RANDOM.nextInt(PHONE_PREFIXES.length)];
        StringBuilder sb = new StringBuilder(prefix);
        for (int i = 0; i < 8; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    /** 中文姓名（常见姓氏 + 1~2 字名） */
    public static String name() {
        char surname = SURNAMES.charAt(RANDOM.nextInt(SURNAMES.length()));
        StringBuilder sb = new StringBuilder().append(surname);
        int given = RANDOM.nextInt(10) < 8 ? 2 : 1;
        for (int i = 0; i < given; i++) {
            sb.append(GIVEN_CHARS.charAt(RANDOM.nextInt(GIVEN_CHARS.length())));
        }
        return sb.toString();
    }

    /** 邮箱地址（随机字母数字本地部分 + 常见域名） */
    public static String email() {
        StringBuilder sb = new StringBuilder();
        int len = 6 + RANDOM.nextInt(5);
        for (int i = 0; i < len; i++) {
            if (i == 0 || RANDOM.nextInt(3) == 0) {
                sb.append((char) ('a' + RANDOM.nextInt(26)));
            } else {
                sb.append(RANDOM.nextInt(10));
            }
        }
        return sb.append('@')
                .append(MAIL_DOMAINS[RANDOM.nextInt(MAIL_DOMAINS.length)]).toString();
    }

    /** 银行卡号（62 开头银联段，16 或 19 位，Luhn 校验位合法） */
    public static String bankCard() {
        int len = RANDOM.nextBoolean() ? 16 : 19;
        StringBuilder body = new StringBuilder("62");
        for (int i = 2; i < len - 1; i++) {
            body.append(RANDOM.nextInt(10));
        }
        return body.append(luhnCheckDigit(body)).toString();
    }

    /** Luhn 校验位（对 body 追加一位后整体通过 Luhn 校验） */
    public static int luhnCheckDigit(CharSequence body) {
        int sum = 0;
        boolean doubling = true; // 从最右位开始，追加位前 body 末位相当于倍增位
        for (int i = body.length() - 1; i >= 0; i--) {
            int d = body.charAt(i) - '0';
            if (doubling) {
                d *= 2;
                if (d > 9) {
                    d -= 9;
                }
            }
            sum += d;
            doubling = !doubling;
        }
        return (10 - sum % 10) % 10;
    }

    /** IPv4 地址（约 1/4 概率为私有网段） */
    public static String ipv4() {
        int a;
        if (RANDOM.nextInt(4) == 0) {
            int which = RANDOM.nextInt(3);
            if (which == 0) {
                return "10." + rand255() + "." + rand255() + "." + randHost();
            }
            if (which == 1) {
                return "172." + (16 + RANDOM.nextInt(16)) + "." + rand255() + "." + randHost();
            }
            return "192.168." + rand255() + "." + randHost();
        }
        do {
            a = 1 + RANDOM.nextInt(223);
        } while (a == 127);
        return a + "." + rand255() + "." + rand255() + "." + randHost();
    }

    /** MAC 地址（本地管理位，避免撞真实厂商 OUI） */
    public static String macAddress() {
        StringBuilder sb = new StringBuilder();
        int first = (RANDOM.nextInt(64) << 2) | 0x02;
        sb.append(String.format("%02X", first));
        for (int i = 1; i < 6; i++) {
            sb.append(':').append(String.format("%02X", RANDOM.nextInt(256)));
        }
        return sb.toString();
    }

    private static int rand255() {
        return RANDOM.nextInt(256);
    }

    private static int randHost() {
        return 1 + RANDOM.nextInt(254);
    }

    private static int daysInMonth(int year, int month) {
        if (month == 2) {
            boolean leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
            return leap ? 29 : 28;
        }
        if (month == 4 || month == 6 || month == 9 || month == 11) {
            return 30;
        }
        return 31;
    }
}
