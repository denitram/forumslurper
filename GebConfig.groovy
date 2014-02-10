/*
This is the Geb configuration file.
See: http://www.gebish.org/manual/current/configuration.html
*/

import org.openqa.selenium.Proxy
import org.openqa.selenium.Capabilities
import org.openqa.selenium.remote.DesiredCapabilities
import org.openqa.selenium.htmlunit.HtmlUnitDriver
import org.openqa.selenium.firefox.FirefoxDriver
import org.openqa.selenium.chrome.ChromeDriver

/*
System.properties.with { p ->
        proxyHost = p['http.proxyHost']
        proxyPort = p['http.proxyPort']
}
*/

Proxy proxy = new Proxy()
proxy.setProxyAutoconfigUrl("http://wwwproxy.rivm.nl")
DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit()
//capabilities.setCapability(CapabilityType.PROXY, proxy)
capabilities.setCapability('PROXY', proxy)
//WebDriver driver = new HtmlUnitDriver(capabilities)

/*
if (proxyHost != null && proxyPort != null) {   
	Proxy proxy = new Proxy()
	//proxy.proxyType(ProxyType.MANUAL) 
	proxy.httpProxy(proxyHost+":"+proxyPort)
	proxy.sslProxy(proxyHost+":"+proxyPort)
	proxy.ftpProxy(proxyHost+":"+proxyPort)
	Capabilities caps = new DesiredCapabilities()
	caps.capability("proxy", proxy)
}
*/

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
