/*
This is the Geb configuration file.
See: http://www.gebish.org/manual/current/configuration.html
*/

import org.openqa.selenium.Proxy
import org.openqa.selenium.Proxy.ProxyType
import org.openqa.selenium.Capabilities
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.chrome.ChromeDriver

/*
Proxy proxy = new Proxy()
proxy.setProxyAutoconfigUrl("http://wwwproxy.rivm.nl")
DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit()
//capabilities.setCapability(CapabilityType.PROXY, proxy)
capabilities.setCapability('PROXY', proxy)
//WebDriver driver = new HtmlUnitDriver(capabilities)
*/

proxyHost = System.getProperty('http.proxyHost')
proxyPort = System.getProperty('http.proxyPort')
if (proxyHost != null && proxyPort != null) {   
	println ">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>> ${proxyHost}"
	Proxy proxy = new Proxy()
	proxy.setProxyType(ProxyType.MANUAL) 
	proxy.setHttpProxy(proxyHost+":"+proxyPort)
	proxy.setSslProxy(proxyHost+":"+proxyPort)
	proxy.setFtpProxy(proxyHost+":"+proxyPort)
	DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit()
	capabilities.setCapability('PROXY', proxy)
}

//driver = { new HtmlUnitDriver() }
driver = { new HtmlUnitDriver(capabilities) }
//driver = { (caps==null?new HtmlUnitDriver():new HtmlUnitDriver(caps)) }
//driver = { new HtmlUnitDriver(caps) }

environments {

	// run as “groovy -Dgeb.env=htmlunit forumslurper”
	// See: http://code.google.com/p/selenium/wiki/HtmlUnitDriver
	htmlunit {
		//driver = { new HtmlUnitDriver() }
		driver = { new HtmlUnitDriver(capabilities) }
		//driver = { (caps==null?new HtmlUnitDriver():new HtmlUnitDriver(caps)) }
		//driver = { new HtmlUnitDriver(caps) }
	}

	// run as “groovy -Dgeb.env=chrome forumslurper”
	// See: http://code.google.com/p/selenium/wiki/ChromeDriver
	chrome {
		driver = { new ChromeDriver() }
	}

	// run as “groovy -Dgeb.env=firefox forumslurper”
	// See: http://code.google.com/p/selenium/wiki/FirefoxDriver
	firefox {
		driver = { new FirefoxDriver() }
	}

}
