package com.zyniel.apps.westiescrapper;

import com.zyniel.apps.westiescrapper.helpers.ConfigurationHelper;
import com.zyniel.apps.westiescrapper.helpers.SupportedBrowsers;
import com.zyniel.apps.westiescrapper.model.IDataProcessor;
import com.zyniel.apps.westiescrapper.model.WestieDataExtractor;
import com.zyniel.apps.westiescrapper.model.WestieImageExtractor;
import com.zyniel.apps.westiescrapper.model.WestieParser;
import org.openqa.selenium.WebDriver;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class WestieParserApp {

    private static Logger logger = LoggerFactory.getLogger(WestieParserApp.class);

    public static void main(String[] args) {

        // Load Configuration
        WebDriver driver = ConfigurationHelper.getSupportedBrowserDriver(SupportedBrowsers.EDGE);
        String url = ConfigurationHelper.getWestieAppUrl();

        // Prepare parser
        // IDataProcessor processor = new WestieDataExtractor();
        IDataProcessor processor = new WestieImageExtractor(driver);
        WestieParser parser = new WestieParser(driver, url, processor);

        // Start parsing
        parser.parse();

    }
}