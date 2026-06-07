package qz.integration.services;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;

public class TcvnCharsetTests {

	@SuppressWarnings("all")
	private static final String CHARSET_NAME = "TCVN-3-1";

	@Test
	public void testCharsetAvailability() {
		try {
			Charset cs = Charset.forName(CHARSET_NAME);
			Assert.assertNotNull(cs);
		} catch (UnsupportedCharsetException e) {
			Assert.fail(CHARSET_NAME + " charset provider is not registered or supported in this runtime.");
		}
	}

	@DataProvider(name = "tcvnData")
	public Object[][] tcvnData() {
		return new Object[][] {
			// Basic ASCII characters (Should be a 1:1 mapping in TCVN-3)
			{
				"Viet Nam",
				new byte[] {
					(byte) 0x56,
					(byte) 0x69,
					(byte) 0x65,
					(byte) 0x74,
					(byte) 0x20,
					(byte) 0x4E,
					(byte) 0x61,
					(byte) 0x6D
				}
			},
			// Literal Vietnamese input to 8-bit TCVN byte equivalents
			{
				"Tiếng Việt Nam",
				new byte[] {
					(byte) 0x54, // "Tiếng"
					(byte) 0x69,
					(byte) 0xD5,
					(byte) 0x6E,
					(byte) 0x67,
					(byte) 0x20, // " "
					(byte) 0x56, // "Việt"
					(byte) 0x69,
					(byte) 0xD6,
					(byte) 0x74,
					(byte) 0x20, // " "
					(byte) 0x4E, // "Nam"
					(byte) 0x61,
					(byte) 0x6D
				}
			},
			// Empty string should return an empty byte array
			{
				"",
				new byte[] {}
			}
		};
	}

	@DataProvider(name = "invalidCharacterData")
	public Object[][] invalidCharacterData() {
		return new Object[][] {
			// Kanji characters should fall back to '?' (0x3F) once for each character
			{
				"日本語",
				new byte[] { (byte) 0x3F, (byte) 0x3F, (byte) 0x3F }
			},
			// Cyrillic characters should be more of the same
			{
				"Привет",
				new byte[] { (byte) 0x3F, (byte) 0x3F, (byte) 0x3F, (byte) 0x3F, (byte) 0x3F, (byte) 0x3F }
			},
			// Mixed valid ASCII and invalid characters
			{
				"Vn日本語",
				new byte[] { (byte) 0x56, (byte) 0x6E, (byte) 0x3F, (byte) 0x3F, (byte) 0x3F }
			}
		};
	}

	@Test(dataProvider = "tcvnData")
	public void testTcvnEncoding(String inputString, byte[] expectedBytes) throws Exception {
		byte[] actualBytes = inputString.getBytes(CHARSET_NAME);
		Assert.assertEquals(actualBytes, expectedBytes);
	}

	@Test(dataProvider = "tcvnData")
	public void testTcvnDecoding(String expectedString, byte[] inputBytes) throws Exception {
		String actualString = new String(inputBytes, CHARSET_NAME);
		Assert.assertEquals(actualString, expectedString);
	}

	@Test(dataProvider = "invalidCharacterData")
	public void testInvalidCharactersEncoding(String inputString, byte[] expectedBytes) throws Exception {
		byte[] actualBytes = inputString.getBytes(CHARSET_NAME);
		Assert.assertEquals(actualBytes, expectedBytes);
	}
}
