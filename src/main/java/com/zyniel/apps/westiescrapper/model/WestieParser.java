package com.zyniel.apps.westiescrapper.model;

import com.zyniel.apps.westiescrapper.helpers.ConfigurationHelper;
import com.zyniel.apps.westiescrapper.helpers.SeleniumHelper;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import javax.imageio.ImageIO;


//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class WestieParser {

    /** Processing engine used to parse the Website Events data.
     * Example :
     *  - WestieBasicParser: Extract images
     *  - WestieParser: Export Event data as JSON
     */
    private IDataProcessor processor;

    /** Standard 10s wait - Used by all standard checks */
    private WebDriverWait wait = null;
    /** Ultra quick millisecond wait - Used by EC checks */
    private WebDriverWait nowait = null;
    /** Ultra quick millisecond wait - Used for processing / heavy loadings checks */
    private WebDriverWait longwait = null;
    /** Ultra quick millisecond wait - Used for fast operations checks */
    private WebDriverWait shortwait = null;

    private static final Logger logger = LoggerFactory.getLogger(WestieParser.class);

    /**
     * Enumerates the main sections of the Westie.app website
     */
    private enum SiteSection {
        LOGIN_EMAIL, LOGIN_PIN, HOME, EVENTS, LESSONS, UNKNOWN, LOADING
    }

    // -----------------------------------------------------------------------------------------------------------------
    // Constants used by Selectors to identify content of the Westie.app website
    //
    // NOTE: Works on 30/12/2023. Should be updated if necessary.
    // Will change due to app upgrade / changes, and break the workflow if not updated.
    // -----------------------------------------------------------------------------------------------------------------
    final String XPATH_EVENT_PAGE = "//div[@id='app-root']//div[@data-test='nav-bar']/h1[contains(text(), 'Évènements à venir')]";
    final String XPATH_LESSONS_PAGE = "//div[@id='app-root']//div[@data-test='nav-bar']/h1[contains(text(), 'Stages & Soirées')]";
    final String XPATH_HOME_PAGE = "//div[@id='app-root']//div[@data-test='nav-bar']/h1[contains(text(), 'Accueil')]";
    final String XPATH_LOGIN_EMAIL_PAGE = "//div[@id='app-root']//form/input[@data-test='app-email-input']";
    final String XPATH_LOGIN_PIN_PAGE = "//div[@id='app-root']//form/input[@data-test='app-pin-input']";
    final String ID_LOADING = "loading-placeholder";
    final String XPATH_LOGIN_EMAIL_BTN = "//*[@id='app-root']/div[2]/div/div/div/div[3]/button[1]";
    final String XPATH_LOGIN_EMAIL_INPUT = "//div[@id='app-root']//form/input[@data-test='app-email-input']";
    final String XPATH_LOGIN_PIN_INPUT = "//div[@id='app-root']//form/input[@data-test='app-pin-input']";
    final String XPATH_HOME_EVENT_TILE = "//div[starts-with(@id, 'screenScrollView')]//div[@class='tile-title' and @data-test='tile-item-title' and contains(text(), 'Évènements')]";
    final String XPATH_EVENTS_LIST = "//div[starts-with(@id, 'screenScrollView')]//div[@data-test='app-vertical-list']/div[starts-with (@class, 'vlist___')]/div[starts-with (@class, 'vlist___')]/div[@data-index]";
    final String XPATH_EVENT_IDX_ATTR = "data-index";
    final String XPATH_EVENT_ROW = ".//div[@class='tile-inner']";
    final String XPATH_APP_ROOT_DIV = "//div[starts-with(@id, 'screenScrollView')]";
    final String XPATH_VIEWPORT = "//div[starts-with(@id, 'OverlayscreenScrollView')]";
    final String XPATH_EVENT_FAVORITE = "//div[@data-test='app-toggle-icon-overlay']";
    final String XPATH_EVENTS_FILTERS = "//div[starts-with(@id, 'OverlayscreenScrollView') and @class='fab-target']";

    private WebDriver driver;
    private String websiteUrl;

    private final int MAX_FLOW_RETRIES = 10;

    public WestieParser(WebDriver driver, String url, IDataProcessor processor) {
        // Create Edge Driver
        this.driver = driver;
        this.websiteUrl = url;
        this.processor = processor;
        this.longwait = new WebDriverWait(driver, Duration.ofSeconds(60));
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        this.shortwait = new WebDriverWait(driver, Duration.ofSeconds(1));
        this.nowait = new WebDriverWait(driver, Duration.ofMillis(10));
    }

    /**
     * @return The Selenium Webdriver used for parsing the Website
     */
    public WebDriver getDriver() {
        return driver;
    }

    /**
     * @param driver Define the Selenium Webdriver to use for parsing the Website
     */
    public void setDriver(WebDriver driver) {
        this.driver = driver;
    }

    /**
     * @return URL to the Westie.App website
     */
    public String getWebsiteUrl() {
        return websiteUrl;
    }

    /**
     * @param websiteUrl Define the URL to the Westie.App website
     */
    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    /**
     * Starts parsing Westie.App.
     *
     * Currently attempts to reach the Event page, and scroll down the whole page
     * while processing the information.
     */
    public void parse() {
        logger.info("Starting URL parsing...");

        driver.get(websiteUrl);
        try {
            // Check for current opened page, to recover session flow
            // TODO: Understand how to handle arrays of generics ...
            logger.info("Analysing current page");
            // Analyse current page to predict Worflow to follow
            // Using 'wait' to ensure the page has enough time to load
            boolean forceQuit = false;
            int curRetry = 0;
            int maxRetry = 10;
            SiteSection currentSection;
            do {
                currentSection = getCurrentSection();
                if (currentSection == SiteSection.UNKNOWN) {
                    logger.error("Failed to identify page - waiting a bit more");
                    curRetry++;
                    forceQuit = (curRetry > maxRetry);
                } else if (currentSection == SiteSection.LOGIN_EMAIL) {
                    logger.info("Login page found - No current session");
                    curRetry = 0;
                    loginUser();
                } else if (currentSection == SiteSection.LOGIN_PIN) {
                    logger.info("PIN input page found - Ongoing Authentication");
                    curRetry = 0;
                } else if (currentSection == SiteSection.HOME) {
                    logger.info("Landing page found - Already Authenticated");
                    curRetry = 0;
                    clickEvents();
                } else if (currentSection == SiteSection.EVENTS) {
                    logger.info("Event page found - Already Authenticated");
                    curRetry = 0;
                    parseEvents();
                    forceQuit = true;
                } else if (currentSection == SiteSection.LESSONS) {
                    logger.info("Lessons page found - Not implemented !");
                    curRetry = 0;
                    forceQuit = true;
                }
                driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
            } while (!forceQuit);

        } finally {
            driver.quit();
        }
        logger.info("Finished URL parsing !");
    }

    private void clickEvents() {
        logger.info("Opening events...");
        SeleniumHelper.click(this.driver, By.xpath(XPATH_HOME_EVENT_TILE));
        logger.info("Accessed events !");
    }

    private void parseEvents() {
        logger.debug("Starting EVENT PARSER");
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
        // driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(1));
        boolean isStale = false;
        int currentTry = 0;
        int maxRetry = 3;

        // Fetch viewport information
        do {
            try {
                WebElement vp = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_VIEWPORT)));
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
            cleanupHUD();

            // Get all Event elements visible in the Selenium DOM as a list of WebElement then
            // Convert those elements to Jsoup Element for faster data parsing
            List<WebElement> eventWebElements = driver.findElements(By.xpath(XPATH_EVENTS_LIST));

            // Quickly dispo of WebElements in Element
            for (WebElement we : eventWebElements) {
                boolean hasProcessed = processor.process(we);
                currentIdx = processor.getLastIndex();
                if (!hasProcessed) {
                    logger.warn ("WebElement processing was not successful.");
                }
            }

            /*
            int failedParse = 0;
            int passedParse = 0;

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
            */

            logger.info("Accessed events !");

            // Continue scrolling if the last element has moved after scroll
            needScroll = (lastIdx != currentIdx);
            if (needScroll) {
                lastIdx = currentIdx;
                logger.debug("Last element has changed > Scrolling");
                SeleniumHelper.scrollElementBy(driver, By.xpath(XPATH_APP_ROOT_DIV), 300, 0);
            } else {
                logger.debug("Last element has not changed > Finished Parsing");
            }
        } while (needScroll);
    }

    /**
     * @param event
     * @return
     */
    private int parseEvent(WebElement event) {
        int idx = -1;
        try {
            String attr = event.getAttribute(XPATH_EVENT_IDX_ATTR);
            idx = Integer.parseInt(event.getAttribute(XPATH_EVENT_IDX_ATTR));
            try {
                WebElement eventData = event.findElement(By.xpath(XPATH_EVENT_ROW));
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

    /**
     * @param event WebElement representing the Event banner to capture
     * @param image Full filepath to captured image location
     */
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

    /**
     * Set of actions to log the user in as automatically input the Email, and waiting for manual PIN input.
     */
    private void loginUser() {
        WebElement emailInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LOGIN_EMAIL_INPUT)));

        if (emailInput.isDisplayed()) {

            // Automatic user email input
            logger.info("Login page found - Inputing Email");
            emailInput.sendKeys(ConfigurationHelper.getUserEmail());

            WebElement continueBtn = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LOGIN_EMAIL_BTN)));
            continueBtn.click();

            // Manual PIN input
            // TODO: Automate ? Rely on session ?
            WebElement pinInput = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LOGIN_PIN_INPUT)));
            if (pinInput.isDisplayed()) {
                logger.info("Manual PIN input needed - You have 60s to access mailbox and input PIN");
                // Wait until submission - when the PIN input is not found anymore
                longwait.until(ExpectedConditions.numberOfElementsToBe(By.xpath(XPATH_LOGIN_PIN_INPUT), 0));
                logger.debug("Manual PIN input detected");
            } else {
                logger.error("Authentication flow broken. Review app.");
            }
        }
    }

    /**
     * Removes unnecessary HUD elements overlapping event tiles and preventing a clean screenshot.
     */
    private void cleanupHUD() {
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

    /**
     * @return Corresponding SiteSection depending on the currently shown webpage if valid, else SiteSection.UNKNOWN.
     */
    private SiteSection getCurrentSection() {
        // Checks for any eligible pages
        boolean validSection = SeleniumHelper.anyElementsVisible(shortwait,
                ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_HOME_PAGE)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LOGIN_EMAIL_PAGE)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LOGIN_PIN_PAGE)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_EVENT_PAGE)),
                ExpectedConditions.visibilityOfElementLocated(By.xpath(XPATH_LESSONS_PAGE))
        );

        // Identify which section his currently shown
        if (validSection) {
            if (SeleniumHelper.isElementVisible(nowait, By.xpath(XPATH_HOME_PAGE))) {
                return SiteSection.HOME;
            } else if (SeleniumHelper.isElementVisible(nowait, By.xpath(XPATH_LOGIN_EMAIL_PAGE))) {
                return SiteSection.LOGIN_EMAIL;
            } else if (SeleniumHelper.isElementVisible(nowait, By.xpath(XPATH_LOGIN_PIN_PAGE))) {
                return SiteSection.LOGIN_PIN;
            } else if (SeleniumHelper.isElementVisible(nowait, By.xpath(XPATH_EVENT_PAGE))) {
                return SiteSection.EVENTS;
            } else if (SeleniumHelper.isElementVisible(nowait, By.xpath(XPATH_LESSONS_PAGE))) {
                return SiteSection.LESSONS;
            } else {
                throw new RuntimeException("Expected section not found.");
            }
        } else {
            return SiteSection.UNKNOWN;
        }
    }
}