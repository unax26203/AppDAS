<?php
include 'conexion.php';
header('Content-Type: application/json');

$raw = file_get_contents("php://input");
$data = json_decode($raw, true);

$id = $data['id'] ?? null;
$titulo = $data['titulo'] ?? '';
$descripcion = $data['descripcion'] ?? '';
$is_completed = isset($data['isCompleted']) ? (int)$data['isCompleted'] : 0;
$tipo = $data['tipo'] ?? '';
$imagen = $data['imagen'] ?? null;

if (!$id) {
    echo json_encode(['success' => false, 'error' => 'Falta ID']);
    exit;
}

$stmt = $con->prepare("UPDATE media_items SET titulo=?, descripcion=?, is_completed=?, tipo=?, imagen=? WHERE id=?");
$stmt->bind_param("ssissi", $titulo, $descripcion, $is_completed, $tipo, $imagen, $id);

$response = [];
if ($stmt->execute()) {
    $response['success'] = true;
} else {
    $response['success'] = false;
    $response['error'] = $stmt->error;
}

$stmt->close();
$con->close();
echo json_encode($response);
