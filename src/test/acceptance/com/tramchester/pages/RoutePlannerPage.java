package com.tramchester.pages;


import com.tramchester.Stations;
import org.joda.time.LocalDate;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.Select;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.stream.Collectors;

public class RoutePlannerPage extends Page {
    private long timeoutInSeconds = 30;
    private String toStop = "toStop";
    private String fromStop = "fromStop";

    public RoutePlannerPage(WebDriver driver) throws InterruptedException {
        super(driver);
    }

    public void setFromStop(String name) {
        setSelectorTo(fromStop, name);
    }

    public String getFromStop() {
        return getSelected(fromStop).getText();
    }

    public void setToStop(String name) {
        setSelectorTo(toStop, name);
    }

    public String getToStop() {
        return  getSelected(toStop).getText();
    }

    public void setTime(String value) {
        WebElement time = getHourElement();
        time.clear();
        time.sendKeys(value);
    }

    public String getTime() {
        return getHourElement().getAttribute("value");
    }

    private WebElement getHourElement() {
        waitForElement("hour",timeoutInSeconds);
        return findElementById("hour");
    }

    private WebElement getSelected(String id) {
        Select selector = new Select(driver.findElement(By.id(id)));
        return selector.getFirstSelectedOption();
    }

    public List<WebElement> getRecentFromStops() {
        String expression = optionGroupExpression(fromStop);
        return driver.findElements(By.xpath(expression));
    }

    public List<WebElement> getRecentToStops() {
        String expression = optionGroupExpression(toStop);
        return driver.findElements(By.xpath(expression));
    }

    private String optionGroupExpression(String id) {
        return String.format("//select[@id='%s']/optgroup[@label='Recent']/option", id);
    }

    public RouteDetailsPage submit() {
        WebElement plan = findElementById("plan");
        plan.click();
        return new RouteDetailsPage(driver);
    }

    public void waitForToStops() {
        WebDriverWait wait = new WebDriverWait(driver, timeoutInSeconds);
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id(fromStop)));
        WebElement fromStopElement = findElementById(fromStop);
        wait.until(ExpectedConditions.textToBePresentInElement(fromStopElement, Stations.Altrincham.getName()));
    }

    public List<String> getToStops() {
        WebElement list = findElementById(toStop);
        Select select = new Select(list);
        List<WebElement> options = select.getOptions();
        return options.stream().map(option -> option.getText()).collect(Collectors.toList());
    }

    public void load(String url) {
        driver.get(url);
    }

    public void setDate(LocalDate value) {

        WebElement date = findElementById("date");
        date.clear();
        String input = value.toString("YYYY-MM-dd");
        date.sendKeys(input);
    }

}
