term of agile project task hierarchy mapping to allure report annotation

1.Epic: is this use case including all possible paths (I only defined happy path so far, no other path defined yet), all these paths of this use case together represent a 'functional requirement' . map to allure report annotation @Epic

2.Feature: is this current defined use case path I have given to you so far. based on this 1.xx 2.xx 3.xx  md doc. it is called Feature or 'functional feature' only because i want to synchronize it with the allure report @Feature annotation

3.user story/enabler story: is the incremental work with acceptance criteria and each story has its own TDD cycle with test and implementation, each story has a list of WBS. map to allure report annotation @Story

4.test scenario: each test description and test code. Map to @DisplayName annotation of JUNIT5