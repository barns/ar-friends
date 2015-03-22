<?php
// Unfortunately, the only public web server I have is in PHP. >.>
header("Content-type: application/json");

/**
 * Function to output the response as a JSON object, and stop execution.
 *
 * @param $response The associative response array.
 */
function output($response) {
    try {
        $response = json_encode($response);
        exit($response);
    } catch (Exception $e) {
        error(500, '500 Internal Server Error', 'The JSON object was malformed.');
    }
}

/**
 * Function to output an error (stopping execution).
 *
 * @param $status The HTTP status code to set.
 * @param $message The error message to output. Optional.
 * @param $details The details of the error message. Optional.
 */
function error($status = 500, $message = false, $details = '') {
    if (!$message) {
        switch ($status) {
            case 400:
                $message = '400 Bad Request';
                break;
            case 404:
                $message = '404 Not Found';
                break;
            default:
                $status = 500;
                $message = '500 Internal Server Error';
                break;
        }
    }

    header($message, true, $status);

    output(array(
        'error' => array(
            'message' => $message,
            'details' => $details
        )
    ));
}

function validate(&$get, $parameters) {
    foreach ($parameters as $parameter) {
        if (!isset($get[$parameter])) {
            error(400, false, 'Parameter '.$parameter.' is required.');
        }
    }
}

function execute(&$connection, $query) {
    $statement = $connection->prepare($query);
    if (!$statement->execute()) {
        error(500, false, $statement->errorInfo()[2]);
    }
}

// Ensure that the values provided are valid and contain the $_GET array
$parameters = array(
    'fb_id' => '/^[0-9]+$/',
    'fb_name' => '/^.+$/',
    'long' => '/^[0-9]+(.[0-9]+)?$/',
    'lat' => '/^[0-9]+(.[0-9]+)?$/',
    'alt' => '/^[0-9]+(.[0-9]+)?$/'
);
$get = array();
foreach ($_GET as $key => $value) {
    $thisMatches = FALSE;
    $value = htmlentities($value);
    foreach ($parameters as $parameter => $regex) {
        if ($key == $parameter && preg_match($regex, $value)) {
            $get[$key] = $value;
            $thisMatches = TRUE;
            break;
        }
    }
    if (!$thisMatches) {
        error(400, false, 'Invalid key-value combination for '.$key.'='.$value);
    }
}

// Get the values provided
validate($get, array('fb_id'));
$fb_id = $get['fb_id'];
if (isset($get['fb_name'])) {
    $fb_name = $get['fb_name'];
}
if (isset($get['long'])) {
    $longitude = $get['long'];
}
if (isset($get['lat'])) {
    $latitude = $get['lat'];
}
if (isset($get['alt'])) {
    $altitude = $get['alt'];
}

// Get the configuration file
if (!file_exists('../config.inc.php')) {
    error(500, false, 'Database configuration file does not exist.');
}
require_once '../config.inc.php';
if (!isset($database_host, $database_name, $database_user, $database_pass)) {
    error(500, false, 'Invalid database configuration.');
}

// Connect to the database
try {
    $connection = new PDO("mysql:host=$database_host;dbname=$database_name", $database_user, $database_pass);
} catch (Exception $e) {
    error(500, false, 'Could not connect to database.');
}

// Initialise response
$response = array(
    'content' => array()
);

switch (preg_replace('/^\/([^\/\?]*)((\/|\?).*)?$/', '$1', $_SERVER['REQUEST_URI'])) {
    case 'insert':
        validate($get, array('fb_name','long','lat','alt'));
        execute($connection, "INSERT INTO ARFriends (fb_id, fb_name, longitude, latitude, altitude)
                              VALUES ('$fb_id', '$fb_name', '$longitude', '$latitude', '$altitude')");
        break;
    case 'update':
        if (isset($fb_name)) {
            execute($connection, "UPDATE ARFriends
                                  SET fb_name='$fb_name'
                                  WHERE fb_id='$fb_id'");
            $response['content'] = 1;
            output($response);
        } else if (isset($longitude,$latitude,$altitude)) {
            execute($connection, "UPDATE ARFriends
                                  SET longitude='$longitude',
                                      latitude='$latitude',
                                      altitude='$altitude'
                                  WHERE fb_id='$fb_id'");
        } else {
            error(400, false, 'Update either name or long,lat,alt!');
        }
        break;
    case 'delete':
        execute($connection, "DELETE FROM ARFriends WHERE fb_id='$fb_id'");
        $response['content'] = 1;
        output($response);
        break;
    case 'select':
        validate($get, array('long','lat'));
        break;
    default:
        error(404);
}

// Select nearby people
$query = "SELECT fb_id, fb_name, longitude, latitude, altitude
          FROM ARFriends
          WHERE POW((longitude-$longitude),2)
                +POW((latitude-$latitude),2) < 1
                AND fb_id != '$fb_id'";
$statement = $connection->prepare($query);
$statement->bindColumn(1, $s_fb_id);
$statement->bindColumn(2, $s_fb_name);
$statement->bindColumn(3, $s_longitude);
$statement->bindColumn(4, $s_latitude);
$statement->bindColumn(5, $s_altitude);
if (!$statement->execute()) {
    error(500, false, 'Could not select values. '
                      .$statement->errorInfo()[2]);
}
while ($statement->fetch()) {
    array_push(
        $response['content'],
        array(
            'fb_id' => $s_fb_id,
            'fb_name' => $s_fb_name,
            'longitude' => $s_longitude,
            'latitude' => $s_latitude,
            'altitude' => $s_altitude
        )
    );
}
output($response);
?>