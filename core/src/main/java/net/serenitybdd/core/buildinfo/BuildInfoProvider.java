package net.serenitybdd.core.buildinfo;

import com.beust.jcommander.internal.Maps;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import net.thucydides.core.ThucydidesSystemProperty;
import net.thucydides.core.guice.Injectors;
import net.thucydides.core.guice.ThucydidesModule;
import net.thucydides.core.util.EnvironmentVariables;
import net.thucydides.core.util.NameConverter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import static ch.lambdaj.Lambda.filter;
import static org.hamcrest.CoreMatchers.startsWith;

/**
 * Created by john on 12/02/15.
 */
public class BuildInfoProvider {
    private final EnvironmentVariables environmentVariables;
    private final DriverCapabilityRecord driverCapabilityRecord;

    public BuildInfoProvider(EnvironmentVariables environmentVariables) {
        this.environmentVariables = environmentVariables;
        this.driverCapabilityRecord = Injectors.getInjector().getInstance(DriverCapabilityRecord.class);
    }

    public BuildProperties getBuildProperties() {
        Map<String, String> generalProperties = Maps.newHashMap();
        generalProperties.put("Default Driver",ThucydidesSystemProperty.DRIVER.from(environmentVariables,"firefox"));
        generalProperties.put("Operating System",System.getProperty("os.name") + " version " + System.getProperty("os.version"));
        addRemoteDriverPropertiesTo(generalProperties);
        addSaucelabsPropertiesTo(generalProperties);
        addCustomPropertiesTo(generalProperties);

        List<String> drivers = driverCapabilityRecord.getDrivers();
        Map<String, Properties> driverPropertiesMap = driverCapabilityRecord.getDriverCapabilities();

        return new BuildProperties(generalProperties, drivers, driverPropertiesMap);
    }

    private void addRemoteDriverPropertiesTo(Map<String, String> buildProperties) {
        if (ThucydidesSystemProperty.WEBDRIVER_REMOTE_DRIVER.isDefinedIn(environmentVariables)) {
            buildProperties.put("Remote driver", ThucydidesSystemProperty.WEBDRIVER_REMOTE_DRIVER.from(environmentVariables));
            if (ThucydidesSystemProperty.WEBDRIVER_REMOTE_BROWSER_VERSION.from(environmentVariables) != null) {
                buildProperties.put("Remote browser version", ThucydidesSystemProperty.WEBDRIVER_REMOTE_BROWSER_VERSION.from(environmentVariables));
            }
            if (ThucydidesSystemProperty.WEBDRIVER_REMOTE_OS.from(environmentVariables) != null) {
                buildProperties.put("Remote OS", ThucydidesSystemProperty.WEBDRIVER_REMOTE_OS.from(environmentVariables));
            }
        }
    }

    private void addSaucelabsPropertiesTo(Map<String, String> buildProperties) {
        if (ThucydidesSystemProperty.SAUCELABS_URL.isDefinedIn(environmentVariables)) {
            buildProperties.put("Saucelabs URL", maskAPIKey(ThucydidesSystemProperty.SAUCELABS_URL.from(environmentVariables)));
            if (ThucydidesSystemProperty.SAUCELABS_USER_ID.from(environmentVariables) != null) {
                buildProperties.put("Saucelabs user", ThucydidesSystemProperty.SAUCELABS_USER_ID.from(environmentVariables));
            }
            if (ThucydidesSystemProperty.SAUCELABS_TARGET_PLATFORM.from(environmentVariables) != null) {
                buildProperties.put("Saucelabs target platform", ThucydidesSystemProperty.SAUCELABS_TARGET_PLATFORM.from(environmentVariables));
            }
            if (ThucydidesSystemProperty.SAUCELABS_DRIVER_VERSION.from(environmentVariables) != null) {
                buildProperties.put("Saucelabs driver version", ThucydidesSystemProperty.SAUCELABS_DRIVER_VERSION.from(environmentVariables));
            }
            if (ThucydidesSystemProperty.WEBDRIVER_REMOTE_OS.from(environmentVariables) != null) {
                buildProperties.put("Remote OS", ThucydidesSystemProperty.WEBDRIVER_REMOTE_OS.from(environmentVariables));
            }
        }
    }

    private String maskAPIKey(String url) {
        int apiKeyStart = url.indexOf(":");
        int apiKeyEnd = url.indexOf("@");
        return url.substring(0,apiKeyStart + 3) + "XXXXXXXXXXXXXXXX" + url.substring(apiKeyEnd);
    }

    private void addCustomPropertiesTo(Map<String, String> buildProperties) {

        List<String> sysInfoKeys = filter(startsWith("sysinfo."), environmentVariables.getKeys());
        for(String key : sysInfoKeys) {
            String simplifiedKey = key.replace("sysinfo.", "");
            String expression = environmentVariables.getProperty(key);

            String value = evaluateGroovyExpression(expression);

            buildProperties.put(humanizedFormOf(simplifiedKey), value);
        }
    }

    private String humanizedFormOf(String simplifiedKey) {
        return StringUtils.capitalize(StringUtils.replace(simplifiedKey,"."," "));
    }

    private String evaluateGroovyExpression(String expression) {
        Binding binding = new Binding();
        binding.setVariable("env", environmentVariables);
        GroovyShell shell = new GroovyShell(binding);
        return shell.evaluate(expression).toString();
    }
}
