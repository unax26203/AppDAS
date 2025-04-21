<?php
$lat = $_GET['lat'] ?? null;
$lng = $_GET['lng'] ?? null;
$tipo = $_GET['tipo'] ?? null;

if (!$lat || !$lng || !$tipo) {
    http_response_code(400);
    echo json_encode(['error' => 'Parámetros incompletos']);
    exit;
}

$apiKey = '*';
$url = "https://maps.googleapis.com/maps/api/place/nearbysearch/json?location=$lat,$lng&radius=1500&type=$tipo&key=$apiKey";

$response = file_get_contents($url);
header('Content-Type: application/json');
echo $response;
