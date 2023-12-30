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
public class WestieImageExtractor {

    final String xpathContinuePinBtn = "//div[starts-with(@id, 'screenScrollView')]/*[@id='app-root']/div[2]/div/div/div/div[3]/button[1]";
    final String xpathEmailInput = "//div[starts-with(@id, 'screenScrollView')]//input[@data-test='app-email-input']";
    final String xpathPinInput = "//div[starts-with(@id, 'screenScrollView')]//input[@data-test='app-pin-input']";
    final String xpathEventTile = "//div[starts-with(@id, 'screenScrollView')]//div[@class='tile-title' and @data-test='tile-item-title' and contains(text(), 'Évènements')]";

    final String xpathEventList = "//div[starts-with(@id, 'screenScrollView')]//div[@data-test='app-vertical-list']/div[starts-with (@class, 'vlist___')]/div[starts-with (@class, 'vlist___')]/div[@data-index]";
    final String xpathEventData = ".//div[@class='tile-inner']";
    final String xpathAppRoot = "//div[starts-with(@id, 'screenScrollView')]";
    final String xpathViewPort = "//div[starts-with(@id, 'OverlayscreenScrollView')]";

    // Elements to cleanup
    final String xpathHearth = "//div[@data-test='app-toggle-icon-overlay']";
    final String xpathFilters = "//div[starts-with(@id, 'OverlayscreenScrollView') and @class='fab-target']";

    private static final Logger logger = LoggerFactory.getLogger(WestieImageExtractor.class);

    private WebDriver driver;
    private String websiteUrl;

    public WestieImageExtractor(WebDriver driver, String url) {
        // Create Edge Driver
        this.driver = driver;
        this.websiteUrl = url;
    }

