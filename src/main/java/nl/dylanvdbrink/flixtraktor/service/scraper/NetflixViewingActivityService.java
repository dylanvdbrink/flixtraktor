package nl.dylanvdbrink.flixtraktor.service.scraper;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.exceptions.NetflixScrapeException;
import nl.dylanvdbrink.flixtraktor.pojo.NetflixTitle;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Service
@CommonsLog
public class NetflixViewingActivityService {
    private static final String NETFLIX_BASEURL = "https://www.netflix.com";
    private static final String LOGIN = "/gb/login";

    @Value("${netflix.username}")
    private String netflixUsername;

    @Value("${netflix.password}")
    private String netflixPassword;

    @Value("${netflix.profile}")
    private String netflixProfile;

    @Value("${tempdir}")
    private String tempDir;

    @Value("${chromedriver.binary.path}")
    private String chromedriverPath;

    /**
     * Get the viewing activity
     * @return List of NetflixTitle objects
     * @throws NetflixScrapeException If something went wrong while using the chromedriver
     * @throws IOException If something went wrong
     * @throws InterruptedException  If something went wrong while using the chromedriver
     */
    public List<NetflixTitle> getViewingActivity(int pageSize) throws NetflixScrapeException, IOException, InterruptedException {
        List<NetflixTitle> titles;
        Gson gson = new Gson();

        WebDriver driver;
        try {
            driver = createChromeDriver();
        } catch (URISyntaxException e) {
            throw new NetflixScrapeException("Could not create chromeDriver");
        }

        JavascriptExecutor js;
        if (driver instanceof JavascriptExecutor) {
            js = (JavascriptExecutor)driver;
        } else {
            throw new NetflixScrapeException("Could not get viewingactivity CSV because browser does not support JavaScript");
        }

        log.debug("Logging in...");
        driver.get(NETFLIX_BASEURL + LOGIN);
        try {
            driver.findElement(By.id("email")).sendKeys(netflixUsername);
            driver.findElement(By.id("password")).sendKeys(netflixPassword);
            driver.findElement(By.id("email")).submit();
        } catch (NoSuchElementException nsee) {
            log.debug("Trying alternate element ids for logging in");
            driver.findElement(By.id("id_userLoginId")).sendKeys(netflixUsername);
            driver.findElement(By.id("id_password")).sendKeys(netflixPassword);
            driver.findElement(By.id("id_userLoginId")).submit();
        }
        log.debug("Success logging in");

        log.debug("Selecting profile...");
        int maxAttempts = 50;
        int currentAttempt = 1;
        while (driver.findElements(By.className("choose-profile")).isEmpty() && currentAttempt <= maxAttempts) {
            Thread.sleep(100);
            currentAttempt++;
        }

        List<WebElement> profiles = driver.findElements(By.className("choose-profile")).get(0).findElements(By.className("profile"));
        boolean profileFound = false;
        for (WebElement profile : profiles) {
            if (netflixProfile.equals(profile.findElement(By.className("profile-name")).getText())) {
                profileFound = true;
                driver.get(profile.findElement(By.className("profile-link")).getAttribute("href"));
                break;
            }
        }

        if (!profileFound) {
            throw new NetflixScrapeException("Could not find profile: " + netflixProfile);
        }
        log.debug("Success selecting profile " + netflixProfile);

        log.debug("Retrieving buildidentifier...");
        String buildIdentifier = (String) js.executeScript("return netflix.appContext.state.model.models.serverDefs.data.BUILD_IDENTIFIER");
        log.debug("Success: " + buildIdentifier);

        log.debug("Retrieving viewing activity...");
        driver.get(String.format("%s/shakti/%s/viewingactivity?pgsize=%s", NETFLIX_BASEURL, buildIdentifier, pageSize));
        JsonReader reader = new JsonReader(new StringReader(removeHtmlTags(driver.getPageSource())));
        reader.setLenient(true);
        JsonElement element = JsonParser.parseReader(reader);

        Type listType = new TypeToken<ArrayList<NetflixTitle>>(){}.getType();
        titles = gson.fromJson(element.getAsJsonObject().get("viewedItems"), listType);
        log.debug("Success retrieving viewing activity");

        return titles;
    }

    /**
     * Removes the html tags at the begin and end of the pagesource
     * @param pageSource The page source
     * @return Returns the pagesource without the html tags
     */
    private String removeHtmlTags(final String pageSource) {
        String result;
        result = pageSource.replace("<html><head></head><body>" +
                "<pre style=\"word-wrap: break-word; white-space: pre-wrap;\">", "");
        result = result.replace("</pre></body></html>", "");
        return result.trim();
    }

    /**
     * Create the Chrome webdriver
     */
    private WebDriver createChromeDriver() throws URISyntaxException, IOException, InterruptedException {
        System.setProperty("webdriver.chrome.driver", chromedriverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--test-type");
        options.addArguments("--headless");
        options.addArguments("--disable-extensions");
        options.addArguments("--disable-notifications");

        ChromeDriverService driverService = ChromeDriverService.createDefaultService();
        ChromeDriver driver = new ChromeDriver(driverService, options);

        Map<String, Object> commandParams = new HashMap<>();
        commandParams.put("cmd", "Page.setDownloadBehavior");
        Map<String, String> params = new HashMap<>();
        params.put("behavior", "allow");
        params.put("downloadPath", tempDir);
        commandParams.put("params", params);
        Gson gson = new Gson();
        String command = gson.toJson(commandParams);
        String u = driverService.getUrl().toString() + "/session/" + driver.getSessionId() + "/chromium/send_command";
        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI(u))
                .header("content-type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(command))
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        return driver;
    }
}
