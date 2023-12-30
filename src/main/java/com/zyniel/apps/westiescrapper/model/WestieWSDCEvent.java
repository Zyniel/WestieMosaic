package com.zyniel.apps.westiescrapper.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Date;

public class WestieWSDCEvent extends WestieEvent{

    private static final Logger logger = LoggerFactory.getLogger(WestieWSDCEvent.class);

    /***
     * West Coast Swing competitive WSDC event
     * @param name Name of the event
     * @param startDate Day on which the event starts. Must be prior or equal to the end date.
     * @param endDate Day on which the event ends. Must be later or equal to the start date.
     * @param city Name of the city hosting the event
     * @param country Name of the country hosting the event
     */
    public WestieWSDCEvent(String name, Date startDate, Date endDate, String city, String country) {
        super(name, startDate, endDate, city, country);
        // Competitive event
        super.setWSDC(true);
    }

    /***
     * West Coast Swing competitive WSDC event with all information
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
    public WestieWSDCEvent(String name, Date startDate, Date endDate, String city, String country, String fullLocation, URL facebookUrl, URL websiteUrl, URL bannerUrl) {
        super(name, startDate, endDate, city, country);
        // Competitive event
        super.setWSDC(true);
        super.setFullLocation(fullLocation);
        super.setFacebookUrl(facebookUrl);
        super.setWebsiteUrl(websiteUrl);
        super.setBannerUrl(bannerUrl);
    }
}