    public WebDriver getDriver() {
        return driver;
    }

    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public void parse() {
        logger.info("Starting URL parsing...");

        driver.get(websiteUrl);
        try {
            Wait<WebDriver> wait = new WebDriverWait(driver, Duration.ofSeconds(20));
            Wait<WebDriver> nowait = new WebDriverWait(driver, Duration.ofMillis(10));
            // Check if login needed

            // Check for current opened page, to recover session flow
            // TODO: Understand how to handle arrays of generics ...
            logger.info("Analysing current page");
            Boolean foundFlow = SeleniumHelper.anyElementsVisible(wait,
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathEventTile)),
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathPinInput)),
                    ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathEmailInput))
            );

            if (foundFlow) {
                if (SeleniumHelper.isElementVisible(nowait, By.xpath(xpathEmailInput))) {
                    logger.info("Login page found - No current session");
                    loginUser(wait);
                } else {
                    if (SeleniumHelper.isElementVisible(nowait, By.xpath(xpathPinInput))) {
                        logger.info("PIN input page found - Ongoing Authentication");
                    } else {
                        if (SeleniumHelper.isElementVisible(nowait, By.xpath(xpathEventTile))) {
                            logger.info("Landing page found - Already Authenticated");
                            clickEvents(wait);
                            parseEvents(wait);
                        }
                    }
                }

            } else {
                logger.error("Failed to continue flow");
                System.exit(10);
            }

        } finally {
            driver.quit();
        }
        logger.info("Finished URL parsing !");
    }

    private void clickEvents(Wait<WebDriver> wait) {
        logger.info("Opening events...");
        SeleniumHelper.click(this.driver, By.xpath(xpathEventTile));
        logger.info("Accessed events !");
    }

    private void parseEvents(Wait<WebDriver> wait) {
        int vpH = 0;
        int topBound = 0;
        int bottomBound = 0;
        int lastIdx = -1;
        int lastY = 0;
        int currentIdx = 0;
        int currentY = 0;

        // Slow scroll until the end of the page attempting to scrap as many images as possible
        boolean needScroll = true;

        // Get Viewport information
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
        boolean isStale = false;
        int currentTry = 0;
        int maxRetry = 3;

        // Fetch viewport information
        do {
            try {
                WebElement vp = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathViewPort)));
                vpH = vp.getSize().getHeight();
                topBound = vp.getLocation().getY();
                bottomBound = topBound + vpH;
            } catch (StaleElementReferenceException e) {
                isStale = true;
                currentTry++;
            }
        } while (isStale && currentTry < maxRetry);

        do {
            // Fetch visible events
            logger.info("Parsing visible events...");
            driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));

            // Remove useless elements on HUD preventing clean screenshots
            cleanupHUD(wait);

            int failedParse = 0;
            int passedParse = 0;

            // Get all events before keeping only visible ones and saving them as PNG
            List<WebElement> eventElements = driver.findElements(By.xpath(xpathEventList));
            for(WebElement event: eventElements) {
                int eventY = event.getLocation().getY();
                int eventH = event.getSize().getHeight();
                if ((eventY < topBound) || (eventY + eventH > bottomBound)) {
                    logger.info("Skipping event - Outside of viewport");
                    if (passedParse > 0){
                        // Exit parsing on first Out of Viewport following a successful parsing
                        // Prevents working on unuseable images
                        // TODO : Remove when re-composing images from pics + data ?
                        logger.info("Ending parsing - Bottom of viewport reached");
                        break;
                    }
                } else {
                    currentIdx = parseEvent(wait, event);
                    currentY = eventY;
                    if (currentIdx > 0) {
                        passedParse++;
                    }
                }
            }
            logger.info("Accessed events !");

            // Continue scrolling if the last element has moved after scroll
            needScroll = (lastIdx != currentIdx || lastY != currentY);
            if (needScroll) {
                lastIdx = currentIdx;
                lastY = currentY;
                logger.debug("Last element has changed > Scrolling");
                SeleniumHelper.scrollElementBy(driver, By.xpath(xpathAppRoot), 300, 0);
            } else {
                logger.debug("Last element has not changed > Finished Parsing");
            }
        } while (needScroll);
    }

    private int parseEvent(Wait<WebDriver> wait, WebElement event){
        int idx = -1;
        try {
            String attr = event.getAttribute("data-index");
            idx = Integer.parseInt(event.getAttribute("data-index"));
            try {
                WebElement eventData = event.findElement(By.xpath(xpathEventData));
                if (eventData != null) {
                    logger.info("Parsed event: " + idx);

                    Path eventImage = Paths.get(idx + ".png");
                    saveElementAsPng(eventData, eventImage);
                }
            } catch (StaleElementReferenceException | TimeoutException e) {
                logger.error(e.toString());
            }
        } catch (Exception e) {
            logger.info("Skipping event - Failed to parse event ID");
        }
        return idx;
    }

    private void saveElementAsPng(WebElement event, Path image) {
        File screenshot = null;
        try {
            screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);

            BufferedImage fullImg = ImageIO.read(screenshot);
            Point point = event.getLocation();
            int eleWidth = event.getSize().getWidth();
            int eleHeight = event.getSize().getHeight();

            BufferedImage eleScreenshot= fullImg.getSubimage(point.getX(), point.getY(), eleWidth, eleHeight);
            ImageIO.write(eleScreenshot, "png", image.toFile());

            logger.info("Event saved as PNG : " + image.toString());
        } catch (IOException e) {
            logger.error("Error occurred while saving the screenshot: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error occurred while capturing the screenshot: " + e.getMessage());
        }
    }

    private void loginUser(Wait<WebDriver> wait) {
        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathEmailInput)));
        if (emailInput.isDisplayed()) {
            logger.info("Login page found - No current session");
            emailInput.sendKeys(ConfigurationHelper.getUserEmail());

            String xpathContinueEmailBtn = "//*[@id='app-root']/div[2]/div/div/div/div[3]/button[1]";
            WebElement continueBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathContinueEmailBtn)));
            continueBtn.click();

            // Manually enter PIN
            WebElement pinInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(xpathPinInput)));
            if (pinInput.isDisplayed()) {
                logger.info("Manual PIN input needed - Please access mailbox and input PIN");
                Wait<WebDriver> waitLonger = new WebDriverWait(driver, Duration.ofSeconds(60));
                waitLonger.until(ExpectedConditions.elementToBeClickable(By.xpath(xpathContinuePinBtn)));
                logger.info("PIN entered - Click SIGN IN");
            } else {
                logger.error("Authentication flow broken. Review app.");
            }
        }
    }

    private void cleanupHUD (Wait<WebDriver> wait) {
        // Remove hearths
        List<WebElement> hearthElements = driver.findElements(By.xpath(xpathHearth));
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for(WebElement hearth: hearthElements) {
            js.executeScript("arguments[0].style.visibility='hidden'", hearth);
        }

        // Remove hearths
        WebElement filtersElement = driver.findElement(By.xpath(xpathFilters));
        js.executeScript("arguments[0].style.visibility='hidden'", filtersElement);
    }
}