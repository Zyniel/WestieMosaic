package com.zyniel.apps.westiescrapper.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Date;

public class WestieSocialEvent extends WestieEvent{

    private static final Logger logger = LoggerFactory.getLogger(WestieSocialEvent.class);

    /***
     * West Coast Swing non-competitive Social event
     * @param name Name of the event
     * @param startDate Day on which the event starts. Must be prior or equal to the end date.
     * @param endDate Day on which the event ends. Must be later or equal to the start date.
     * @param city Name of the city hosting the event
     * @param country Name of the country hosting the event
     */
    public WestieSocialEvent(String name, Date startDate, Date endDate, String city, String country) {
        super(name, startDate, endDate, city, country);
        super.setWSDC(false); // Not a competitive event
    }

    /***
     * West Coast Swing non-competitive Social event with all information
     * @param name Name of the event
     * @param startDate Day on which the event starts. Must be prior or equal to the end date.
     * @param endDate Day on which the event ends. Must be later or equal to the start date.
     * @param city Name of the city hosting the event
     * @param country Name of the country hosting the event
     * @param fullLocation Aggregation of all location information (Venue, Address, City, Country)
     * @param facebookUrl External facebook URL to get further information about the event
     * @param websiteUrl External website URL to get further information about the event
     * @param bannerUrl External banner URL to illustrate the event
     */
    public WestieSocialEvent(String name, Date startDate, Date endDate, String city, String country, String fullLocation, URL facebookUrl, URL websiteUrl, URL bannerUrl) {
        super(name, startDate, endDate, city, country);
        // Not a competitive event
        super.setWSDC(false);
        super.setFullLocation(fullLocation);
        super.setFacebookUrl(facebookUrl);
        super.setWebsiteUrl(websiteUrl);
        super.setBannerUrl(bannerUrl);
    }
}
