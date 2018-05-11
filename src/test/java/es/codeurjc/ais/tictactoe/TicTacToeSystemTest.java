package es.codeurjc.ais.tictactoe;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import org.openqa.selenium.net.NetworkUtils;

public class TicTacToeSystemTest {

	private WebDriver browser1;
	private WebDriver browser2;
   private String ownIp;

	@BeforeClass
	public static void setupClass() {
		WebApp.start();
	}
	
	@AfterClass
	public static void teardownClass() {
		WebApp.stop();
	}

	@Before
	public void setupTest() throws Exception {
      ownIp = new NetworkUtils().getIp4NonLoopbackAddressOfThisMachine().getHostAddress();
      DesiredCapabilities capability_browser1 = DesiredCapabilities.chrome();
      capability_browser1.setCapability("enableVideo", true);
      DesiredCapabilities capability_browser2 = DesiredCapabilities.chrome();
      browser1 = new RemoteWebDriver(new URL("http://selenoid:4444/wd/hub"), capability_browser1);
      browser2 = new RemoteWebDriver(new URL("http://selenoid:4444/wd/hub"), capability_browser2);
	}

	@After
	public void teardown() {
		if (browser1 != null) {
			browser1.quit();
		}
		if (browser2 != null) {
			browser2.quit();
		}		
	}

	@Test
	public void player1WinsTest() throws InterruptedException {
		ticTacToeTest(new int[] { 0, 3, 1, 4, 2 }, "Jugador1 wins! Jugador2 looses.");
	}
	
	@Test
	public void player2WinsTest() throws InterruptedException {
		ticTacToeTest(new int[] { 0, 3, 1, 4, 6, 5 }, "Jugador2 wins! Jugador1 looses.");
	}
	
	@Test
	public void drawTest() throws InterruptedException {
		ticTacToeTest(new int[] { 0, 1, 3, 6, 4, 8, 7, 5, 2 }, "Draw!");
	}


	private void ticTacToeTest(int[] cells, String gameOverMessage) throws InterruptedException {

		String player1 = "Jugador1";
		String player2 = "Jugador2";

		browser1.get("http://" + ownIp + ":8080");
		setPlayerName(browser1, player1);

		browser2.get("http://" + ownIp + ":8080");
		setPlayerName(browser2, player2);

		for (int i = 0; i < cells.length; i++) {
			if (i % 2 == 0) {
				markCell(browser1, cells[i]);
			} else {
				markCell(browser2, cells[i]);
			}
		}

		Thread.sleep(500);

		String player1Alert = browser1.switchTo().alert().getText();
		String player2Alert = browser2.switchTo().alert().getText();

		assertThat(player1Alert).isEqualTo(gameOverMessage);
		assertThat(player2Alert).isEqualTo(gameOverMessage);

	}

	private void setPlayerName(WebDriver browser, String name) {
		WebElement searchInput = browser.findElement(By.id("nickname"));
		searchInput.sendKeys(name);

		WebElement button = browser.findElement(By.id("startBtn"));
		button.click();
	}

	private void markCell(WebDriver browser, int cellId) {
		WebElement cell = browser.findElement(By.id("cell-" + cellId));
		cell.click();
	}
}
