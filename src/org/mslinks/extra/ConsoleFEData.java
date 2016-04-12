/*
	https://github.com/BlackOverlord666/org.mslinks
	
	Copyright (c) 2015 Dmitrii Shamrikov

	Licensed under the WTFPL
	You may obtain a copy of the License at
 
	http://www.wtfpl.net/about/
 
	Unless required by applicable law or agreed to in writing, software
	distributed under the License is distributed on an "AS IS" BASIS,
	WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*/
package org.mslinks.extra;

import org.mslinks.io.ByteReader;
import org.mslinks.io.ByteWriter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import org.mslinks.Serializable;
import org.mslinks.ShellLinkException;

public class ConsoleFEData implements Serializable {
	public static final int signature = 0xA0000004;
	public static final int size = 0xc;
	
	private String lang;
	
	public ConsoleFEData() {
		Locale l = Locale.getDefault();
		lang = l.getLanguage() + "-" + l.getCountry();
	}
	
	public ConsoleFEData(ByteReader br, int sz) throws ShellLinkException, IOException {
		if (sz != size) throw new ShellLinkException();
		int t = (int)br.read4bytes();
		lang = ids.get(t >>> 16);
	}

	@Override
	public void serialize(ByteWriter bw) throws IOException {
		bw.write4bytes(size);
		bw.write4bytes(signature);
		bw.write4bytes(langs.get(lang) << 16);
	}
	
	public String getLanguage() { return lang; }
	public ConsoleFEData setLanguage(String s) { lang = s; return this;}
	
