# oar-customer-service-api

This is a reference customers service API implementation using FastAPI. It provides several endpoints for creating, retrieving, updating, and deleting records, as well as sending emails. 

The goal of this app is to serve as a replacement of the Salesforce backend for the RPA Request Handler, in case of any sort of technical issues.

The app uses TinyDB as a database.


## Requirements

- Python 3.6+

Make sure you have Python 3.6 or a later version installed on your system before running the project.

## Technologies

This API is built using the following technologies:

- FastAPI: A modern, fast (high-performance), web framework for building APIs with Python 3.6+ based on standard Python type hints. FastAPI is built on top of Starlette for the web parts and Pydantic for the data parts.

- Uvicorn: A lightning-fast ASGI server that can serve multiple requests simultaneously. Uvicorn is built on top of the asyncio framework provided by Python 3.6+.

## Installation

### Local Installation

- Clone this repository and navigate to the project directory:

```sh
git clone https://github.com/usnistgov/oar-customer-service.git
cd oar-customer-service
```

- Create and activate a virtual environment:

```sh
python -m venv venv
source venv/bin/activate
```

- Install requirements:

```sh
pip install -r requirements.txt
```

- Set `PYTHONPATH`:

```sh
export PYTHONPATH=$PWD
```

This will tell Python interpreter to also look in the current working directory for modules and packages.

- Start the server:

```sh
uvicorn app.main:app --reload --port 9595
```

This will start the app on http://localhost:9595.


### Docker Installation

```sh
docker build -t customer-service-api .
docker run -p 9595:9595 --name customer-service-api-container  --rm customer-service-api
```

Change the port number based on available ports.

## Endpoints

The following endpoints are provided by the app:

`GET /services/apexrest/system/V1.0/pdrcaseget/{id}`: Returns a record with the specified ID.
`POST /services/apexrest/system/V1.0/pdrcasecreate`: Creates a new record with the provided data.
`PATCH /services/apexrest/system/V1.0/pdrcaseupdate/{id}`: Updates the status of a record with the specified ID.
`POST /services/apexrest/system/V1.0/pdremail`: Sends an email using the provided data.

All endpoints require a bearer token in the Authorization header. This only checks if token exists and if it matches this regex pattern:

```sh
r"^[A-Za-z0-9-_]+?\.[A-Za-z0-9-_]+?\.([A-Za-z0-9-_]+)?$"
```

See the Swagger UI at http://localhost:9595/docs for detailed documentation and testing.

## Testing

To run the automated tests, use the following command in root folder:

```sh
pytest
```

To check if service is up and running, using your favorite HTTP client, send an HTTP request like this:

```sh
GET /services/apexrest/system/V1.0/pdrcase HTTP/1.1
Host: localhost:9595
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c
```
A successful response would the message `"Service is up and running."`

>Like mentioned earlier, at the moment, token validation logic is simple; endpoints only check if token exists and if it matches this regex pattern:

```sh
r"^[A-Za-z0-9-_]+?\.[A-Za-z0-9-_]+?\.([A-Za-z0-9-_]+)?$"
```

## About TinyDB

[TinyDB](https://tinydb.readthedocs.io/en/latest/) is a lightweight, open-source document-oriented database that is designed to be simple and easy to use. It's built in Python and provides a simple JSON-based syntax for storing and querying data.

### Installation

To install TinyDB, run the following command in your terminal:

```sh
pip install tinydb
```


### Usage

1. **Create a database**

To create a new TinyDB database, you can simply create a new instance of the `TinyDB` class and pass in the path to your database file. For example:

```python
from tinydb import TinyDB

db = TinyDB('db.json')
```

This will create a new database file named `db.json` in the current directory.


2. Insert data

You can insert data into the database using the insert method on your database instance. For example:

```python
db.insert({'recordId': '12345', 'caseNum': '1234567890'})
```

This will insert a new document into the database with the specified data.

3. Update data

You can update data in the database using the update method on your database instance. For example:

```python
db.update({'age': 35}, where('name') == 'John')
```

4. Useful tricks

- To get records table:

```python
records_table = db.table("records")
```

- To change default table name (_default) to records:
```python
test_db.default_table_name = "records"
```

- To set table ID type (default int):

```python
from tinydb import Table

Table.document_id_class = str
```

Check [TinyDB API](https://tinydb.readthedocs.io/en/latest/api.html) for more details.


## License

This app is licensed under the MIT License. See LICENSE for more information.