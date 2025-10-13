Sure — here’s a **consolidated summary of all the full HTTP GET URIs** that your K6 script will call (assuming the default environment variable values):

---

### **Base Parameters**

```
BASE_URL = http://localhost:8765
USER_ID = 1760085803933
QUESTION_BANK_ID = 1760085804015000
```

---

### **Full HTTP GET URIs**

1. **Basic query**

```
GET http://localhost:8765/api/v1/users/1760085803933/question-banks/1760085804015000/questions?page=0&size=20
```

2. **Category filter**

```
GET http://localhost:8765/api/v1/users/1760085803933/question-banks/1760085804015000/questions?categories=general&page=0&size=20
```

3. **Text search**

```
GET http://localhost:8765/api/v1/users/1760085803933/question-banks/1760085804015000/questions?searchText=load&page=0&size=20
```

4. **Combined filters**

```
GET http://localhost:8765/api/v1/users/1760085803933/question-banks/1760085804015000/questions?categories=general&tags=beginner&searchText=load&page=0&size=10
```

---

### **Generalized URI pattern**

```
GET {BASE_URL}/api/v1/users/{USER_ID}/question-banks/{QUESTION_BANK_ID}/questions?{queryString}
```

Where `queryString` varies based on test scenario:

* `page=0&size=20`
* `categories=Math&page=0&size=20`
* `searchText=equation&page=0&size=20`
* `categories=Math&tags=algebra&searchText=solve&page=0&size=10`

---

Would you like me to also consolidate this into a **single table** (e.g., columns for *Scenario Name*, *Query Params*, and *Full URL*) for inclusion in documentation or a test report?
