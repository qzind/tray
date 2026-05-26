package qz.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import qz.utils.ArabicConversionUtilities;

public class ArabicConversionUtilitiesTests {

	private static final Logger log = LogManager.getLogger(ArabicConversionUtilitiesTests.class);

	@DataProvider(name = "arabicData")
	public Object[][] arabicData() {
		return new Object[][] {
			// Basic ASCII characters (Should be a 1:1 mapping in TCVN-3)
			{
				"QZ Tray Test",
				new byte[] {
					(byte) 0x51, (byte) 0x5A, (byte) 0x20, (byte) 0x54,
					(byte) 0x72, (byte) 0x61, (byte) 0x79, (byte) 0x20,
					(byte) 0x54, (byte) 0x65, (byte) 0x73, (byte) 0x74
				}
			},
			// ESC/P Control Codes (Recommended by Gemini)
			{
				"\u001b\u0041\u0042", // ESC A B
				new byte[] {
					(byte) 0x1B, (byte) 0x41, (byte) 0x42
				}
			},
			// Empty string should return an empty byte array
			{
				"",
				new byte[] {}
			},
			// Literal Arabic Input
			{
				"مرحبا",
				new byte[] {
					(byte) 0xA8,
					(byte) 0xC8,
					(byte) 0xCD,
					(byte) 0xD1,
					(byte) 0xE5
				}
			}
		};
	}

	@Test(dataProvider = "arabicData")
	public void testConvertToIBM864(String inputString, byte[] expectedBytes) throws Exception {
		byte[] actualBytes = ArabicConversionUtilities.convertToIBM864(inputString);
		Assert.assertEquals(actualBytes, expectedBytes);
	}
}
