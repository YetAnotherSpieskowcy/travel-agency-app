import os

import chromedriver_autoinstaller
import pytest
from pyvirtualdisplay import Display
from selenium import webdriver


@pytest.fixture
def base_url():
    port = os.environ.get("PYTEST_PORT")
    port = port if port is not None else "8080"
    return f"http://localhost:{port}"


@pytest.fixture
def driver(base_url):
    if os.name == "posix":
        display = Display(visible=0, size=(800, 800))
        display.start()
        chromedriver_autoinstaller.install()
    driver = webdriver.Chrome()
    driver.get(base_url)

    yield driver

    driver.close()
