# flake8: noqa E731 not aplicable when working with selenium
import os
import unittest

import chromedriver_autoinstaller
from pyvirtualdisplay import Display
from selenium import webdriver
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.wait import WebDriverWait


class SearchTest(unittest.TestCase):
    def setUp(self):
        if os.name == "posix":
            display = Display(visible=0, size=(800, 800))
            display.start()
            chromedriver_autoinstaller.install()
        self.BASE_URL = "http://localhost:8080"
        self.driver = webdriver.Chrome()

    def tearDown(self):
        self.driver.close()

    def test_search_string(self):
        self.driver.get(self.BASE_URL)

        results = lambda: self.driver.find_element(By.ID, "results")
        assert (  # Test if list is empty
            results().find_element(By.TAG_NAME, "p").text
            == "Start typing to see the results..."
        )
        search_bar = lambda: self.driver.find_element(By.NAME, "search")
        search_bar().send_keys("test search string")

        assert (  # Test if list contains at least one correct result
            results().find_element(By.XPATH, "//div/div/p[1]").text
            == "test search string"
        )
        assert (  # Test if list contains at least one correct result
            results().find_element(By.XPATH, "//div/div/p[2]").text == "Hello, world!"
        )

    def test_details_view(self):
        self.driver.get(self.BASE_URL)
        wait = WebDriverWait(self.driver, 5)

        search_bar = lambda: self.driver.find_element(By.NAME, "search")
        search_bar().send_keys("test search string")

        wait.until(
            EC.presence_of_element_located(
                (By.XPATH, "/html/body/div/div/div[1]/div[2]/button")
            )
        )
        button = lambda: self.driver.find_element(
            By.XPATH, "/html/body/div/div/div[1]/div[2]/button"
        )
        assert button().text == "DETAILS"
        button().click()

        wait.until(
            EC.presence_of_element_located((By.XPATH, "/html/body/div/div/div[1]/p[1]"))
        )
        assert (
            self.driver.find_element(By.XPATH, "/html/body/div/div/div[1]/p[1]").text
            == "Trip no. 0"
        )
        assert (
            self.driver.find_element(By.XPATH, "/html/body/div/div/div[1]/p[2]").text
            == "A slightly longer description for trip number 0"
        )
        cancel = lambda: self.driver.find_element(
            By.XPATH, "/html/body/div/div/div[1]/button"
        )
        assert cancel().text == "CANCEL"
        cancel().click()
        results = lambda: self.driver.find_element(By.ID, "results")
        assert (  # Test if list is empty
            results().find_element(By.TAG_NAME, "p").text
            == "Start typing to see the results..."
        )
