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

proxyHost = System.getProperty('http.proxyHost')
proxyPort = System.getProperty('http.proxyPort')

def isProxied() {
	println "in isProxied"
	return (proxyHost != null && proxyPort != null)   
}

def buildProxyCaps(caps) {
	println "in buildProxyCaps"
	Proxy proxy = new Proxy()
	proxy.setProxyType(ProxyType.MANUAL) 
	proxy.setHttpProxy(proxyHost+":"+proxyPort)
	proxy.setSslProxy(proxyHost+":"+proxyPort)
	proxy.setFtpProxy(proxyHost+":"+proxyPort)
	caps.setCapability('PROXY', proxy)
	return caps
}

if (isProxied) {
	DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit()
	capabilities = buildProxyCaps(capabilities)
	driver = { new HtmlUnitDriver(capabilities) }
} else {
	driver = { new HtmlUnitDriver() }
}

environments {

	// run as “groovy -Dgeb.env=htmlunit forumslurper”
	// See: http://code.google.com/p/selenium/wiki/HtmlUnitDriver
	htmlunit {
		if (isProxied) {
			println "in htmlunit"
			DesiredCapabilities capabilities = DesiredCapabilities.htmlUnit()
			capabilities = buildProxyCaps(capabilities)
			driver = { new HtmlUnitDriver(capabilities) }
			assert driver != null
			assert driver.capabilities != null
			println "---------------------"
			println driver.capabilities.asMap().each{ println "${it.key}=${it.value}" }
			println "---------------------"
		} else {
			driver = { new HtmlUnitDriver() }
		}
	}

	// run as “groovy -Dgeb.env=chrome forumslurper”
	// See: http://code.google.com/p/selenium/wiki/ChromeDriver
	chrome {
		if (isProxied) {
			DesiredCapabilities capabilities = DesiredCapabilities.chrom()
			capabilities = buildProxyCaps(capabilities)
			driver = { new ChromeDriver(capabilities) }
		} else {
			driver = { new ChromeDriver() }
		}
	}

	// run as “groovy -Dgeb.env=firefox forumslurper”
	// See: http://code.google.com/p/selenium/wiki/FirefoxDriver
	firefox {
		if (isProxied) {
			DesiredCapabilities capabilities = DesiredCapabilities.firefox()
			capabilities = buildProxyCaps(capabilities)
			driver = { new FirefoxDriver(capabilities) }
		} else {
			driver = { new FirefoxDriver() }
		}
	}

}
