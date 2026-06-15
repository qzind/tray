package qz.integration.services;

import java.security.Provider;
import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

public class SecuritySpiTests {
	@DataProvider(name = "providers")
	public Object[][] providers() throws ClassNotFoundException {
		return new Object[][] {
			{ "BC", "org.bouncycastle.jce.provider.BouncyCastleProvider" },
			{ "BCPQC", "org.bouncycastle.pqc.jcajce.provider.BouncyCastlePQCProvider" },
		};
	}

	@Test(dataProvider = "providers")
	public void testProviderRegistration(String providerName, String className) {
		try {
			Security.addProvider((Provider)Class.forName(className).getDeclaredConstructor().newInstance());
		} catch(Exception e) {
			Assert.fail(e.getMessage());
		}

		Provider p = Security.getProvider(providerName);
		Assert.assertNotNull(p, String.format("Provider '%s' was not found!", providerName));
		Assert.assertEquals(p.getClass().getName(), className, String.format("Provider's class '%s' does not match '%s'", p.getClass().getName(), className));
	}

}