	private static HashMap<String, Integer> langs = new HashMap<String, Integer>() {{
		put("ar", 0x0001); put("bg", 0x0002); put("ca", 0x0003); put("zh-Hans", 0x0004); put("cs", 0x0005); put("da", 0x0006); put("de", 0x0007); put("el", 0x0008); put("en", 0x0009); put("es", 0x000a);
		put("fi", 0x000b); put("fr", 0x000c); put("he", 0x000d); put("hu", 0x000e); put("is", 0x000f); put("it", 0x0010); put("ja", 0x0011); put("ko", 0x0012); put("nl", 0x0013); put("no", 0x0014);
		put("pl", 0x0015); put("pt", 0x0016); put("rm", 0x0017); put("ro", 0x0018); put("ru", 0x0019); put("bs", 0x001a); put("hr", 0x001a); put("sr", 0x001a); put("sk", 0x001b); put("sq", 0x001c);
		put("sv", 0x001d); put("th", 0x001e); put("tr", 0x001f); put("ur", 0x0020); put("id", 0x0021); put("uk", 0x0022); put("be", 0x0023); put("sl", 0x0024); put("et", 0x0025); put("lv", 0x0026);
		put("lt", 0x0027); put("tg", 0x0028); put("fa", 0x0029); put("vi", 0x002a); put("hy", 0x002b); put("az", 0x002c); put("eu", 0x002d); put("dsb", 0x002e); put("hsb", 0x002e); put("mk", 0x002f);
		put("st", 0x0030); put("ts", 0x0031); put("tn", 0x0032); put("ve", 0x0033); put("xh", 0x0034); put("zu", 0x0035); put("af", 0x0036); put("ka", 0x0037); put("fo", 0x0038); put("hi", 0x0039);
		put("mt", 0x003a); put("se", 0x003b); put("ga", 0x003c); put("yi", 0x003d); put("ms", 0x003e); put("kk", 0x003f); put("ky", 0x0040); put("sw", 0x0041); put("tk", 0x0042); put("uz", 0x0043);
		put("tt", 0x0044); put("bn", 0x0045); put("pa", 0x0046); put("gu", 0x0047); put("or", 0x0048); put("ta", 0x0049); put("te", 0x004a); put("kn", 0x004b); put("ml", 0x004c); put("as", 0x004d);
		put("mr", 0x004e); put("sa", 0x004f); put("mn", 0x0050); put("bo", 0x0051); put("cy", 0x0052); put("km", 0x0053); put("lo", 0x0054); put("my", 0x0055); put("gl", 0x0056); put("kok", 0x0057);
		put("mni", 0x0058); put("sd", 0x0059); put("syr", 0x005a); put("si", 0x005b); put("chr", 0x005c); put("iu", 0x005d); put("am", 0x005e); put("tzm", 0x005f); put("ks", 0x0060); put("ne", 0x0061);
		put("fy", 0x0062); put("ps", 0x0063); put("fil", 0x0064); put("dv", 0x0065); put("bin", 0x0066); put("ff", 0x0067); put("ha", 0x0068); put("ibb", 0x0069); put("yo", 0x006a); put("quz", 0x006b);
		put("nso", 0x006c); put("ba", 0x006d); put("lb", 0x006e); put("kl", 0x006f); put("ig", 0x0070); put("kr", 0x0071); put("om", 0x0072); put("ti", 0x0073); put("gn", 0x0074); put("haw", 0x0075);
		put("la", 0x0076); put("so", 0x0077); put("ii", 0x0078); put("pap", 0x0079); put("arn", 0x007a); put("moh", 0x007c); put("br", 0x007e); put("ug", 0x0080); put("mi", 0x0081); put("oc", 0x0082);
		put("co", 0x0083); put("gsw", 0x0084); put("sah", 0x0085); put("qut", 0x0086); put("rw", 0x0087); put("wo", 0x0088); put("prs", 0x008c); put("gd", 0x0091); put("ku", 0x0092); put("quc", 0x0093);
		put("ar-SA", 0x0401); put("bg-BG", 0x0402); put("ca-ES", 0x0403); put("zh-TW", 0x0404); put("cs-CZ", 0x0405); put("da-DK", 0x0406); put("de-DE", 0x0407); put("el-GR", 0x0408); put("en-US", 0x0409);
		put("es-ES_tradnl", 0x040a); put("fi-FI", 0x040b); put("fr-FR", 0x040c); put("he-IL", 0x040d); put("hu-HU", 0x040e); put("is-IS", 0x040f); put("it-IT", 0x0410); put("ja-JP", 0x0411); put("ko-KR", 0x0412);
		put("nl-NL", 0x0413); put("nb-NO", 0x0414); put("pl-PL", 0x0415); put("pt-BR", 0x0416); put("rm-CH", 0x0417); put("ro-RO", 0x0418); put("ru-RU", 0x0419); put("hr-HR", 0x041a); put("sk-SK", 0x041b);
		put("sq-AL", 0x041c); put("sv-SE", 0x041d); put("th-TH", 0x041e); put("tr-TR", 0x041f); put("ur-PK", 0x0420); put("id-ID", 0x0421); put("uk-UA", 0x0422); put("be-BY", 0x0423); put("sl-SI", 0x0424);
		put("et-EE", 0x0425); put("lv-LV", 0x0426); put("lt-LT", 0x0427); put("tg-Cyrl-TJ", 0x0428); put("fa-IR", 0x0429); put("vi-VN", 0x042a); put("hy-AM", 0x042b); put("az-Latn-AZ", 0x042c); put("eu-ES", 0x042d);
		put("hsb-DE", 0x042e); put("mk-MK", 0x042f); put("st-ZA", 0x0430); put("ts-ZA", 0x0431); put("tn-ZA", 0x0432); put("ve-ZA", 0x0433); put("xh-ZA", 0x0434); put("zu-ZA", 0x0435); put("af-ZA", 0x0436);
		put("ka-GE", 0x0437); put("fo-FO", 0x0438); put("hi-IN", 0x0439); put("mt-MT", 0x043a); put("se-NO", 0x043b); put("yi-Hebr", 0x043d); put("ms-MY", 0x043e); put("kk-KZ", 0x043f); put("ky-KG", 0x0440);
		put("sw-KE", 0x0441); put("tk-TM", 0x0442); put("uz-Latn-UZ", 0x0443); put("tt-RU", 0x0444); put("bn-IN", 0x0445); put("pa-IN", 0x0446); put("gu-IN", 0x0447); put("or-IN", 0x0448); put("ta-IN", 0x0449);
		put("te-IN", 0x044a); put("kn-IN", 0x044b); put("ml-IN", 0x044c); put("as-IN", 0x044d); put("mr-IN", 0x044e); put("sa-IN", 0x044f); put("mn-MN", 0x0450); put("bo-CN", 0x0451); put("cy-GB", 0x0452);
		put("km-KH", 0x0453); put("lo-LA", 0x0454); put("my-MM", 0x0455); put("gl-ES", 0x0456); put("kok-IN", 0x0457); put("mni-IN", 0x0458); put("sd-Deva-IN", 0x0459); put("syr-SY", 0x045a); put("si-LK", 0x045b);
		put("chr-Cher-US", 0x045c); put("iu-Cans-CA", 0x045d); put("am-ET", 0x045e); put("tzm-Arab-MA", 0x045f); put("ks-Arab", 0x0460); put("ne-NP", 0x0461); put("fy-NL", 0x0462); put("ps-AF", 0x0463); put("fil-PH", 0x0464);
		put("dv-MV", 0x0465); put("bin-NG", 0x0466); put("fuv-NG", 0x0467); put("ha-Latn-NG", 0x0468); put("ibb-NG", 0x0469); put("yo-NG", 0x046a); put("quz-BO", 0x046b); put("nso-ZA", 0x046c); put("ba-RU", 0x046d);
		put("lb-LU", 0x046e); put("kl-GL", 0x046f); put("ig-NG", 0x0470); put("kr-NG", 0x0471); put("om-ET", 0x0472); put("ti-ET", 0x0473); put("gn-PY", 0x0474); put("haw-US", 0x0475); put("la-Latn", 0x0476);
		put("so-SO", 0x0477); put("ii-CN", 0x0478); put("pap-029", 0x0479); put("arn-CL", 0x047a); put("moh-CA", 0x047c); put("br-FR", 0x047e); put("ug-CN", 0x0480); put("mi-NZ", 0x0481); put("oc-FR", 0x0482);
		put("co-FR", 0x0483); put("gsw-FR", 0x0484); put("sah-RU", 0x0485); put("qut-GT", 0x0486); put("rw-RW", 0x0487); put("wo-SN", 0x0488); put("prs-AF", 0x048c); put("plt-MG", 0x048d); put("zh-yue-HK", 0x048e);
		put("tdd-Tale-CN", 0x048f); put("khb-Talu-CN", 0x0490); put("gd-GB", 0x0491); put("ku-Arab-IQ", 0x0492); put("quc-CO", 0x0493); put("qps-ploc", 0x0501); put("qps-ploca", 0x05fe); put("ar-IQ", 0x0801);
		put("ca-ES-valencia", 0x0803); put("zh-CN", 0x0804); put("de-CH", 0x0807); put("en-GB", 0x0809); put("es-MX", 0x080a); put("fr-BE", 0x080c); put("it-CH", 0x0810); put("ja-Ploc-JP", 0x0811); put("nl-BE", 0x0813);
		put("nn-NO", 0x0814); put("pt-PT", 0x0816); put("ro-MD", 0x0818); put("ru-MD", 0x0819); put("sr-Latn-CS", 0x081a); put("sv-FI", 0x081d); put("ur-IN", 0x0820); put("az-Cyrl-AZ", 0x082c); put("dsb-DE", 0x082e);
		put("tn-BW", 0x0832); put("se-SE", 0x083b); put("ga-IE", 0x083c); put("ms-BN", 0x083e); put("uz-Cyrl-UZ", 0x0843); put("bn-BD", 0x0845); put("pa-Arab-PK", 0x0846); put("ta-LK", 0x0849); put("mn-Mong-CN", 0x0850);
		put("bo-BT", 0x0851); put("sd-Arab-PK", 0x0859); put("iu-Latn-CA", 0x085d); put("tzm-Latn-DZ", 0x085f); put("ks-Deva", 0x0860); put("ne-IN", 0x0861); put("ff-Latn-SN", 0x0867); put("quz-EC", 0x086b); put("ti-ER", 0x0873);
		put("qps-plocm", 0x09ff); put("ar-EG", 0x0c01); put("zh-HK", 0x0c04); put("de-AT", 0x0c07); put("en-AU", 0x0c09); put("es-ES", 0x0c0a); put("fr-CA", 0x0c0c); put("sr-Cyrl-CS", 0x0c1a); put("se-FI", 0x0c3b);
		put("mn-Mong-MN", 0x0c50); put("tmz-MA", 0x0c5f); put("quz-PE", 0x0c6b); put("ar-LY", 0x1001); put("zh-SG", 0x1004); put("de-LU", 0x1007); put("en-CA", 0x1009); put("es-GT", 0x100a); put("fr-CH", 0x100c);
		put("hr-BA", 0x101a); put("smj-NO", 0x103b); put("tzm-Tfng-MA", 0x105f); put("ar-DZ", 0x1401); put("zh-MO", 0x1404); put("de-LI", 0x1407); put("en-NZ", 0x1409); put("es-CR", 0x140a); put("fr-LU", 0x140c);
		put("bs-Latn-BA", 0x141a); put("smj-SE", 0x143b); put("ar-MA", 0x1801); put("en-IE", 0x1809); put("es-PA", 0x180a); put("fr-MC", 0x180c); put("sr-Latn-BA", 0x181a); put("sma-NO", 0x183b); put("ar-TN", 0x1c01);
		put("en-ZA", 0x1c09); put("es-DO", 0x1c0a); put("sr-Cyrl-BA", 0x1c1a); put("sma-SE", 0x1c3b); put("ar-OM", 0x2001); put("en-JM", 0x2009); put("es-VE", 0x200a); put("fr-RE", 0x200c); put("bs-Cyrl-BA", 0x201a);
		put("sms-FI", 0x203b); put("ar-YE", 0x2401); put("en-029", 0x2409); put("es-CO", 0x240a); put("fr-CD", 0x240c); put("sr-Latn-RS", 0x241a); put("smn-FI", 0x243b); put("ar-SY", 0x2801); put("en-BZ", 0x2809);
		put("es-PE", 0x280a); put("fr-SN", 0x280c); put("sr-Cyrl-RS", 0x281a); put("ar-JO", 0x2c01); put("en-TT", 0x2c09); put("es-AR", 0x2c0a); put("fr-CM", 0x2c0c); put("sr-Latn-ME", 0x2c1a); put("ar-LB", 0x3001);
		put("en-ZW", 0x3009); put("es-EC", 0x300a); put("fr-CI", 0x300c); put("sr-Cyrl-ME", 0x301a); put("ar-KW", 0x3401); put("en-PH", 0x3409); put("es-CL", 0x340a); put("fr-ML", 0x340c); put("ar-AE", 0x3801);
		put("en-ID", 0x3809); put("es-UY", 0x380a); put("fr-MA", 0x380c); put("ar-BH", 0x3c01); put("en-HK", 0x3c09); put("es-PY", 0x3c0a); put("fr-HT", 0x3c0c); put("ar-QA", 0x4001); put("en-IN", 0x4009); put("es-BO", 0x400a);
		put("ar-Ploc-SA", 0x4401); put("en-MY", 0x4409); put("es-SV", 0x440a); put("ar-145", 0x4801); put("en-SG", 0x4809); put("es-HN", 0x480a); put("en-AE", 0x4c09); put("es-NI", 0x4c0a); put("en-BH", 0x5009);
		put("es-PR", 0x500a); put("en-EG", 0x5409); put("es-US", 0x540a); put("en-JO", 0x5809); put("es-419", 0x580a); put("en-KW", 0x5c09); put("en-TR", 0x6009); put("en-YE", 0x6409); put("bs-Cyrl", 0x641a);
		put("bs-Latn", 0x681a); put("sr-Cyrl", 0x6c1a); put("sr-Latn", 0x701a); put("smn", 0x703b); put("az-Cyrl", 0x742c); put("sms", 0x743b); put("zh", 0x7804); put("nn", 0x7814); put("bs", 0x781a); put("az-Latn", 0x782c);
		put("sma", 0x783b); put("uz-Cyrl", 0x7843); put("mn-Cyrl", 0x7850); put("iu-Cans", 0x785d); put("tzm-Tfng", 0x785f); put("zh-Hant", 0x7c04); put("nb", 0x7c14); put("sr", 0x7c1a); put("tg-Cyrl", 0x7c28); put("dsb", 0x7c2e);
		put("smj", 0x7c3b); put("uz-Latn", 0x7c43); put("pa-Arab", 0x7c46); put("mn-Mong", 0x7c50); put("sd-Arab", 0x7c59); put("chr-Cher", 0x7c5c); put("iu-Latn", 0x7c5d); put("tzm-Latn", 0x7c5f); put("ff-Latn", 0x7c67);
		put("ha-Latn", 0x7c68); put("ku-Arab", 0x7c92);
	}};
	
	private static HashMap<Integer, String> ids = new HashMap<>();
	
	static {
		for (String i : langs.keySet())
			ids.put(langs.get(i), i);
	}
}
