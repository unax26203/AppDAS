<?php

header("Access-Control-Allow-Origin: *");
header("Access-Control-Allow-Methods: POST");
header("Access-Control-Allow-Headers: Content-Type");

$response = [];

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    $imagenBase64 = $_POST['imagen'];
    $nombreArchivo = $_POST['nombre']; // por ejemplo: img_20240419.jpg

    $ruta = __DIR__ . "/uploads/$nombreArchivo";

    $datosBinarios = base64_decode($imagenBase64);

    if ($datosBinarios === false) {
        http_response_code(400);
        echo "No se pudo decodificar la imagen base64";
        exit;
    }

    if (file_put_contents($ruta, $datosBinarios)) {
        $response['status'] = 'ok';
        $response['file'] = "uploads/$nombreArchivo";
        echo json_encode($response);
    } else {
        http_response_code(500);
        echo "Error al guardar en $ruta";
    }
} else {
    http_response_code(405);
    echo "Método no permitido";
}
