ROOT_DIR := $(abspath .)
ALLURE_OUT := $(ROOT_DIR)/target/allure-report

.PHONY: test
test:
	mvn -q -T1C -DskipITs=false test

.PHONY: allure-generate
allure-generate:
	allure generate -o $(ALLURE_OUT) \
	  $(ROOT_DIR)/internal-layer/question-bank/target/allure-results \
	  $(ROOT_DIR)/internal-layer/shared/target/allure-results \
	  $(ROOT_DIR)/global-shared-library/target/allure-results

.PHONY: allure-open
allure-open:
	allure open $(ALLURE_OUT)

.PHONY: allure
allure: test allure-generate allure-open


