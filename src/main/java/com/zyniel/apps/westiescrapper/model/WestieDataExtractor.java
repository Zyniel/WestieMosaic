package com.zyniel.apps.westiescrapper.model;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebElement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

public class WestieDataExtractor implements IDataProcessor{

    private static final Logger logger = LoggerFactory.getLogger(WestieDataExtractor.class);

    // Then Map them to objects representing Westie Events
    HashMap<Integer, WestieEvent> westieEvents;

    int lastIndex;

    public WestieDataExtractor() {
        this.westieEvents = new HashMap<>();
        this.lastIndex = -1;
    }

    @Override
    public boolean process(WebElement webElement) {
        // Extract WebElement HTML for faster and stale-free processing
        Document doc = Jsoup.parse(webElement.getAttribute("outerHTML"));
        // Extract div containing
        Element elt = doc.getElementsByAttribute("data-index").first();
        int currentIdx = Integer.parseInt(elt.attributes().get("data-index"));
        boolean isEvent = !(elt.getElementsByClass("tile-image-area")).isEmpty();
        if (isEvent && !westieEvents.containsKey(currentIdx)) {
            logger.debug("New event found : " + currentIdx);
            try {
                WestieEvent evt = convertElementToObject(elt);
                westieEvents.put(currentIdx, evt);
            } catch (RuntimeException | ParseException e) {
                logger.warn("Could not convert Event " + currentIdx + " to WestieEvent.");
            }
        } else {
            logger.debug("Event already exists : " + currentIdx);
        }
        this.lastIndex = currentIdx;
        return true;
    }

    @Override
    public int getLastIndex() {
        return lastIndex;
    }

