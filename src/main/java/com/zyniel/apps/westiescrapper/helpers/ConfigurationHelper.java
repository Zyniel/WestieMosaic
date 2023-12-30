package com.zyniel.apps.westiescrapper.helpers;

import io.github.bonigarcia.wdm.WebDriverManager;
import org.openqa.selenium.PageLoadStrategy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;

public class ConfigurationHelper {

    private static final Logger logger = LoggerFactory.getLogger(ConfigurationHelper.class);

    // Comparing value as switch
    public static WebDriver getSupportedBrowserDriver(SupportedBrowsers browserName) {
        WebDriver driver = null;
        String dataFolder = System.getenv("LOCALAPPDATA");

        switch (browserName) {
            case CHROME:
                ChromeOptions chromeOptions = new ChromeOptions();
                chromeOptions.addArguments("start-maximized"); // open Browser in maximized mode
                chromeOptions.addArguments("disable-infobars"); // disabling infobars
                chromeOptions.addArguments("--disable-extensions"); // disabling extensions
                chromeOptions.addArguments("--disable-gpu"); // applicable to Windows os only
                chromeOptions.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
                chromeOptions.addArguments("--no-sandbox"); // Bypass OS security model
                chromeOptions.addArguments("--disable-in-process-stack-traces");
                chromeOptions.addArguments("--disable-logging");
                chromeOptions.addArguments("--log-level=3");
                chromeOptions.addArguments("--remote-allow-origins=*");

                // Save sessions
                String googleDataFolder = dataFolder + "\\Google\\Chrome\\User Data\\";
                chromeOptions.addArguments("--user-data-dir=" + googleDataFolder);
                logger.debug(googleDataFolder);

                driver = WebDriverManager
                        .chromedriver()
                        .capabilities(chromeOptions)
                        .create();

            case EDGE:
                // Set options
                EdgeOptions edgeOptions = new EdgeOptions();
                // edgeOptions.addArguments("--headless");
                edgeOptions.addArguments("profile-directory=Default");
                edgeOptions.addArguments("start-maximized"); // open Browser in maximized mode
                edgeOptions.addArguments("--disable-infobars"); // disabling infobars
                edgeOptions.addArguments("--disable-extensions"); // disabling extensions
                edgeOptions.addArguments("--disable-gpu"); // applicable to Windows os only
                edgeOptions.addArguments("--disable-dev-shm-usage"); // overcome limited resource problems
                edgeOptions.addArguments("--no-sandbox"); // Bypass OS security model
                edgeOptions.addArguments("--disable-in-process-stack-traces");
                edgeOptions.addArguments("--disable-logging");
                edgeOptions.addArguments("--log-level=3");
                edgeOptions.addArguments("--remote-allow-origins=*");

                // Set preferences
                HashMap<String, Object> edgePrefs = new HashMap<>();
                edgePrefs.put("smart_explore.on_image_hover", 0);
                edgePrefs.put("user_experience_metrics.personalization_data_consent_enabled", 0);
                edgePrefs.put("profile.default_content_settings.popups", 0);
                edgePrefs.put("profile.default_content_setting_values.notifications", 2);
                edgePrefs.put("profile.default_content_setting_values.automatic_downloads" , 1);
                edgePrefs.put("profile.content_settings.pattern_pairs.*,*.multiple-automatic-downloads",1);
                edgePrefs.put("profile.record_user_choices.show_greeting", 1);
                edgePrefs.put("profile.record_user_choices.show_image_of_day", 0);
                edgePrefs.put("profile.record_user_choices.show_settings", 0);
                edgePrefs.put("profile.record_user_choices.show_top_sites", 0);
                // Remove all fluff on Homepage to Load faster
                edgePrefs.put("ntp.background_image_type", "off");                  // ---> Remove background
                edgePrefs.put("ntp.background_image.provider", "NoBackground");     //
                edgePrefs.put("ntp.background_image.userSelected", 1);              // <--- Remove background
                edgePrefs.put("ntp.layout_mode", 0);                                // ---> Remove all content
                edgePrefs.put("ntp.news_feed_display", "headingsonly");             //
                edgePrefs.put("ntp.num_personal_suggestions", 0);                   //
                edgePrefs.put("ntp.quick_links_options", 0);                        //
                edgePrefs.put("ntp.record_user_choices.setting", "layout_mode");    //
                edgePrefs.put("ntp.record_user_choices.source", "ntp");             //
                edgePrefs.put("ntp.record_user_choices.value", 0);                  //
                edgePrefs.put("ntp.show_greeting", 0);                              //
                edgePrefs.put("ntp.show_image_of_day", 0);                          //
                edgePrefs.put("ntp.show_settings", 0);                              // <--- Remove all content

                edgeOptions.setExperimentalOption("prefs", edgePrefs);
                edgeOptions.setExperimentalOption("excludeSwitches", Collections.singletonList("enable-automation"));

                // Change Page Load Stretegy
                edgeOptions.setPageLoadStrategy(PageLoadStrategy.EAGER);

                // Save sessions
                String edgeDataFolder = getSessionData(browserName);
                edgeOptions.addArguments("--user-data-dir=" + edgeDataFolder);
                logger.debug(edgeDataFolder);

                // Setup Driver
                driver = WebDriverManager
                        .edgedriver()
                        .capabilities(edgeOptions)
                        .create();
        }

        return driver;
    }

    public static String getWestieAppUrl() {
        return "https://westie.app/";
    }

    public static String getSessionData(SupportedBrowsers browserName) {
        String dir = System.getProperty("user.dir");
        return Paths.get(dir, "Selenium", browserName.toString(), "User Data").toString();
    }

    public static String getUserEmail() {
        return "cabannes.francois.dance@gmail.com";
    }
}
