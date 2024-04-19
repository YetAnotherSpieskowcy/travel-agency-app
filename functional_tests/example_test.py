import os
import unittest

import chromedriver_autoinstaller
from pyvirtualdisplay.display import Display
from selenium import webdriver


class ExampeTest(unittest.TestCase):
    def setUp(self):
        if os.name == "posix":
            display = Display(visible=0, size=(800, 800))
            display.start()
            chromedriver_autoinstaller.install()
        self.BASE_URL = "http://localhost:8080"
        self.driver = webdriver.Chrome()

    def tearDown(self):
        self.driver.close()

    def test_example(self):
        self.driver.get(self.BASE_URL)
        assert self.driver.title == "Travel agency app"
