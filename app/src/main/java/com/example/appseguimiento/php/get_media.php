<?php
header('Content-Type: application/json');
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);

include 'conexion.php';

$response = [];

$sql = "SELECT * FROM media_items";
$result = $con->query($sql);

$items = [];
if ($result) {
    while ($row = $result->fetch_assoc()) {
        $items[] = $row;
    }
    echo json_encode(["items" => $items]);
} else {
    $response['error'] = $con->error;
    echo json_encode($response);
}

$con->close();