    private WestieEvent convertElementToObject(Element element) throws RuntimeException, ParseException{
        if (element != null) {
            // XPath Expressions for elements
            String xpathImgOverlay = "//div[contains(@class,'tile-image-area')]//div[contains(@class,'tile-overlay')]";
            String xpathImgTop = xpathImgOverlay + "/div[contains(@class,'tile-corner-container')]";
            String xpathImgTopLeft = xpathImgTop + "/div[contains(@class,'top-left-content') and contains(@class,'corner-content')]";
            String xpathImgTopLeftTag = xpathImgTopLeft + "/div[@data-test='app-tag-overlay']";
            String xpathImgBottom = xpathImgOverlay + "/div[contains(@class,'tile-corner-container')]/div[contains(@class,'bottom-left-content') and contains(@class,'corner-content')]";
            String xpathImgCenter = xpathImgOverlay + "/div[contains(@class,'center-content') and contains(@class,'corner-content')]/div[contains(@class,'tile-text-container')]";
            // Get RAW data
            String title = element.selectXpath(xpathImgCenter + "/div[contains(@class,'tile-title')]").text();
            String subtitle = element.selectXpath(xpathImgCenter + "/div[contains(@class,'tile-subtitle')]").text();
            String dates = element.selectXpath(xpathImgBottom).text();
            String tag = element.selectXpath(xpathImgTopLeftTag).text();

            // Convert data
            // Split Subtitle by the ',''
            // -> Left  : City
            // -> Right : Country
            int sepIdx = subtitle.indexOf(',');
            String city = subtitle.substring(0, sepIdx).trim();
            String country = subtitle.substring(sepIdx + 1).trim();

            // WSDC event if the tag exists and contains WSDC
            boolean isWSDC = (tag.equalsIgnoreCase("WSDC"));

            // Event name is the Title
            String name = title;

            // Cannot define at time of parsing
            String location = "";

            // ---------------------------------------------------------------------------------------------------------
            // Date parsing will rely on three different patterns based on event period being
            // ---------------------------------------------------------------------------------------------------------
            // Split the date section by '-'
            // -> Pattern 01 : 2 dates on single month - dd-dd MMMM yyyy
            //      + Start : First 'dd' + " " + Right side 'MMMM yyyy '
            //      + End   : RHS of  pattern 'dd MMMM yyyy'
            // -> Pattern 02 : 2 dates over two different months - 'dd MMMM yyyy - dd MMMM yyyy'
            //      + Start : LHS of pattern 'dd MMMM yyyy'
            //      + End   : RHS of pattern 'dd MMMM yyyy'
            // -> Pattern 03 : 2 dates over different months in different years 'dd MMMM - dd MMMM yyyy'
            //      + Start : LHS of pattern 'dd MMMM' + ' ' + RHS of pattern 'yyyy'
            //      + End   : RHS of pattern 'dd MMMM yyyy'

            SimpleDateFormat formatter = new SimpleDateFormat("dd MMMM yyyy", Locale.FRENCH);
            String rawEndDate = "";
            String rawStartDate = "";
            Date endDate = null;
            Date startDate = null;
            sepIdx = dates.indexOf('-');

            Pattern sameMonth = Pattern.compile("\\d{1,2}-\\d{1,2} \\w+ \\d{4}", Pattern.UNICODE_CHARACTER_CLASS);
            Pattern sameYear = Pattern.compile("\\d{1,2} \\w+ - \\d{1,2} \\w+ \\d{4}", Pattern.UNICODE_CHARACTER_CLASS);
            Pattern diffYears = Pattern.compile("\\d{1,2} \\w+ \\d{4} - \\d{1,2} \\w+ \\d{4}", Pattern.UNICODE_CHARACTER_CLASS);

            boolean isSameMonth = sameMonth.matcher(dates).matches();
            boolean isSameYear = sameYear.matcher(dates).matches();
            boolean isDiffYears = diffYears.matcher(dates).matches();

            // Pattern 01: "12-14 Janvier 2024"
            if (isSameMonth || isSameYear || isDiffYears) {
                if (isSameMonth) {
                    rawEndDate = dates.substring(sepIdx + 1).trim();
                    rawStartDate = dates.substring(0, sepIdx).trim() + " " + rawEndDate.substring(2).trim();
                    logger.debug("Pattern 01: '" + dates + " >> '" + rawStartDate + "' '" + rawEndDate + "'");
                }
                // Pattern 02: "12 Janvier - 14 Février 2024"
                else if (isSameYear) {
                    rawEndDate = dates.substring(sepIdx + 1).trim();
                    rawStartDate = dates.substring(0, sepIdx - 1) + " " + rawEndDate.substring(rawEndDate.length() - 4);
                    logger.debug("Pattern 02: '" + dates + " >> '" + rawStartDate + "' '" + rawEndDate + "'");
                }
                // Pattern 03: "31 Décembre 2023 - 03 Janvier 2024"
                else if (isDiffYears) {
                    rawStartDate = dates.substring(0, sepIdx).trim();
                    rawEndDate = dates.substring(sepIdx + 1).trim();
                    logger.debug("Pattern 03: '" + dates + " >> '" + rawStartDate + "' '" + rawEndDate + "'");
                }
                try {
                    startDate = formatter.parse(rawStartDate);
                } catch (ParseException e) {
                    logger.error("Unable to parse the Start Date: " + rawStartDate);
                }
                try {
                    endDate = formatter.parse(rawEndDate);
                }  catch (ParseException e) {
                    logger.error("Unable to parse the End Date: " + rawEndDate);
                }
            } else {
                throw new RuntimeException("Unknown date pattern");
            }

            WestieEvent evt = null;
            if (isWSDC) {
                evt = new WestieWSDCEvent(name, startDate, endDate, city, country);
            } else {
                evt = new WestieSocialEvent(name, startDate, endDate, city, country);
            }
            return evt;
        } else {
            throw new RuntimeException ("Failed to convert Element to WestieEvent");
        }
    }

    public HashMap<Integer, WestieEvent> getWestieEvents() {
        return westieEvents;
    }

    public void setWestieEvents(HashMap<Integer, WestieEvent> westieEvents) {
        this.westieEvents = westieEvents;
    }

}
