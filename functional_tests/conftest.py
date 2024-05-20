import os

import chromedriver_autoinstaller
import pytest
from pyvirtualdisplay import Display
from selenium import webdriver


@pytest.fixture()
def base_url():
    port = os.environ.get("PYTEST_PORT")
    port = port if port is not None else "8080"
    return f"http://localhost:{port}"


@pytest.fixture
def base_driver():
    if os.name == "posix":
        display = Display(visible=0, size=(800, 800))
        display.start()
        chromedriver_autoinstaller.install()
    options = webdriver.ChromeOptions()
    options.add_argument("--incognito")
    options.add_argument("--disable-site-isolation-trials")
    #options.add_argument("--headless")
    driver = webdriver.Chrome(options=options)

    yield driver

    driver.close()


@pytest.fixture
def sessionless_driver(base_driver, base_url):
    base_driver.get(base_url + "/login.html")
    return base_driver


@pytest.fixture
def driver(base_driver, base_url):
    base_driver.get(base_url + "/login.html")
    base_driver.add_cookie({"name": "user", "value": "testuser"})
    base_driver.get(base_url)
    return base_driver
