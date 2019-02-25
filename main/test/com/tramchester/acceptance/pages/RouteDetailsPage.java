package com.tramchester.acceptance.pages;


import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RouteDetailsPage extends Page {


    private ProvidesDateInput providesDateInput;

    public RouteDetailsPage(WebDriver driver, ProvidesDateInput providesDateInput) {
        super(driver);
        this.providesDateInput = providesDateInput;
    }

    public String getJourneyHeading(int index) {
        return getTextFor("journeyHeading", index);
    }

    public String getJourneyBegin(int index) {
        return getTextFor("journeyBegin", index);
    }

    public String getJourneyEnd(int index) {
        return getTextFor("journeyEnd", index);
    }

    public String getSummary(int index) {
        return getTextFor("journeySummary", index);
    }

    public boolean waitForRoutes() {
        return waitForElement("routes", timeOut).isEnabled();
    }

    public boolean journeyPresent(int index) {
        return waitForElement(formJourneyPanelId(index), timeOut).isDisplayed();
    }

    public JourneyDetailsPage getDetailsFor(int index) {
        WebElement panel = findElementById(formJourneyPanelId(index));
        panel.click();
        waitForElement("journeyHeader", timeOut);
        return new JourneyDetailsPage(driver, providesDateInput);
    }

    private String formJourneyPanelId(int index) {
        return "journeyPanel" + Integer.toString(index);
    }

    public boolean waitForError() {
        return waitForElement("NoRoutes", 4*timeOut).isEnabled();
    }

    public boolean notesPresent() {
        return driver.findElement(By.id("Notes")).isDisplayed();
    }
}
