package com.zyniel.apps.westiescrapper.helpers;

import org.openqa.selenium.*;
import org.openqa.selenium.support.ui.ExpectedCondition;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Wait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Arrays;

public class SeleniumHelper {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumHelper.class);

    public static void click(WebDriver driver, By locator) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(locator));
        element.click();
    }

    public static void type(WebDriver driver, By locator, String text) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(locator));
        element.click();
        element.clear();
        element.sendKeys(text);
    }

    public static  Boolean isElementVisible(Wait<WebDriver> wait, By elementBy) {
        try {
            wait.until(ExpectedConditions.visibilityOfElementLocated(elementBy));
        } catch (NoSuchElementException | TimeoutException e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            return false;
        }
        return true;
    }

    public static Boolean anyElementsVisible(Wait<WebDriver> wait, ExpectedCondition<?>... elementsBy) {
        try {
            wait.until(ExpectedConditions.or(elementsBy));
        } catch (NoSuchElementException | TimeoutException e) {
            logger.error(Arrays.toString(e.getStackTrace()));
            return false;
        }
        return true;
    }

    public static boolean elementIsSelected(WebDriver driver, By locator) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.elementToBeClickable(locator));
        return element.isSelected();
    }

    public static String getText(WebDriver driver, By locator) {
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(locator));
        return element.getText();
    }

    public static boolean scrollToPageBottom(WebDriver driver) {
        long initialLength = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
        long currentLength = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");

        return (initialLength == currentLength);
    }

    public static boolean scrollToPageTop(WebDriver driver) {
        long initialLength = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");
        ((JavascriptExecutor) driver).executeScript("window.scrollTo(0, document.body.scrollHeight)");
        long currentLength = (long) ((JavascriptExecutor) driver).executeScript("return document.body.scrollHeight");

        return (initialLength == currentLength);
    }

    public static boolean scrollElementBy(WebDriver driver, By by, int top, int left) {
        // Get height before
        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(by));
        long initialLength = (long) ((JavascriptExecutor) driver).executeScript("return arguments[0].scrollHeight", element);

        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollBy({ top: " + String.valueOf(top) + ", left: " + String.valueOf(left) +", behavior: \"smooth\" });", element);

        // Get height after and compare
        element = new WebDriverWait(driver, Duration.ofSeconds(10)).until(ExpectedConditions.visibilityOfElementLocated(by));
        long currentLength = (long) ((JavascriptExecutor) driver).executeScript("return arguments[0].scrollHeight", element);

        return (initialLength == currentLength);
    }
}
