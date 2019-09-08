package nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.scraper;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.exceptions.NetflixScrapeException;
import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.pojo.NetflixTitle;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeDriverService;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
@Service
@CommonsLog
public class NetflixViewingActivityService {

    private static final String NETFLIX_BASEURL = "https://www.netflix.com";
    private static final String LOGIN = "/nl-en/login";

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

    @Value("${maxbatchsize:20}")
    private int maxBatchSize;

    /**
     * Get the viewing activity
     * @return List of NetflixTitle objects
     * @throws NetflixScrapeException If something went wrong while using the chromedriver
     * @throws IOException If something went wrong
     * @throws InterruptedException  If something went wrong while using the chromedriver
     */
    public List<NetflixTitle> getViewingActivity() throws NetflixScrapeException, IOException, InterruptedException {
        List<NetflixTitle> titles;
        Gson gson = new Gson();

        WebDriver driver = createChromeDriver();

        JavascriptExecutor js;
        if (driver instanceof JavascriptExecutor) {
            js = (JavascriptExecutor)driver;
        } else {
            throw new NetflixScrapeException("Could not get viewingactivity CSV because browser does not support JavaScript");
        }

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

        List<WebElement> profiles = driver.findElements(By.className("list-profiles")).get(0).findElements(By.className("profile"));
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

        String buildIdentifier = (String) js.executeScript("return netflix.appContext.state.model.models.serverDefs.data.BUILD_IDENTIFIER");

        driver.get(String.format("%s/shakti/%s/viewingactivity?pgsize=%s", NETFLIX_BASEURL, buildIdentifier, Integer.toString(maxBatchSize)));
        JsonElement element = new JsonParser().parse(removeHtmlTags(driver.getPageSource()));

        Type listType = new TypeToken<ArrayList<NetflixTitle>>(){}.getType();
        titles = gson.fromJson(element.getAsJsonObject().get("viewedItems"), listType);

        return titles;
    }

    /**
     * Removes the html tags at the begin and end of the pagesource
     * @param pageSource The page source
     * @return Returns the pagesource without the html tags
     */
    private String removeHtmlTags(final String pageSource) {
        String result;
        result = pageSource.replace("<html xmlns=\"http://www.w3.org/1999/xhtml\"><head></head><body>" +
                "<pre style=\"word-wrap: break-word; white-space: pre-wrap;\">", "");
        result = result.replace("</pre></body></html>", "");
        return result.trim();
    }

    /**
     * Create the Chrome webdriver
     * @return
     * @throws IOException
     */
    private WebDriver createChromeDriver() throws IOException {
        System.setProperty("webdriver.chrome.driver", chromedriverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--test-type");
        options.addArguments("--headless");
        options.addArguments("--disable-extensions");

        ChromeDriverService driverService = ChromeDriverService.createDefaultService();
        ChromeDriver driver = new ChromeDriver(driverService, options);

        Map<String, Object> commandParams = new HashMap<>();
        commandParams.put("cmd", "Page.setDownloadBehavior");
        Map<String, String> params = new HashMap<>();
        params.put("behavior", "allow");
        params.put("downloadPath", tempDir);
        commandParams.put("params", params);
        Gson gson = new Gson();
        HttpClient httpClient = HttpClientBuilder.create().build();
        String command = gson.toJson(commandParams);
        String u = driverService.getUrl().toString() + "/session/" + driver.getSessionId() + "/chromium/send_command";
        HttpPost request = new HttpPost(u);
        request.addHeader("content-type", "application/json");
        request.setEntity(new StringEntity(command));
        httpClient.execute(request);

        return driver;
    }
}
