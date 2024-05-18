import pytest
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.wait import WebDriverWait


@pytest.mark.flaky(retries=2, only_on=[TimeoutException])
def test_correct_login(sessionless_driver):
    driver = sessionless_driver
    wait = WebDriverWait(driver, 5)
    input = driver.find_element(By.ID, "login")
    input.send_keys("user0")
    submit = driver.find_element(By.XPATH, "/html/body/div/form/input[2]")
    submit.click()
    wait.until(lambda driver: driver.get_cookie("user") is not None)
    assert driver.get_cookie("user")["path"] == "/"
    assert driver.get_cookie("user")["value"] == "38ed62b0-db97-4b9d-acfe-1fa988e88fa7"


invalid_logins = ["user", "Robert'); DROP TABLE users;--", "!@#$%^&*()", ""]


@pytest.mark.flaky(retries=2, only_on=[TimeoutException])
@pytest.mark.parametrize("user", range(len(invalid_logins)))
def test_invalid_login(sessionless_driver, user):
    driver = sessionless_driver
    wait = WebDriverWait(driver, 5)
    input = driver.find_element(By.ID, "login")
    input.send_keys(invalid_logins[user])
    submit = driver.find_element(By.XPATH, "/html/body/div/form/input[2]")
    submit.click()
    assert driver.get_cookie("user") is None
    wait.until(
        EC.text_to_be_present_in_element(
            (By.XPATH, '//*[@id="status"]'), "Username not found"
        )
    )
    assert (
        driver.find_element(By.XPATH, '//*[@id="status"]').text == "Username not found"
    )
