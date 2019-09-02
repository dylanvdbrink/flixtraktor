package nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.scraper;

import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import lombok.extern.apachecommons.CommonsLog;
import nl.dylanvdbrink.flixtraktor.netflixhistorywatcher.parser.NetflixViewingActivityCSVParser;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@CommonsLog
public class NetflixViewingActivityScraper {

    private static final String NETFLIX_BASEURL = "https://www.netflix.com";
    private static final String LOGIN = "/nl-en/login";

    private final NetflixViewingActivityCSVParser parser;

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

    @Autowired
    public NetflixViewingActivityScraper(NetflixViewingActivityCSVParser parser) {
        this.parser = parser;
    }

    public List<NetflixTitle> getViewingActivity() throws NetflixScrapeException, IOException, InterruptedException {
        List<NetflixTitle> titles;
        Gson gson = new Gson();

        WebDriver driver = createDriver();

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
            log.debug("Trying alternate element ids");
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

        driver.get(String.format("%s/api/shakti/%s/viewingactivitycsv", NETFLIX_BASEURL, buildIdentifier));
        String content = driver.getPageSource().substring(121).replace("</pre></body></html>", "").trim();
        driver.quit();

        Type t = new TypeToken<HashMap<String, String>>(){}.getType();
        Map<String, String> response = gson.fromJson(content, t);
        String csv = response.get("csv");
        log.debug("Got viewingactivity csv=" + csv);

        titles = parser.parse(csv);
        return titles;
    }

    private WebDriver createDriver() throws IOException {
        System.setProperty("webdriver.chrome.driver", chromedriverPath);

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--test-type");
        options.addArguments("--headless");
        options.addArguments("--disable-extensions"); //to disable browser extension popup

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
