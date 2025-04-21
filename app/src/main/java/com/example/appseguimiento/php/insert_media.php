<?php
include 'conexion.php';
header('Content-Type: application/json');
ini_set('display_errors', 1);
ini_set('display_startup_errors', 1);
error_reporting(E_ALL);


$raw = file_get_contents("php://input");
$data = json_decode($raw, true);


if (is_null($data)) {
    echo json_encode([
        'success' => false,
        'error' => 'JSON inválido o vacío',
        'raw_body' => $raw,
        'json_error' => json_last_error_msg()
    ]);
    exit;
}


$errores = [];
if (!isset($data['titulo'])) $errores[] = 'Falta título';
if (!isset($data['descripcion'])) $errores[] = 'Falta descripción';
if (!isset($data['tipo'])) $errores[] = 'Falta tipo';

if (!empty($errores)) {
    echo json_encode([
        'success' => false,
        'error' => 'Parámetros incompletos',
        'detalles' => $errores,
        'data_recibida' => $data
    ]);
    exit;
}


$titulo = $data['titulo'];
$descripcion = $data['descripcion'];
$is_completed = isset($data['isCompleted']) ? (int)$data['isCompleted'] : 0;
$tipo = $data['tipo'];
$imagen = $data['imagen'] ?? '';

$stmt = $con->prepare("INSERT INTO media_items (titulo, descripcion, is_completed, tipo, imagen) VALUES (?, ?, ?, ?, ?)");
$stmt->bind_param("ssiss", $titulo, $descripcion, $is_completed, $tipo, $imagen);

$response = [];
if ($stmt->execute()) {
    $response['success'] = true;
    $response['id'] = $stmt->insert_id;
} else {
    $response['success'] = false;
    $response['error'] = $stmt->error;
}

$stmt->close();
$con->close();
echo json_encode($response);
