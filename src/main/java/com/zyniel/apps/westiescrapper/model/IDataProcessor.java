package com.zyniel.apps.westiescrapper.model;

import org.openqa.selenium.WebElement;

public interface IDataProcessor {

    /***
     * Process a single WebElement
     * @param e WebElement representing the Westie.app Event DOM.
     *          Contains a picture, event name, location, dates and a tag reprenseting its nature
     * @return True is the parsing was fine, false is something did not work properly
     */
    boolean process(WebElement e);

    /***
     *
     * @return Return lastest parsed index to help tracking progress
     */
    int getLastIndex ();
}
