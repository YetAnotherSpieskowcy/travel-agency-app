import pytest
from selenium.common.exceptions import TimeoutException
from selenium.webdriver.common.by import By
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.support.wait import WebDriverWait

@pytest.mark.flaky(retries=2, only_on=[TimeoutException])
def test_e2e(driver):
    wait = WebDriverWait(driver, 8)

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

    details_button = lambda: driver.find_element(By.XPATH, "//div/div[2]/input")
    details_button().click()

    wait.until(
        EC.presence_of_element_located(
            (By.ID, "start-reservation-btn")
        )
    )

    reservation_button = lambda: driver.find_element(By.ID, "start-reservation-btn")
    reservation_button().click()

    wait.until(
        EC.presence_of_element_located(
            (By.XPATH, '//*[@id="purchase-form"]/button')
        )
    )

    buy_button = lambda: driver.find_element(By.XPATH, '//*[@id="purchase-form"]/button')
    buy_button().click()

    wait.until(
        EC.presence_of_element_located(
            (By.XPATH, '//*[@id="container"]/p')
        )
    )

    confirm_string = lambda: driver.find_element(By.XPATH, '//*[@id="container"]/p')

    assert(
        confirm_string().text in ["Gratulacje, kupiłeś wycieczkę!",
        "Coś poszło nie tak, najprawdopodobniej skończyły się miejsca.",
        "Coś poszło nie tak, płatność najpewniej została odrzucona przez operatora."]
    )
