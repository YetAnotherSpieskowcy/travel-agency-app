# flake8: noqa E731 not aplicable when working with selenium
import pytest
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.wait import WebDriverWait


@pytest.mark.flaky(retries=2, only_on=[TimeoutException])
def test_search_destination(driver):
    wait = WebDriverWait(driver, 5)

    wait.until(EC.presence_of_element_located((By.ID, "results")))
    results = lambda: driver.find_element(By.ID, "results")
    assert (  # Test if list is empty
        results().find_element(By.TAG_NAME, "p").text
        == "Kliknij Szukaj, aby wyświetlić listę"
    )
    search_bar = lambda: driver.find_element(By.NAME, "destination")
    search_bar().send_keys("Kolumbia")

    search_button = lambda: driver.find_element(By.NAME, "Krzys")
    search_button().click()

    wait.until(
        EC.presence_of_element_located(
            (By.XPATH, '//*[@id="results"]/div[1]/div[1]/p[1]')
        )
    )
    assert (  # Test if list contains at least one correct result
        len(results().find_element(By.XPATH, "//div[1]/div[1]/p[1]").text) > 0
    )
    assert (  # Test if list contains at least one correct result
        "Kolumbia" in results().find_element(By.XPATH, "//div/div/p[2]").text
    )


acceptable_destinations = ["Korfu", "Wielka", "Kan"]
wrong_destinations = ["ziemniaki", "1234", "`drxtcfygvjhbk`", "!@#$%"]


@pytest.mark.flaky(retries=2, only_on=[TimeoutException])
@pytest.mark.parametrize("destination", range(len(acceptable_destinations)))
def test_search_acceptable_destination(driver, destination):
    wait = WebDriverWait(driver, 5)
    results = lambda: driver.find_element(By.ID, "results")
    assert (  # Test if list is empty
        results().find_element(By.TAG_NAME, "p").text
        == "Kliknij Szukaj, aby wyświetlić listę"
    )
    search_bar = lambda: driver.find_element(By.NAME, "destination")
    search_bar().send_keys(acceptable_destinations[destination])

    search_button = lambda: driver.find_element(By.NAME, "Krzys")
    search_button().click()

    wait.until(
        EC.presence_of_element_located(
            (By.XPATH, '//*[@id="results"]/div[1]/div[1]/p[1]')
        )
    )
    assert (  # Test if list contains at least one correct result
        acceptable_destinations[destination]
        in results().find_element(By.XPATH, "//div/div/p[2]").text
    )


@pytest.mark.flaky(retries=2, only_on=[TimeoutException])
@pytest.mark.parametrize("destination", range(len(wrong_destinations)))
def test_search_wrong_destination(driver, destination):
    wait = WebDriverWait(driver, 5)
    results = lambda: driver.find_element(By.ID, "results")
    assert (  # Test if list is empty
        results().find_element(By.TAG_NAME, "p").text
        == "Kliknij Szukaj, aby wyświetlić listę"
    )
    search_bar = lambda: driver.find_element(By.NAME, "destination")
    search_bar().send_keys(wrong_destinations[destination])

    search_button = lambda: driver.find_element(By.NAME, "Krzys")
    search_button().click()

    wait.until(EC.presence_of_element_located((By.NAME, "noResults")))
    assert (  # Test if list contains at least one correct result
        results().find_element(By.XPATH, "//div/div/p").text == "Brak wyników"
    )


acceptable_from = ["Warszawa", "Gdańsk", "Kat"]
wrong_from = ["ziemniaki", "1234", "`drxtcfygvjhbk`", "!@#$%"]


@pytest.mark.flaky(retries=2, only_on=[TimeoutException])
@pytest.mark.parametrize("f", range(len(acceptable_from)))
def test_search_acceptable_from(driver, f):
    wait = WebDriverWait(driver, 5)
    results = lambda: driver.find_element(By.ID, "results")
    assert (  # Test if list is empty
        results().find_element(By.TAG_NAME, "p").text
        == "Kliknij Szukaj, aby wyświetlić listę"
    )
    search_bar = lambda: driver.find_element(By.NAME, "from")
    search_bar().send_keys(acceptable_from[f])

    search_button = lambda: driver.find_element(By.NAME, "Krzys")
    search_button().click()

    wait.until(
        EC.presence_of_element_located(
            (By.XPATH, '//*[@id="results"]/div[1]/div[1]/p[1]')
        )
    )
    assert (  # Test if list contains at least one correct result
        len(results().find_element(By.XPATH, "//div/div/p[2]").text) > 0
    )


@pytest.mark.flaky(retries=2, only_on=[TimeoutException])
@pytest.mark.parametrize("f", range(len(wrong_from)))
def test_search_wrong_from(driver, f):
    wait = WebDriverWait(driver, 5)
    results = lambda: driver.find_element(By.ID, "results")
    assert (  # Test if list is empty
        results().find_element(By.TAG_NAME, "p").text
        == "Kliknij Szukaj, aby wyświetlić listę"
    )
    search_bar = lambda: driver.find_element(By.NAME, "from")
    search_bar().send_keys(wrong_from[f])

    search_button = lambda: driver.find_element(By.NAME, "Krzys")
    search_button().click()

    wait.until(EC.presence_of_element_located((By.NAME, "noResults")))
    assert (  # Test if list contains at least one correct result
        results().find_element(By.XPATH, "//div/div/p").text == "Brak wyników"
    )


@pytest.mark.skip()
@pytest.mark.flaky(retries=2, only_on=[TimeoutException])
def test_details_view(driver):
    wait = WebDriverWait(driver, 5)
    wait.until(EC.presence_of_element_located((By.NAME, "search")))
    search_bar = lambda: driver.find_element(By.NAME, "search")
    search_bar().send_keys("test search string")

    wait.until(
        EC.presence_of_element_located(
            (By.XPATH, "/html/body/div/div/div[1]/div[2]/button")
        )
    )
    button = lambda: driver.find_element(
        By.XPATH, "/html/body/div/div/div[1]/div[2]/button"
    )
    assert button().text == "DETAILS"
    button().click()

    wait.until(
        EC.presence_of_element_located((By.XPATH, "/html/body/div/div/div[1]/p[1]"))
    )
    assert (
        driver.find_element(By.XPATH, "/html/body/div/div/div[1]/p[1]").text
        == "Trip no. 0 \u2605\u2605\u2605\u2be8"
    )
    assert (
        driver.find_element(By.XPATH, "/html/body/div/div/div[1]/p[2]").text
        == "A slightly longer description for trip number 0"
    )
    cancel = lambda: driver.find_element(By.XPATH, "/html/body/div/div/div[1]/button")
    assert cancel().text == "CANCEL"
    cancel().click()
    results = lambda: driver.find_element(By.ID, "destination")
    assert (  # Test if list is empty
        results().find_element(By.TAG_NAME, "p").text
        == "Start typing to see the results..."
    )
