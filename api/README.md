# AR-Friends API

## Setup

The file `index.php` is expects a file in it's parent directory called
`config.inc.php`. This file should contain the following variable
declarations (edited to suit, obviously):

```php
<?php
$database_host = '[host address]';
$database_user = '[username]';
$database_pass = '[password]';
$database_name = '[database name]';
?>
```

The file should be hosted on a web server which allows URL redirection,
such that the index.php file can interpret all of the files.

## Errors

Errors are output with a consistent structure. The following is an example
error.

```JS
{
	"error": {
		"message": "400 Bad Request",
		"details": "Parameter fb_id is required."
	}
}
```

## Queries

### INSERT

This inserts a new user into the database. This will return an error if the
user already exists.

```
Usage: /insert?fb_id=[fb_id]&fb_name=[fb_name]&lat=[lat]&long=[long]&alt=[alt]
```

Upon success, this will immediately select the nearby users and return them.

### UPDATE

This updates an existing row in the database.

#### Usage 1

```
Usage: /update?fb_id=[fb_id]&fb_name=[fb_name]
```

Upon success, this will return the following JSON structure.

```JS
{
	"content": 1
}
```

#### Usage 2

```
Usage: /update?fb_id=[fb_id]&lat=[lat]&long=[long]&alt=[alt]
```

Upon success, this will return the following JSON structure.

```JS
{
	"content": [
		{
			"fb_id": ?,
			"fb_name": ?,
			"latitude": ?,
			"longitude": ?,
			"altitude": ?
		},
		...
	]
}
```

### DELETE

This deletes a row from the database.

```
Usage: /delete?fb_id=[fb_id]
```

Upon success, this will return the following JSON structure.

```JS
{
	"content": 1
}
```