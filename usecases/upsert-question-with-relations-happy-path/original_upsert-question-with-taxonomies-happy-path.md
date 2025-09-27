# Pre-requested files
description: pre-requested files for this use case

## request json
request json file for this use case
with file extension .json
file name: upsert-question-with-taxonomy-relationship.json

##  data modeling doc
data modeling design for this use case
under data-modeling dir

## Reference projects
under dir reference-project

### reference project: try-mongo-testcontainer
  under dir try-mongo-testcontainer
  contains a demo project of junit test with testcontainer mongodb , you may refer to the code if needed related to testcontainer part




# Project name
quiz content management system backend, or call it in short as quiz cms
# maven submodule structure 

below is the maven submodule structure hierarchy
```
quiz-cms
  pom.xml
  external-service-proxy
    pom.xml
  global-shared-library
    pom.xml
  internal-layer
    pom.xml
    question-bank
      pom.xml
    quiz-session
      pom.xml
  orchestration-layer
    pom.xml

```

please understand the current structure of each submodule, especially by scanning the pom.xml of each.

## big Picture-basic flow and dependency between submodule

this general flow works on every use case
### orchestration-layer contains HTTP controller and Orchestration application service,
  It received command and query from frontend via request json
  make use of the mediator library(which located inside global-shared-library submodule) to route the each command handler and query handler.
 then the command handler or query handler with route to each child submodule of internal-layer submodule
  the spring security RBAC can be disabled to bypass it at current stage
  each handler usually handle one use case from command or query
### each child submodule of internal-layer submodule
each child submodule of of internal-layer submodule is a bounded context of DDD. For example question-bank is for question bank bounded context. Every child submodule of internal-layer submodule has its own hexagon architecture structure. It use its own application service via its own Application Port ins interface to serve the calling from orchestration-layer submodule's command handler or query handler, it also means, internal-layer and its child submodule has no controller.
And each submodule will also do persistence to its related database. these implementation of those persistence are inside infrastructure dir of each submodule
# external-service-proxy submodule
can leave it there now, no dependency happen yet


# tech stack
spring boot 3.5.6
mongoDB 8.0
testcontainter mongodb 
JUnit 5
Jacoco
# submodule global-shared-library

an existing mediator library serving command and query to command handler and query handler

# submodule orchestration-layer
the spring boot with spring application main method

# use case happy path

This is what we need to do
## brief introduction: 
upsert a question with Complex related taxonomy to question bank

## use case flow-happy path:
spring boot controller inside orchestration layer receive the HTTP POST request from frontend as 
  POST /api/users/{userId}/questionbanks/{questionbankId}/questions
  Content-Type: application/json
  request json file are at "Pre-requested files" section

This controller constructor inject mediator library from submodule global-shared-library
This controller create command object based on the request json, after that, mediator route the command object out to command handler which is inside the orchestration-layer submodule also.
This command handler's purpose is solely handle this use case above.
This command handler Constructor inject the application service port(ins) interface of submodule question-bank, which is child submodule of internal-layer submodule.
Inside the command handler, It's called the related method of the application service of question back.

Inside the question-bank submodule, It follows the Hexagon architecture and Domain Driven Design approach, But it does not have controller, It expose its application service via application service port(ins) interface.
The application service has the method doing upsert operation to decide to do 
  option 1) insert if question is not exist (check existence by source_question_id of request json equals to tag source_question_id value within the question document of mongodb )
    or 
  option 2) update if question exist in mongodb
No matter which option, The application service instantiate the aggregate object needed from XXXAggregate.java in domain dir, each aggregate java file Inherit AggregateRoot.Java Which located inside internal-layer subdomain a shared dir, which will be shared across all the submodule of internal-layer
AggregateRoot.Java main purpose: versioning, store domain event, compare peer

For this use case, the aggregate used related are as below:
  QuestionBanksPerUserAggregate.java (map to question_banks_per_user of mongoDB)
  QuestionAggregate.java (map to questions in mongoDB)
  TaxonomySetAggregate.java (map to taxonomy_sets in mongoDB)
  QuestionTaxonomyRelationshipAggregate.java (map to question_taxonomy_relationships in mongoDB)

# testdataloader to pre-load data, for integration test and unit test  for TDD approach
test loader creation: You need to create the test data loader also(if not exist), located inside internal-layer subdomain a shared dir, which will be shared across all the submodule of internal-layer

IMPORTANT: in order to fulfill this use case integration test and unit test, some data need to be pre-loaded using testdataloader with json file, 
  question_banks_per_user and taxonomy_sets Need to be pre-loaded 
  question_banks_per_user needs to be pre-loaded with default question back information 
  taxonomy_sets needs to be pre-loaded with default taxonomy set information,
  Those pre-loaded relation information can be found under "Pre-requested files" section 
  - the request json file
  - You also need refer to data modeling doc in the "Pre-requested files"
  we need you prepare the pre-loaded json file for each mongodb document/collection first

## short pause
show me what you have done
show me what is next
asking me for options to proceed
option 1: yes proceed coding
option 2: no, I need some modification

# TDD approach
before doing coding, we need you create necessary test code for this use case first. using JUNIT 5
the test no need to cover every use case path, just enough for this use case is ok.
add in allure report annotation to the JUNIT 5 test code
also, I need jacoco report for test coverage

# after finished coding test code for TDD
## short pause
show me what you have done
show me what is next
asking me for options to proceed
option 1: yes proceed coding
option 2: no, I need some modification

# do the implementation coding which pass all the test created above

# regarding persist the data for the use case

Store the data into database for this use case
Currently I just want you to store into testcontainer mongodb to fullfile the use case and tdd, no need setup external real mongodb yet

for this use case only below collection with be affected(CRUD) with persistence operation
- questions
- question_taxonomy_relationships
please correct me if I miss some collection for this use case

# principle of coding
## use incremental TDD approach
fulfil smaller scope test first with smaller scope coding, then expand to larger scope test with larger scope coding. on and on.
