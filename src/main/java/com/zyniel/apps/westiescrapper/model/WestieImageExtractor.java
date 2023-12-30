


package com.zyniel.apps.westiescrapper.model;

import com.zyniel.apps.westiescrapper.helpers.ConfigurationHelper;
import com.zyniel.apps.westiescrapper.helpers.SeleniumHelper;
import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class WestieImageExtractor implements IDataProcessor {
    final String XPATH_EVENT_IDX_ATTR = "data-index";
    final String XPATH_EVENT_ROW = ".//div[@class='tile-inner']";
    final String XPATH_APP_ROOT_DIV = "//div[starts-with(@id, 'screenScrollView')]";
    final String XPATH_VIEWPORT = "//div[starts-with(@id, 'OverlayscreenScrollView')]";
    final String XPATH_EVENT_FAVORITE = "//div[@data-test='app-toggle-icon-overlay']";
    final String XPATH_EVENTS_FILTERS = "//div[starts-with(@id, 'OverlayscreenScrollView') and @class='fab-target']";

    private static final Logger logger = LoggerFactory.getLogger(com.zyniel.apps.westiescrapper.model.WestieImageExtractor.class);

    private WebDriverWait wait = null;

    private Rectangle viewport = null;

    private WebDriver driver;

    int lastIndex;

    public WestieImageExtractor(WebDriver driver) {
        this.driver = driver;
        this.lastIndex = -1;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
    }

    @Override
    public boolean process(WebElement e) {
        int passedParse = 0;
        int currentIdx = -1;
        int eventY = e.getLocation().getY();
        int eventH = e.getSize().getHeight();

        if (viewport == null) {
            boolean isStale = false;
            int currentTry = 0;
            int maxRetry = 5;
            // Fetch viewport information
            do {
                try {
                    WebElement vp = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_VIEWPORT)));
                    viewport = vp.getRect();
                } catch (StaleElementReferenceException ex) {
                    isStale = true;
                    currentTry++;
                }
            } while (isStale && currentTry < maxRetry);
        }

        int lowBound = viewport.getY();
        int topBound = viewport.getY() + viewport.getHeight();
        if ((eventY < lowBound) || (eventY + eventH > topBound)) {
            logger.info("Skipping event - Outside of viewport");
            if (passedParse > 0) {
                // Exit parsing on first Out of Viewport following a successful parsing
                // Prevents working on unuseable images
                // TODO : Remove when re-composing images from pics + data ?
                logger.info("Ending parsing - Bottom of viewport reached");
            }
            return false;
        } else {
            try {
                String attr = e.getAttribute(XPATH_EVENT_IDX_ATTR);
                currentIdx = Integer.parseInt(e.getAttribute(XPATH_EVENT_IDX_ATTR));
                this.lastIndex = currentIdx;
                try {
                    WebElement eventData = e.findElement(By.xpath(XPATH_EVENT_ROW));
                    if (eventData != null) {
                        logger.info("Parsed event: " + currentIdx);

                        Path eventImage = Paths.get(currentIdx + ".png");
                        saveElementAsPng(eventData, eventImage);
                    }
                } catch (StaleElementReferenceException | TimeoutException ex) {
                    logger.error(ex.toString());
                }
            } catch (Exception ex) {
                logger.info("Skipping event - Failed to parse event ID");
            }

            if (currentIdx > 0) {
                passedParse++;
            }
            return true;
        }
    }

    private void saveElementAsPng(WebElement event, Path image) {
        File screenshot = null;
        try {
            screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.FILE);

            BufferedImage fullImg = ImageIO.read(screenshot);
            Point point = event.getLocation();
            int eleWidth = event.getSize().getWidth();
            int eleHeight = event.getSize().getHeight();

            BufferedImage eleScreenshot = fullImg.getSubimage(point.getX(), point.getY(), eleWidth, eleHeight);
            ImageIO.write(eleScreenshot, "png", image.toFile());

            logger.info("Event saved as PNG : " + image.toString());
        } catch (IOException e) {
            logger.error("Error occurred while saving the screenshot: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error occurred while capturing the screenshot: " + e.getMessage());
        }
    }


    private void cleanupHUD(Wait<WebDriver> wait) {
        // Remove hearths
        List<WebElement> hearthElements = driver.findElements(By.xpath(XPATH_EVENT_FAVORITE));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (WebElement hearth : hearthElements) {
            js.executeScript("arguments[0].style.visibility='hidden'", hearth);
        }

        // Remove hearths
        WebElement filtersElement = driver.findElement(By.xpath(XPATH_EVENTS_FILTERS));
        js.executeScript("arguments[0].style.visibility='hidden'", filtersElement);
    }

    @Override
    public int getLastIndex() {
        return 0;
    }
}