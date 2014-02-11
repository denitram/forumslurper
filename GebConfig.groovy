/*
This is the Geb configuration file.
See: http://www.gebish.org/manual/current/configuration.html
*/

import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.chrome.ChromeDriver

driver = { new HtmlUnitDriver() }

environments {

	// run as “groovy -Dgeb.env=htmlunit ...”
	// See: http://code.google.com/p/selenium/wiki/HtmlUnitDriver
	htmlunit {
		driver = { new HtmlUnitDriver() }
	}

	// run as “groovy -Dgeb.env=chrome ...”
	// See: http://code.google.com/p/selenium/wiki/ChromeDriver
	chrome {
		driver = { new ChromeDriver() }
	}

	// run as “groovy -Dgeb.env=firefox ...”
	// See: http://code.google.com/p/selenium/wiki/FirefoxDriver
	firefox {
		driver = { new FirefoxDriver() }
	}

}
