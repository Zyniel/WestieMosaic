package com.zyniel.apps.westiescrapper.model;

import java.net.URL;
import java.util.Date;

abstract class WestieEvent {

    protected String name = "";
    protected Date startDate;
    protected Date endDate;
    protected String city = "";
    protected String country = "";
    protected String fullLocation = "";
    protected URL facebookUrl;
    protected URL websiteUrl;
    protected URL bannerUrl;
    protected boolean isWSDC = false;

    /***
     * Westie Event with minimal mandatory information
     * @param name Name of the event
     * @param startDate Day on which the event starts. Must be prior or equal to the end date.
     * @param endDate Day on which the event ends. Must be later or equal to the start date.
     * @param city Name of the city hosting the event
     * @param country Name of the country hosting the event
     */
    public WestieEvent(String name, Date startDate, Date endDate, String city, String country) {
        setName(name);
        setStartDate(startDate);
        setEndDate(endDate);
        setCity(city);
        setCountry(country);
        setWSDC(false);
    }

    /***
     *
     * @return flag to tag WSDC competitive events.
     */
    public boolean isWSDC() {
        return isWSDC;
    }

    /**
     *
     * @param WSDC flags the events as a competitive WSDC event
     */
    public void setWSDC(boolean WSDC) {
        isWSDC = WSDC;
    }

    /***
     *
     * @return event name
     */
    public String getName() {
        return name;
    }

    /***
     *
     * @param name event name (must not be empty or null)
     */
    public void setName(String name) {
        if (name == null) {
            throw new RuntimeException("Event 'Name' must not be Null");
        } else if (name.isEmpty()){
            throw new RuntimeException("Event 'Name' must not be empty");
        }
        this.name = name;
    }

    /***
     *
     * @return day on which the event starts
     */
    public Date getStartDate() {
        return startDate;
    }

    /***
     *
     * @param startDate day on which the event starts (must be non-null and prior or equal to the end date)
     */
    public void setStartDate(Date startDate) {
        if (startDate == null) {
            throw new RuntimeException("Event 'Start Date' cannot be null");
        } else if ((this.endDate != null) && (startDate.compareTo(this.endDate) > 0)) {
            throw new RuntimeException("Event 'Start Date' must be lower or equal to 'End Date'");
        }
        this.startDate = startDate;
    }

    /**
     *
     * @return day on which the event ends
     */
    public Date getEndDate() {
        return endDate;
    }

    /***
     *
     * @param endDate day on which the event ends (must be non-null and later or equal to the start date)
     */
    public void setEndDate(Date endDate) {
        if (endDate == null) {
            throw new RuntimeException("Event 'End Date' cannot be null");
        } else if ((this.startDate != null) && (endDate.compareTo(this.startDate) < 0)) {
            throw new RuntimeException("Event 'End Date' must be greater or equal to 'Start Date'");
        }
        this.endDate = endDate;
    }

    /***
     *
     * @return city name hosting the event
     */
    public String getCity() {
        return city;
    }

    /***
     *
     * @param city Optional city name hosting the event (optional)
     */
    public void setCity(String city) {
        this.city = city;
    }

    /***
     *
     * @return country name hosting the event
     */
    public String getCountry() {
        return country;
    }

    /***
     *
     * @param country Optional country name hosting the event (optional)
     */
    public void setCountry(String country) {
        this.country = country;
    }

    /***
     *
     * @return Optional full venue name, address, city and country to locate the event
     */
    public String getFullLocation() {
        return fullLocation;
    }

    /***
     *
     * @param fullLocation Optional full venue name, address, city and country to locate the event
     */
    public void setFullLocation(String fullLocation) {
        this.fullLocation = fullLocation;
    }

    /***
     *
     * @return facebook URL to find further information regarding the event
     */
    public URL getFacebookUrl() {
        return facebookUrl;
    }

    /***
     *
     * @param facebookUrl Optional facebook URL to find further information regarding the event
     */
    public void setFacebookUrl(URL facebookUrl) {
        this.facebookUrl = facebookUrl;
    }

    /***
     *
     * @return website URL to find further information regarding the event
     */
    public URL getWebsiteUrl() {
        return websiteUrl;
    }

    /***
     *
     * @param websiteUrl Optional website URL to find further information regarding the event
     */
    public void setWebsiteUrl(URL websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    /***
     *
     * @return banner URL to illustrate the event
     */
    public URL getBannerUrl() {
        return bannerUrl;
    }

    /***
     *
     * @param bannerUrl Optional banner URL to illustrate the event
     */
    public void setBannerUrl(URL bannerUrl) {
        this.bannerUrl = bannerUrl;
    }
}
